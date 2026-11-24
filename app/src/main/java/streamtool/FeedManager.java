package streamtool;

import java.util.ArrayList;

import org.json.JSONException;

enum Status {
    IDLE,       // feed is currently doing nothing
    LOADING,    // feed is loading
    STANDBY,    // feed is ready on obs but not shown on screen
    ACTIVE      // feed is shown on screen
}

public class FeedManager {

    static int maxPovs = 4;

    static boolean running = false;

    static Long timeout = 20000L;

    static RuntimeData run;
    static Data data;

    static ArrayList<Feed> feeds = new ArrayList<>();

    static ArrayList<Player> povs = new ArrayList<>();

    static int activeFeeds = 0;

    private static ArrayList<FeedRequest> queue = new ArrayList<>();

    static void init(RuntimeData run, Data data) {

        FeedManager.run = run;
        FeedManager.data = data;

        run.client.clearLinks(run.feedCount);

        for (int i = 0; i < run.feedCount; i++) {
            feeds.add(new Feed("feed" + i));
        }

        // create a separate thread to run feed changes in a queue
        new Thread(new Runnable() {
            public void run() {
                running = true;
                while (running) {
                    if (queue.size() > 0) {
                        FeedRequest request = queue.removeFirst();
                        request.execute();
                    } else {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {}
                    }
                }
                System.out.println("Stopped FeedManager");
            }
        }).start();
    }

    static void stop() {
        running = false;
    }

    // create a request and put it in a queue to be executed in the feed manager thread
    static void requestSetActiveFeeds(ArrayList<Player> povs) {
        queue.add(new FeedRequest() {
            void execute() {
                setActiveFeeds(povs);
            }
        });
    }

    static void requestShowPlayer(Player player, int p) {

        loadPlayer(player);

        if (isOnActive(player)) {
            // if requested player is already shown on screen

            // get the player's current position (p2)
            int p2 = p;
            for (int i = 0; i < povs.size(); i++) {
                if (povs.get(i) == player) p2 = i;
            }

            // get the player in the requested position
            Player other = povs.get(p - 1);

            // swap the players
            povs.set(p2, other);          
            povs.set(p - 1, player);
        } else {
            // if not

            // replace the selected pos with the requested player if possible, or just add them to last possible one
            if (p <= activeFeeds) {
                povs.set(p - 1, player);
            } else povs.add(player);
        }

        // request changing the povs
        requestSetActiveFeeds(povs);
    }

