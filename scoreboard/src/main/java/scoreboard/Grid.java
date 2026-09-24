package scoreboard;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;

class Grid {

    static int[] getScale(int pl) {
        int[] result = new int[3];
        result[0] = Main.getColumns(pl);
        result[1] = Main.getRows(pl, result[0]);
        if (result[0] < 2) result[2] = 2; else result[2] = result[0];
        return result;
    }

    int blockWidth;
    int blockHeight;

    int columns;
    int rows;
    int blockScale;

    int blockCount = 0;
    int maxCount;

    ArrayList<Player> players;

    BufferedImage img;

    ArrayList<BufferedImage> prep;

    int width;
    int height;

    int promCount = 1;
    int demCount = 0;

    boolean moving = false;
    int currentFrame = 0;

    public Grid(int playercount) {
        players = new ArrayList<>();

        resizeGrid(playercount);

        moving = false;
    }

    void resizeGrid(int playercount) {
        int[] scaling = getScale(playercount);

        columns = scaling[0];
        rows = scaling[1];
        blockScale = scaling[2];

        blockWidth = 1800/blockScale;
        width = (blockWidth + 10) * columns - 10;

        blockHeight = 300/blockScale;
        height = (blockHeight + 10) * rows - 10;

        maxCount = playercount;

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

    void prepColors() {
        prep = new ArrayList<>();
        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));

