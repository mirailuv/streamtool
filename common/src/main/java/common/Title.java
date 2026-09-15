package common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Title {
    BufferedImage img;
    
    public Title(int width, int height, String left, String right, Color color, int fontSize, int margin) {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(color);
        g.fillRect(0, 0, width, height);

        Font font = new Font("Mojang", 1, fontSize);

        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(left, margin, height - margin);
        g.drawString(right, width - 200, height - margin);

        g.dispose();
    }
}