    static boolean isOnActive(Player player) {
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.status == Status.ACTIVE && feed.player == player) return true;
        }
        return false;
    }

    static boolean isOnFeed(Player player) {
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.player == player) return true;
        }
        return false;
    }


    static Feed getActiveFeed(int p) {
        if (p > 0 && p <= activeFeeds) {
            for (int i = 0; i < feeds.size(); i++) {
                Feed feed = feeds.get(i);
                if (feed.status == Status.ACTIVE && feed.activePos == p) return feed;
            }
        }

        return null;
    }

    static void setActiveFeeds(ArrayList<Player> povs) {

        // return if request is identical to what's already on screen
        if (povs.equals(FeedManager.povs)) return;

        int prevActive = activeFeeds;

        int activeCount = povs.size();

        // limit to 4 active povs
        if (activeCount > maxPovs) activeCount = maxPovs;

        ArrayList<Feed> newActive = new ArrayList<>();

        // get the feeds for each player
        for (int i = 0; i < activeCount; i++) {
            Feed feed = getPlayerFeed(povs.get(i));
            if (feed != null) {
                newActive.add(feed);
            }
        }

        // update active feed count in case some feeds weren't available
        activeFeeds = newActive.size();
        //System.out.println("Active feeds: " + activeFeeds);

        // wait until the feeds are ready
        while (true) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {}

            boolean test = true;
            for (int i = 0; i < newActive.size(); i++) {
                if (newActive.get(i).status == Status.LOADING) test = false;
            }
            if (test) {
                /*System.out.println("Feeds ready:");
                for (int i = 0; i < newActive.size(); i++) {
                    System.out.println(newActive.get(i).obsName + " " + newActive.get(i).status.toString());
                }*/
                break;
            }
        }

        // change current active feeds to standby
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.status == Status.ACTIVE) {
                //System.out.println("Set " + feed.obsName + " STANDBY");
                feed.status = Status.STANDBY;
            }
        }

        // set new active feeds to active and set their positions
        for (int i = 0; i < newActive.size(); i++) {
            //System.out.println("Set " + newActive.get(i).obsName + " ACTIVE");
            newActive.get(i).status = Status.ACTIVE;
            newActive.get(i).activePos = i + 1;
        }

        // update feeds in obs
        FeedManager.povs = povs;
        updateFeedsOnObs();

        // change scene if necessary
        if (prevActive != activeFeeds) run.commandManager.execute(run.commandParser.getCommand("scene update " + activeCount));
    }

    static void updateFeedsOnObs() {
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            int[] ids = {0, 0, 0, 0};

            // get the ids from config (obsLayout)

            try {
                ids[0] = run.obsLayout.getJSONObject("scenes").getJSONObject("feedIds").getJSONArray("p1").getInt(i);
            } catch (JSONException e) {}

            try {
                ids[1] = run.obsLayout.getJSONObject("scenes").getJSONObject("feedIds").getJSONArray("p2").getInt(i);
            } catch (JSONException e) {}

            try {
                ids[2] = run.obsLayout.getJSONObject("scenes").getJSONObject("feedIds").getJSONArray("p3").getInt(i);
            } catch (JSONException e) {}

            try {
                ids[3] = run.obsLayout.getJSONObject("scenes").getJSONObject("feedIds").getJSONArray("p4").getInt(i);
            } catch (JSONException e) {}

            // set the visibility and update player names for active feeds

            if (feed.status == Status.ACTIVE && feed.activePos == 1) {
                run.client.setSceneItemEnabled("p1", ids[0], true);
                run.client.updatePlayerInfo(1, feed.player);
            } else {
                run.client.setSceneItemEnabled("p1", ids[0], false);
            }

            if (feed.status == Status.ACTIVE && feed.activePos == 2) {
                run.client.setSceneItemEnabled("p2", ids[1], true);
                run.client.updatePlayerInfo(2, feed.player);
            } else {
                run.client.setSceneItemEnabled("p2", ids[1], false);
            }

            if (feed.status == Status.ACTIVE && feed.activePos == 3) {
                run.client.setSceneItemEnabled("p3", ids[2], true);
                run.client.updatePlayerInfo(3, feed.player);
            } else {
                run.client.setSceneItemEnabled("p3", ids[2], false);
            }

            if (feed.status == Status.ACTIVE && feed.activePos == 4) {
                run.client.setSceneItemEnabled("p4", ids[3], true);
                run.client.updatePlayerInfo(4, feed.player);
            } else {
                run.client.setSceneItemEnabled("p4", ids[3], false);
            }
        }
    }

    static Feed getPlayerFeed(Player player) {
        loadPlayer(player);
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.player == player) return feed;
        }
        return null;
    }

    static void loadPlayer(Player player) {
        // check if player is already loaded, and if so, update the time and return
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.player == player) {
                feed.lastUpdate = System.currentTimeMillis();
                return;
            }
        }

        Feed feed = getIdleFeed();

        if (feed != null) {
            feed.load(player);
        }
    }

    static void printStatus() {
        System.out.println("Povs:");
        for (int i = 0; i < povs.size(); i++) {
            System.out.println("p" + (i + 1) + " " + povs.get(i).name);
        }

        System.out.println();
        System.out.println("Feeds:");
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);

            String name;
            if (feed.player != null) {
                name = feed.player.name;
            } else name = "";

            if (feeds.get(i).status == Status.ACTIVE) {
                System.out.println(feed.obsName + " " + name + " " + feed.status.toString() + " " + feed.activePos);
            } else {
                System.out.println(feed.obsName + " " + name + " " + feed.status.toString());
            }
        }
    }

    static Feed getIdleFeed() {

        // if a feed is idle, returns that
        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.status == Status.IDLE) return feed;
        }

        // if no feed is idle, returns the standby feed that hasn't changed for longest
        Feed oldest = null;

        for (int i = 0; i < feeds.size(); i++) {
            Feed feed = feeds.get(i);
            if (feed.status == Status.STANDBY) {
                if (oldest != null) {
                    if (oldest.lastUpdate > feed.lastUpdate) oldest = feed;
                } else oldest = feed;
            }
        }

        return oldest;
    }
}

class Feed {
    String obsName;
    Status status;
    Player player;
    Long lastUpdate = 0L;
    int activePos = 10;
    boolean loadActive = false;

    Feed(String obsName) {
        this.obsName = obsName;
        status = Status.IDLE;
    }

    void reload() {
        load(player);
    }

    void load(Player player) {

        this.player = player;

        if (status == Status.ACTIVE) loadActive = true; else loadActive = false;

        //System.out.println("Set " + obsName + " LOADING");
        status = Status.LOADING;

        lastUpdate = System.currentTimeMillis();

        // send the link to obs
        String link = "http://localhost:" + LinkManager.getPort(player);
        FeedManager.run.client.setFeedLink(obsName, link);

        // run a new thread to wait for obs to load the stream so other stuff doesn't need to wait
        new Thread(new Runnable() {
            public void run() {
                Long timeout = System.currentTimeMillis() + FeedManager.timeout;

                // wait a bit so obs has time to switch to the new source
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                while(System.currentTimeMillis() < timeout) {
                    // break if the streamlink process is dead, as the stream will never load
                    if (player.feed.process != null && !player.feed.process.isAlive()) break;

                    // request feed status from obs
                    FeedManager.run.client.requestFeedStatus(obsName);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}

                    // break if MessageHandler changed the status from loading
                    if (status != Status.LOADING) break;
                }
                if (status == Status.LOADING) {
                    setStatus();
                }
            }
        }).start();
    }

    void setStatus() {
        //System.out.println("Loaded " + obsName + " " + loadActive);
        if (loadActive) status = Status.ACTIVE; else status = Status.STANDBY;
    }
}

abstract class FeedRequest {
    abstract void execute();
}