        Graphics2D g0 = prep.get(0).createGraphics();
        Graphics2D g1 = prep.get(1).createGraphics();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            if (player.hasBlock()) {
                int[] pos = getPos(i);
                int x = (blockWidth + 10) * pos[0];
                int y = (blockHeight + 10) * pos[1];
                if (!player.fadeActive) {
                    g0.drawImage(player.block.elements.background.getLoad(), x, y, null);
                }
                g1.drawImage(player.block.elements.player.getLoad(), x, y, null);
                g1.drawImage(player.block.elements.average.getLoad(), x + player.block.elements.average.x, y + player.block.elements.average.y, null);
                g1.drawImage(player.block.elements.points.getLoad(), x + player.block.elements.points.x, y, null);
            }
        }
    }

    void drawColors() {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage f = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Graphics2D gf = f.createGraphics();
        g.drawImage(prep.get(0), 0, 0, null);

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            if (player.hasBlock() && !player.block.elements.background.load) {
                int[] pos = getPos(i);
                int x = (blockWidth + 10) * pos[0];
                int y = (blockHeight + 10) * pos[1];

                gf.drawImage(player.block.elements.background.getLoad(), x, y, null);
            }
        }

        g.drawImage(f, 0, 0, null);
        g.drawImage(prep.get(1), 0, 0, null);

        g.dispose();
    }

    @Deprecated 
    void drawBlocks() {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, width, height);

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

            if (player.hasBlock()) {
                if (!player.block.appeared) {
                    player.block.appearAnimation();
                }

                BufferedImage image = player.block.getFrame();
                
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

    void prepMoveAnimation() {
        prep = new ArrayList<>();
        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));

        Graphics2D g = prep.get(0).createGraphics();

        for (int i = 0; i < players.size(); i++) if (players.get(i).hasBlock()) {
            Player player = players.get(i);

            if (player.rank == player.newRank) {
                player.block.move = false;

                int[] pos = getPos(i);
                int x = (blockWidth + 10) * pos[0];
                int y = (blockHeight + 10) * pos[1];

                g.drawImage(player.block.getFrame(), x, y, null);
            } else player.block.move = true;
        }
    }

    void drawMoveAnimation(int frame, int length) {

        int animationLength = length;

        if (frame > animationLength) {
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                player.rank = player.newRank;
                player.points = player.newPoints;
            }

            moving = false;

            return;
        }


        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.drawImage(prep.get(0), 0, 0, null);

        BufferedImage f = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gf = f.createGraphics();

        for (int i = players.size() - 1; i >= 0; i--) {
            Player player = players.get(i);

            if (player.hasBlock() && player.block.move) {
                Image image;

                image = player.block.getFrame();
                
                int oldRank = player.rank - 1;
                int[] oldPos = getPos(oldRank);
                int ox = (blockWidth + 10) * oldPos[0];
                int oy = (blockHeight + 10) * oldPos[1];
                int[] newPos = getPos(i);
                int nx = (blockWidth + 10) * newPos[0];
                int ny = (blockHeight + 10) * newPos[1];
                int dx = ox - nx;
                int dy = oy - ny;
                Double cX = dx * frame / animationLength + 0d;
                Double cY = dy * frame / animationLength + 0d;
                int x = ox - (int) Math.round(cX);
                int y = oy - (int) Math.round(cY);

                gf.drawImage(image, x, y, null);
            }
        }

        gf.dispose();

        g.drawImage(f, 0, 0, null);
        g.dispose();
    }

    void prepAvgFade() {
        prep = new ArrayList<>();

        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));

        Graphics2D g = prep.get(0).createGraphics();

        for (int i = 0; i < players.size(); i++) if (players.get(i).hasBlock()) {
            Player player = players.get(i);

            int[] pos = getPos(i);
            int x = (blockWidth + 10) * pos[0];
            int y = (blockHeight + 10) * pos[1];

            g.drawImage(player.block.elements.background.getLoad(), x, y, null);
            g.drawImage(player.block.elements.player.getLoad(), x, y, null);
            g.drawImage(player.block.elements.points.getLoad(), x + player.block.elements.points.x, y + player.block.elements.points.y, null);
        }
    }

    void avgFade(int frame) {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(prep.get(0), 0, 0, null);

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.hasBlock()) { 
                player.block.avgFade(frame);

                int[] pos = getPos(i);
                int x = (blockWidth + 10) * pos[0];
                int y = (blockHeight + 10) * pos[1];

                g.drawImage(player.block.elements.average.getLoad(), x + player.block.elements.average.x, y + player.block.elements.average.y, null);
            }
        }
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

        prep = new ArrayList<>();

        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
        prep.add(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));

        Graphics2D g = prep.get(0).createGraphics();
        Graphics2D g1 = prep.get(1).createGraphics();

        for (int i = 0; i < players.size(); i++) if (players.get(i).hasBlock()) {
            Player player = players.get(i);

            int[] pos = getPos(i);
            int x = (blockWidth + 10) * pos[0];
            int y = (blockHeight + 10) * pos[1];

            g.drawImage(player.block.elements.background.getLoad(), x, y, null);
            g.drawImage(player.block.elements.player.getLoad(), x, y, null);
            g.drawImage(player.block.elements.average.getLoad(), x + player.block.elements.average.x, y + player.block.elements.average.y, null);

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

                g1.drawImage(player.block.elements.points.getLoad(), x + player.block.elements.points.x, y + player.block.elements.points.y, null);
            } else {
                g.drawImage(player.block.elements.points.getLoad(), x + player.block.elements.points.x, y + player.block.elements.points.y, null);
            }
        }
        g.dispose();
        g1.dispose();
    }

    void drawPointAnimations(int frame) {

        Graphics2D gp = prep.get(1).createGraphics();
        gp.setBackground(new Color(0, 0, 0, 0));

        Graphics2D gc = prep.get(2).createGraphics();
        gc.setBackground(new Color(0, 0, 0, 0));

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.hasBlock() && player.block.aReady) {
                player.block.animate(frame);

                if (!player.block.elements.points.load) {
                    int[] pos = getPos(i);
                    int x = (blockWidth + 10) * pos[0];
                    int y = (blockHeight + 10) * pos[1];

                    gp.clearRect(x + player.block.elements.points.x, y + player.block.elements.points.y, player.block.elements.points.img.getWidth(), player.block.elements.points.img.getHeight());
                    gp.drawImage(player.block.elements.points.getLoad(), x + player.block.elements.points.x, y + player.block.elements.points.y, null);
                }
                
                if (!player.block.elements.newPoints.load) {
                    int[] pos = getPos(i);
                    int x = (blockWidth + 10) * pos[0];
                    int y = (blockHeight + 10) * pos[1];

                    gc.clearRect(x + player.block.elements.newPoints.x, y + player.block.elements.newPoints.y, player.block.elements.newPoints.img.getWidth(), player.block.elements.newPoints.img.getHeight());
                    gc.drawImage(player.block.elements.newPoints.getLoad(), x + player.block.elements.newPoints.x, y + player.block.elements.newPoints.y, null);
                }
            }
        }

        gp.dispose();
        gc.dispose();

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(prep.get(0), 0, 0, null);
        g.drawImage(prep.get(1), 0, 0, null);
        g.drawImage(prep.get(2), 0, 0, null);

        g.dispose();
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

                if (true) {
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

    public void updateBlockElements() {
        for (int i = 0; i < players.size(); i++) if (players.get(i).hasBlock()) players.get(i).block.elements.update();
    }
}