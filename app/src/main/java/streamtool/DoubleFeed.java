package streamtool;

public class DoubleFeed {
    Streamlink a = null;
    Streamlink b = null;

    boolean feed = false;

    public void newFeed(Streamlink s) {
        s.start();
        switchFeed();
        if (feed) a = s; else b = s;
    }

    public Streamlink activeFeed() {
        if (feed) return a; else return b;
    }

    private void switchFeed() {
        Streamlink s;
        if (feed) {
            s = a;
            feed = false;
        } else {
            s = b;
            feed = true;
        }
        if (s == null) return;
        new CutFeed(Main.abDelay, s);
    }
}

class CutFeed implements Runnable {
    Long delay;
    Streamlink feed;

    public CutFeed(Long delay, Streamlink feed) {
        this.delay = delay;
        this.feed = feed;
        new Thread(this).start();
    }

    public void run() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {}

        feed.stop();
    }
}
