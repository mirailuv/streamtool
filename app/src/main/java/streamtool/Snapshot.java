package streamtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

public class Snapshot {

    Thread snapshot;
    boolean isRunning;
    int saved;
    int seedNumber = 1;

    int loadTracker = 0;

    void snapshotThread() {
        snapshot = new Thread() {
            public void run() {
                while(true) {
                    if (!isRunning) break;

                    JSONObject data = Main.readJSON(new File("spectate_match.json"));

                    if (data != null) {
                        Long time = System.currentTimeMillis() / 1000;

                        try {
                            Files.createDirectories(Paths.get("snapshots"));
                        } catch (IOException e) {
                            // maybe add some exception logging?
                        }

                        File file = new File("snapshots/spectate-" + time + ".json");

                        try {
                            BufferedWriter w = new BufferedWriter(new FileWriter(file));
                            w.write(data.toString());
                            w.close();
                            saved++;
                        } catch (IOException e) {
                            // maybe add some exception logging?
                        }
                    }

                    try {
                        sleep(10000);
                    } catch (InterruptedException e) {
                        // maybe add some exception logging?
                    }
                }
                System.out.println("Snapshot stopped");
            }
        };
    }

    void snapshotLoadThread() {
        snapshot = new Thread() {
            public void run() {
                while(true) {
                    if (!isRunning) break;

                    File file = new File("snapshots/spectate-" + loadTracker + ".json");
                    JSONObject data = null;
                    if (file.exists()) data = Main.readJSON(file);

                    File spectate = new File("spectate_match.json");

                    if (data != null) {
                        try {
                            BufferedWriter w = new BufferedWriter(new FileWriter(spectate));
                            w.write(data.toString());
                            w.close();
                        } catch (IOException e) {
                            // maybe add some exception logging?
                        }
                    }

                    loadTracker++;

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        // maybe add some exception logging?
                    }
                }
                System.out.println("Snapshot loader stopped");
            }
        };
    }

    public Snapshot() {
        isRunning = false;
        saved = 0;
    }

    void updateSeed(int currentSeed) {
        seedNumber = currentSeed;
    }

    void start() {
        System.out.println("Snapshot starting");

        if (snapshot != null && snapshot.isAlive()) {
            System.out.println("Snapshot already running");

        } else {
            saved = 0;
            isRunning = true;
            snapshotThread();
            snapshot.start();
            System.out.println("Snapshot started");
        }
    }

    void load(int i) {
        System.out.println("Start snapshot loader, from " + i);
        if (snapshot != null && snapshot.isAlive()) {
            System.out.println("Snapshot already running");
        } else {
            loadTracker = i;
            saved = 0;
            isRunning = true;
            snapshotLoadThread();
            snapshot.start();
            System.out.println("Snapshot loader started");
        }
    }

    void stop() {
        System.out.println("Snapshot stopping");

        if (isRunning) {
            System.out.println("Saved snapshots: " + saved);
        } else {
            System.out.println("Snapshot not running");
        }

        isRunning = false;
        if (snapshot != null) snapshot.interrupt();

        // wait for thread to stop
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }

    void status() {
        System.out.println("Running: " + isRunning);
        System.out.println("Saved snapshots: " + saved);
    }
}
