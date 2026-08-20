package streamtool;

public class AutoUpdate {

    // TODO make this
    // separate thread that updates povs every second from spectate_match.json
    // automatically stops when the timer runs out or when switching to completions
    // can also be stopped manually with "update stop"

    boolean isRunning;
    RuntimeData run;

    public AutoUpdate(RuntimeData run) {
        this.run = run;
        isRunning = false;
    }

    void start() {
        new Thread() {
            public void run() {
                isRunning = true;
                while(isRunning) {
                    run.commandManager.execute(run.commandParser.getCommand("update nomsg"));

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {}

                    if (!run.timer.isRunning) isRunning = false;
                }
            }      
        }.start();
    }

    void stop() {
        isRunning = false;
    }
    
}
