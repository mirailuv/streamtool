package streamtool;

import java.util.ArrayList;

public class LinkManager {
    static Data data;
    static RuntimeData run;
    static ArrayList<Streamlink> links = new ArrayList<>();

    static void init(Data data, RuntimeData run) {
        LinkManager.data = data;
        LinkManager.run = run;
    }

    // kill previous feeds and create new ones for anyone who's live
    // call this on init, restore and refresh
    static void createLinks() {
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i) != null) links.get(i).stop();
        }

        Streamlink.killAll();
        links = new ArrayList<>();

        for (int i = 0; i < data.players.length; i++) {
            Player player = data.players[i];
            if (player.live) {
                startFeed(player);
            }
        }
    }

    // find or create the feed for player and return the port
    static int getPort(Player player) {
        // return 0 if player is not live
        if (!player.live) return 0;

        clearStopped();

        String twitch = player.twitch;
        // return the port if there's already a streamlink feed for the player
        for (int i = 0; i < links.size(); i++) {
            Streamlink feed = links.get(i);
            if (feed.twitch.equals(twitch)) return feed.port;
        }

        // if there's no feed, start a new one and return the port
        startFeed(player);
        return player.feed.port;
    }

    static void startFeed(Player player) {
        Streamlink feed = new Streamlink(player.twitch, getAvailablePort());
        feed.start();
        links.add(feed);
        player.feed = feed;
    }

    // if the stream failed to load the process will stop. this removes any feeds that stopped so they don't hold on to a port.
    static void clearStopped() {
        for (int i = 0; i < links.size(); i++) {
            Streamlink feed = links.get(i);
            if (feed.process != null && !feed.process.isAlive()) {
                links.remove(i);
                i--;
            }
        }
    }

    // since this uses a range of 100 ports, this makes sure the same port can't get double booked if streams are refreshed a lot.
    static int getAvailablePort() {
        clearStopped();
        while (true) {
            int port = getPort();
            boolean b = true;

            // check if any of the links are using that port
            for (int i = 0; i < links.size(); i++) {
                if (links.get(i).port == port) {
                    b = false;
                    break;
                }
            }

            // return the port if it's available
            if (b) return port;
        }
    }

    // get next port in line
    static int getPort() {
        run.currentPort++;
        return run.portRange + run.currentPort;
    }
}
