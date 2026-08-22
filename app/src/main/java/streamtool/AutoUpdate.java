package streamtool;

public class AutoUpdate {

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
