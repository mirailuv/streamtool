package streamtool;

import java.io.IOException;

public class Streamlink {

    Thread thread;
    Process process;
    String twitch;
    int port;

    void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }
        thread = new Thread() {
            public void run() {
                ProcessBuilder builder = new ProcessBuilder("streamlink", "--player-external-http-port", "" + port, "twitch.tv/" + twitch);
                try {
                    process = builder.start();
                } catch (IOException e) {}
            }
        };
        thread.start();
    }

    void stop() {
        if (process != null) process.destroy();
    }

    public Streamlink(String twitch, int port) {
        this.twitch = twitch;
        this.port = port;
    }

    public static void killAll() {
        ProcessBuilder builder = new ProcessBuilder("killall","streamlink");
        try {
            builder.start();
        } catch (IOException e) {}
    }
}
