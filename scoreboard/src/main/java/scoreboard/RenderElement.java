package scoreboard;

import java.awt.image.BufferedImage;

public class RenderElement {

    BufferedImage img;
    int x;
    int y;
    float a;
    boolean load;
    boolean cache;

    public RenderElement(BufferedImage img, int x, int y, float a) {
        load = false;
        cache = false;
        this.img = img;
        this.x = x;
        this.y = y;
        this.a = a;
    }

    BufferedImage get() {
        return Main.renderWithAlpha(img, a);
    }

    BufferedImage getLoad() {
        load();
        return get();
    }

    void load() {
        load = true;
    }

    void setCache(boolean cache) {
        this.cache = cache;
    }

    void newImg() {
        load = false;
        int width = img.getWidth();
        int height = img.getHeight();

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    void setAlpha(float a) {
        load = false;
        this.a = a;
    }

    void updateImg(BufferedImage img) {
        load = false;
        this.img = img;
    }
}
