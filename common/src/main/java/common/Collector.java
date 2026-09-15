package common;

public class Collector implements Runnable {
    boolean running = true;
    boolean stopped = false;
    public void run() {
        while (running) {
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        stopped = true;
    }

    public void stop() {
        running = false;
        while (!stopped) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
    }
}
