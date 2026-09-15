package common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

class Block {
    BufferedImage img;

    BufferedImage pl;
    BufferedImage pts;

    BufferedImage avg;
    int avgFade = 30;
    String newAverage;

    int scale;

    int u1, u5, u6;
    int m1, um;

    boolean aReady = false;
    BufferedImage aImg;
    BufferedImage aFrame;
    int gainPoints;
    int newPoints;
    int delay;

    boolean appeared = false;
    int appearN = 0;
    int appearS = 30;

    Color color;

    int currentBlockFrame = 0;

    public BufferedImage getFrame() {
        combine();
        return img;
    }

    public BufferedImage appearAnimation() {
        if (appearN == appearS) appeared = true;
        combine();
        RescaleOp op = Main.getOp(appearN, appearS);
        appearN++;
        return op.filter(img, null);
    }

    public void prepAnimation(int gainPoints, int newPoints, Color color, int delay) {
        this.delay = delay;
        this.gainPoints = gainPoints;
        this.newPoints = newPoints;

        aImg = new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = aImg.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, u1, u1);
        g.setColor(color);
        g.fillRect(0, 0, u1, u1);

        g.setColor(Color.WHITE);
        String pts = gainPoints + "";

        Font pointFont = new Font("Mojang", 1, 160 / scale);
        g.setFont(pointFont);
        if (gainPoints < 10) {
            g.drawString(pts, 98 / scale, 220 / scale);
        } else {
            g.drawString(pts, 33 / scale, 220 / scale);
        }

        g.dispose();
        currentBlockFrame++;

        aFrame = new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB);

        aReady = true;
    }

    public void avgFade(int frame) {
        if (frame <= 30) avgFade = 30 - frame; else avgFade = frame - 30;
        if (frame == 30) setAverage(newAverage);
        combine();
    }

    public void animate(int frame) {

        int localFrame = frame - delay;

        if (!aReady) return;
        if (localFrame > 11 && localFrame <= 80) return;
        if (localFrame < 1) return;
        if (localFrame > 90) {
            aReady = false;
            return;
        }

        Graphics2D g = aFrame.createGraphics();

        g.setBackground(new Color(0, 0, 0, 0));
        g.drawImage(aImg, 0, 0, null);

        int rY = 0, rH = 0;
        if (localFrame < 11) {
            rY = (int) Math.round(u1 / 10d * localFrame);
            rH = u1 - rY;
        }

        if (localFrame > 80) {
            rH = (int) Math.round(u1 / 10d * (localFrame - 80));
        }

        if (localFrame == 11) {
            setPoints(newPoints);
        }

        g.clearRect(0, rY, u1, rH);

        g.dispose();
        currentBlockFrame++;

        combine();
    }

    Block(Color color, int scale) {
        init(color, scale);
    }

    private void init(Color color, int scale) {
        this.color = color;
        this.scale = scale;

        u6 = 1800 / scale;
        u1 = 300 / scale;
        u5 = 1500 / scale;
        m1 = 10 / scale;
        um = u1 / 2;
        if (m1 < 2) m1 = 2;


        img = new BufferedImage(u6, u1, BufferedImage.TYPE_INT_ARGB);
        pl = new BufferedImage(u5, u1, BufferedImage.TYPE_INT_ARGB);
        pts = new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB);
        avg = new BufferedImage(u5, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g1 = pl.createGraphics();
        g1.setBackground(new Color(0, 0, 0, 0));
        g1.clearRect(0, 0, u5, u1);

        g1.setColor(color);
        g1.fillRect(0, 0, u5, u1);
        g1.dispose();

        Graphics2D g2 = pts.createGraphics();
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(u5, 0, u1, u1);

        g2.setColor(color);
        g2.fillRect(u5, 0, u1, u1);
        g2.dispose();

        Graphics2D g3 = pl.createGraphics();
        g3.setBackground(new Color(0, 0, 0, 0));
        g3.clearRect(0, 0, u5, u1);

        combine();
    }

    void newColor(Color color) {
        this.color = color;

        pl = new BufferedImage(u5, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g1 = pl.createGraphics();
        g1.setBackground(new Color(0, 0, 0, 0));
        g1.clearRect(0, 0, u5, u1);

        g1.setColor(color);
        g1.fillRect(0, 0, u5, u1);
        g1.dispose();

        pts = new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = pts.createGraphics();
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(u5, 0, u1, u1);

        g2.setColor(color);
        g2.fillRect(u5, 0, u1, u1);
        g2.dispose();

        combine();
    }

    private void combine() {
        img = new BufferedImage(u6, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));

        g.clearRect(0, 0, u6, u1);
        g.drawImage(pl, 0, 0, null);
        g.drawImage(Main.getOp(avgFade, 30).filter(avg, null), 0, 0, null);
        g.drawImage(pts, u5, 0, null);
        if (aReady) g.drawImage(aFrame, u5, 0, null);
        g.dispose();
        currentBlockFrame++;
    }

    void setPlayer(String username) {
        pl = new BufferedImage(u5, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = pl.createGraphics();

        int headSize = u1 - (m1 * 2);

        BufferedImage head = Api.getHead(username, headSize);

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, u5, u1);

        g.setColor(color);
        g.fillRect(0, 0, u5, u1);

        g.drawImage(head, m1, m1, null);

        g.setColor(Color.WHITE);
        Font fontName = new Font("Mojang", 1, fontSize(username.length()));
        g.setFont(fontName);
        g.drawString(username, u1 + m1, um);

        g.dispose();
        currentBlockFrame++;
    }

    void setNewAverage(String average) {
        newAverage = average;
    }

    void setAverage(String average) {
        avg = new BufferedImage(u5, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = avg.createGraphics();

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, u5, u1);

        g.setColor(Main.avgColor);

        Font fontName = new Font("Mojang", 1, fontSize(12));
        g.setFont(fontName);
        g.drawString(average, u1 + m1, u1 - (m1 * 2));

        g.dispose();
        currentBlockFrame++;
    }

    void setPoints(int points) {
        pts = new BufferedImage(u1, u1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = pts.createGraphics();

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, u1, u1);

        String pointString = points + "";

        g.setColor(color);
        g.fillRect(0, 0, u1, u1);

        g.setColor(Color.WHITE);

        Font pointFont = new Font("Mojang", 1, 160 / scale);
        g.setFont(pointFont);
        if (points < 10) {
            g.drawString(pointString, 98 / scale, 220 / scale);
        } else {
            g.drawString(pointString, 33 / scale, 220 / scale);
        }
        g.dispose();
        currentBlockFrame++;
    }

    private int fontSize(int length) {
        if (length == 16) return 90 / scale;
        if (length == 15) return 95 / scale;
        if (length == 14) return 100 / scale;
        if (length == 13) return 105 / scale;
        if (length == 12) return 110 / scale;
        if (length == 11) return 115 / scale;

        return 120 / scale;

    }
}