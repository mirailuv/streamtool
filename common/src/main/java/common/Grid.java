package common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;

class Grid {

    int blockWidth;
    int blockHeight;

    int columns;
    int rows;
    int blockScale;

    int blockCount = 0;
    int maxCount;

    ArrayList<Player> players;

    BufferedImage img;

    int width;
    int height;

    int promCount = 1;
    int demCount = 0;

    boolean moving = false;
    int currentFrame = 0;

    public Grid() {
        this(2, 6, 2);
    }

    public Grid(int columns, int rows, int scale) {
        players = new ArrayList<>();

        this.blockWidth = 1800/scale;
        this.width = (blockWidth + 10) * columns - 10;

        this.blockHeight = 300/scale;
        this.height = (blockHeight + 10) * rows - 10;

        this.columns = columns;
        this.rows = rows;
        this.blockScale = scale;
        this.maxCount = rows * columns;

        System.out.println("Created grid " + rows + "x" + columns + " = " + maxCount);

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, width, height);

        g.dispose();

        moving = false;
    }

    void updateBlockCount() {
        int newBlockCount = 0;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.hasBlock()) newBlockCount++;
        }
        if (newBlockCount > blockCount) {
            blockCount = newBlockCount;
            order();
            drawBlocks();

            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                player.setRank(i + 1);
            }
        }
    }

    void drawBlocks() {

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, 1810, 950);

        if (moving == true) currentFrame++;

        if (currentFrame > 60) {
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                player.rank = player.newRank;
                player.points = player.newPoints;
            }

            currentFrame = 60;
            moving = false;
        }

        for (int i = players.size() - 1; i >= 0; i--) {

            Player player = players.get(i);

            if (player.hasBlock() && player.lastBlockFrame != player.block.currentBlockFrame) {
                player.lastBlockFrame = player.block.currentBlockFrame;

                Image image;

                if (player.block.appeared) {
                    image = player.block.getFrame();
                } else {
                    image = player.block.appearAnimation();
                }
                
                int[] pos = getPos(i);
                int x = (blockWidth + 10) * pos[0];
                int y = (blockHeight + 10) * pos[1];
                if (!moving) {
                    g.drawImage(image, x, y, null);
                } else {
                    if (player.rank != player.newRank) {
                        int oldRank = player.rank - 1;
                        int[] oldPos = getPos(oldRank);
                        int ox = (blockWidth + 10) * oldPos[0];
                        int oy = (blockHeight + 10) * oldPos[1];
                        int[] newPos = getPos(i);
                        int nx = (blockWidth + 10) * newPos[0];
                        int ny = (blockHeight + 10) * newPos[1];
                        int dx = ox - nx;
                        int dy = oy - ny;
                        Double cX = dx * currentFrame / 60 + 0d;
                        Double cY = dy * currentFrame / 60 + 0d;
                        x = ox - (int) Math.round(cX);
                        y = oy - (int) Math.round(cY);
                    }

                    g.drawImage(image, x, y, null);
                }
            }
        }

        g.dispose();
    }

    void avgFade(int frame) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).hasBlock()) players.get(i).block.avgFade(frame);
        }
        drawBlocks();
    }

    int[] getPos(int i) {
        int[] res = new int[2];

        res[0] = getColumn(i);
        res[1] = getRow(i, res[0]);

        return res;
    }

    int getColumn(int i) {
        int a = 0;
        while (true) {
            int c = (a + 1) * rows;
            if (i < c) return a;
            a++;
        }
    }

    int getRow(int i, int column) {
        return i - (column * rows);
    }

    void addPlayer(Player player) {

        if (players.size() >= maxCount) {
            System.out.println("No more room for: " + player.username);
            return;
        }

        player.setRank(players.size() + 1);
        player.blockScale = this.blockScale;
        players.add(player);
    }

    void prepPointAnimations() {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.newPoints > player.points) {
                int gain = player.newPoints - player.points;

                int a = players.size() / 2 + 5;
                int b = players.size() / 2 + 2;
                int c = players.size() / 2 - 1;

                int rank = gain;

                if (gain == c) rank -= 1;
                if (gain == b) rank -= 3;
                if (gain == a) rank -= 5;

                int delay = players.size() / 2 - rank;

                delay *= 6;

                if (delay < 0) delay = 0;

                player.block.prepAnimation(gain, player.newPoints, Main.pointColor, delay);
            }
        }
    }

    void drawPointAnimations(int frame) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.hasBlock() && player.block.aReady) {
                player.block.animate(frame);
            }
        }

        drawBlocks();
    }

    void update() {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            player.setPoints(player.newPoints);
            player.timeMath();
        }
        order();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            player.setNewRank(i + 1);

        }
        currentFrame = 0;
        moving = true;
    }

    void finishUpdate() {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setRank(player.newRank);

            boolean demoting = false;
            boolean promoting = false;

            if (player.points == 0 && demCount > 0) demoting = true;
            if (player.rank > blockCount - demCount) demoting = true;
            if (player.rank <= promCount) promoting = true;

            if (player.hasBlock()) {
                Color newColor = Main.mainColor;
                if (demoting) newColor = Main.demoteColor;
                if (promoting) newColor = Main.winColor;

                if (player.block.color.getRGB() != newColor.getRGB()) {
                    player.fadeColor(player.block.color, newColor, 30);
                }
            }
        }

        drawBlocks();
    }

    Player getPlayer(String username) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (username.equals(player.username)) return player;
        }

        return null;
    }

    private void order() {
        players.sort(new Comparator<Player>()  {
            public int compare(Player a, Player b) {
                int aaa = Integer.compare(b.points, a.points);
                if (aaa != 0) return aaa; else {
                    int bbb = Integer.compare(a.totalTime, b.totalTime);
                    if (bbb != 0) return bbb; else {
                        if (b.hasBlock() && a.hasBlock()) return 0; else {
                            if (b.hasBlock()) return 1; else return -1;
                        }
                    }
                }
            }
        });
    }
}