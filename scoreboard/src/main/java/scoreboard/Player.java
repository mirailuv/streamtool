package scoreboard;

import java.awt.Color;

class Player {

    String username;

    int rank;
    int points = 0;

    int combinedTime = 0;
    int completions = 0;
    int totalTime;

    String avgStr;

    int newRank;
    int newPoints;

    int blockScale = 1;
    int lastBlockFrame = 0;

    Block block = null;

    boolean hasBlock() {
        if (block != null) return true; else return false;
    }

    Player(String username) {
        this.username = username;
        timeMath();
    }

    void setNewRank(int newRank) {
        this.newRank = newRank;
    }

    void setNewPoints(int newPoints) {
        this.newPoints = newPoints;
        if (!hasBlock()) addBlock();
    }

    void setRank(int rank) {
        this.rank = rank;
    }

    void setPoints(int points) {
        this.points = points;
    }

    void timeMath() {
        int dnfs = 8 - completions;
        int dnfTime = Main.getDnfTime() * dnfs;
        totalTime = dnfTime + combinedTime;
    }

    void setAverage(int seeds) {
        int dnfTime = (seeds - completions) * Main.getDnfTime();

        int total = combinedTime + dnfTime;

        int average = (int) Math.round((total * 1.0) / seeds);

        int avgMin = average / 60000;
        int avgSec = (average / 1000) - (avgMin * 60);
        int avgMs = average - (avgMin * 60000) - (avgSec * 1000);

        String avgMinStr, avgSecStr, avgMsStr;
        if (avgMin < 10) avgMinStr = "0" + avgMin; else avgMinStr = "" + avgMin;
        if (avgSec < 10) avgSecStr = "0" + avgSec; else avgSecStr = "" + avgSec;
        if (avgMs < 10) avgMsStr = "00" + avgMs; else if (avgMs < 100) avgMsStr = "0" + avgMs; else avgMsStr = "" + avgMs;

        avgStr = avgMinStr + ":" + avgSecStr + "." + avgMsStr;
    }

    void updateBlockAverage() {
        if (hasBlock()) block.setNewAverage(avgStr);
    }

    void addCompletion(int time, int points, int seedNumber) {
        combinedTime += time;
        completions++;
        setNewPoints(this.points + points);
    }

    void updateAvg(int seeds) {
        setAverage(seeds);
        updateBlockAverage();
    }

    void addBlock() {
        block = new Block(Main.mainColor, blockScale);
        block.setPlayer(username);
        block.setPoints(points);
    }

    void changeBlockColor(Color color) {
        if (block == null) return;

        block.newColor(color);
    }

    boolean fadeActive = false;
    int fadeX = 0;
    int fadeN;
    Color fadeA;
    Color fadeB;

    void fadeColor(Color a, Color b, int n) {
        fadeActive = true;
        fadeX = 0;
        fadeN = n;
        fadeA = a;
        fadeB = b;
    }

    void fadeUpdate() {
        if (!hasBlock()) return;
        if (!fadeActive) return;

        fadeX++;
        if (fadeX > fadeN) {
            fadeActive = false;
            return;
        }
        changeBlockColor(Main.fadeColor(fadeA, fadeB, fadeX, fadeN));
    }
}