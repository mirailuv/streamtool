package streamtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Log {
    File logFile;
    boolean timestamp;
    ArrayList<String> lines;

    public Log(File logFile, Boolean timestamp) {
        this.logFile = logFile;
        this.timestamp = timestamp;
        lines = new ArrayList<>();
    }

    void write(String line) {
        lines.add(System.currentTimeMillis() + " " + line);
        try {
            updateFile();
        } catch (IOException e) {
            // log exception somewhere
        }
    }

    void updateFile() throws IOException {
        BufferedWriter w = new BufferedWriter(new FileWriter(logFile));
        for (int i = 0; i < lines.size(); i++) {
            w.write(lines.get(i));
            w.newLine();
        }
        
        w.close();
    }
    
}
