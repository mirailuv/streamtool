package common;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;

public class Render implements Runnable {

    JFrame frame;

    Canvas canvas;

    boolean running;
    boolean paused = false;
    boolean rendering = false;
    boolean stopped = false;
    ArrayList<RenderObject> renderQ = new ArrayList<>();
    RenderObject current = null;

    int pauseAfter = -1;

    public void run() {
        running = true;

        while (running) {
            if (!paused) {
                if (current != null) {
                    rendering = true;
                    if (current.isBlank) {
                        current.blank--;
                        if (current.blank == 0) {
                            current = null;
                            new Thread(new Runnable() {
                                public void run() {
                                    System.gc();
                                }
                            }).start();
                        }
                    } else if (current.frames.size() > 0) {
                        drawImage(current.frames.removeFirst(), 0, 0);
                    } else {
                        current = null;
                        new Thread(new Runnable() {
                            public void run() {
                                System.gc();
                            }
                        }).start();
                    }
                } else if (renderQ.size() > 0) {
                    rendering = true;
                    current = renderQ.removeFirst();
                    if (pauseAfter > -1) pauseAfter--;
                    if (pauseAfter == 0) pause();
                } else {
                    rendering = false;
                }
            }
            try {
                Long sleepTime = Main.sleepTime(60);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {}
        }

        stopped = true;
    }

    public void stop() {
        running = false;
        while(!stopped) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
        pauseAfter = -1;
    }

    public void render() {
        paused = false;
        pauseAfter = renderQ.size();
    }


    public void waitRender() {
        while (rendering) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }

    public Render() {
        frame = new JFrame();
        canvas = new Canvas();

        AtomicBoolean updating = new AtomicBoolean();
        frame.addPropertyChangeListener("graphicsConfiguration", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (updating.compareAndSet(false, true)) {
                    try {
                        frame.setBackground(new Color(0, 0, 0, 255));
                        frame.setBackground(new Color(0, 0, 0, 0));
                    } finally {
                    updating.set(false);
                    }
                }
            }
        });

        frame.setTitle("Streamtool Renderer");
        frame.setUndecorated(true);
        frame.setSize(1920, 1080);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setBackground(new Color(0, 0, 0, 0));      

        frame.add(canvas);
        canvas.setBackground(new Color(0, 0, 0, 0));
        canvas.setVisible(true);

        while(!canvas.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        canvas.createBufferStrategy(2);
    }

    void clear() {
        Graphics g = canvas.getBufferStrategy().getDrawGraphics();
        if (g != null) g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.getBufferStrategy().show();
    }

    void close() {
        frame.setVisible(false);
        frame.dispose();
    }

    private void drawImage(BufferedImage img, int x, int y) {
        Graphics g = canvas.getBufferStrategy().getDrawGraphics();
        if (g != null) {
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.drawImage(img, x, y, null);
        }
        canvas.getBufferStrategy().show();
        Toolkit.getDefaultToolkit().sync();
    }

    void qRender(ArrayList<BufferedImage> frames) {
        System.out.println("Queued " + frames.size() + " frames");

        RenderObject o = new RenderObject(frames);
        renderQ.addLast(o);
    }

    void qWait(int frames) {
        System.out.println("Queued " + frames + " blanks");

        RenderObject o = new RenderObject(frames);
        renderQ.addLast(o);
    }

    void test() {
        Random random = new Random();
        Color color = new Color(random.nextInt(256),random.nextInt(256),random.nextInt(256),random.nextInt(256));
        Graphics g = canvas.getBufferStrategy().getDrawGraphics();
        int width = random.nextInt(canvas.getWidth());
        int height = random.nextInt(canvas.getHeight());

        if (g != null) {
            g.setColor(color);
            g.fillRect(0, 0, width, height);
        }
        canvas.getBufferStrategy().show();
    }
}

class RenderObject {
    boolean isBlank; // if there's something to render
    int blank; // delay in frames
    ArrayList<BufferedImage> frames; // frames to render if not delay

    RenderObject(int blank) {
        this.isBlank = true;
        this.blank = blank;
    }

    RenderObject(ArrayList<BufferedImage> frames) {
        this.isBlank = false;
        this.frames = frames;
    }
}