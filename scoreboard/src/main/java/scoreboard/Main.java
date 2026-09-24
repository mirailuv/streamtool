package scoreboard;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import common.Api;
import common.FileSelect;
import common.Render;

public class Main {

    public static int league = 1;

    public static int getDnfTime() {
        int time = 60000;
        if (league == 1) time *= 13;
        if (league == 2) time *= 15;
        if (league == 3) time *= 17;
        if (league == 4) time *= 20;
        if (league == 5) time *= 25;
        if (league == 6) time *= 30;

        return time;
    }

    public static Color mainColor = new Color(255, 255, 255, 63);
    public static Color winColor = new Color(127, 255, 127, 63);
    public static Color demoteColor = new Color(255, 127, 127, 63);
    public static Color pointColor = new Color(127, 255, 127, 255);
    public static Color avgColor = new Color(191, 191, 191, 255);


    // STANDALONE WAY OF RENDERING ANIMATIONS AND STUFF
    public static void main(String[] args) throws InterruptedException {

        if (args.length == 0) {
            System.out.println("No folder specified for data");
            return;
        }

        String folder = args[0];

        File folderPath = Paths.get(folder).toFile();
        if (!folderPath.exists()) {
            folderPath.mkdirs();
            File list = Paths.get(folder, "list.json").toFile();
            JSONArray genList = new JSONArray();

            File sel = new FileSelect().select();
            JSONArray a = Api.readJSONArray(sel);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                genList.put(o.get("ign"));
            }

            try {
                BufferedWriter b = new BufferedWriter(new FileWriter(list));
                b.write(genList.toString());
                b.close();
            } catch (IOException e) {
                System.out.println("FAIL");
                return;
            }
        }


        JSONArray playerList = Api.readJSONArray(Paths.get(folder, "list.json").toFile());

        if (args.length > 1) {
            int[] matches = new int[8];

            matches[0] = 13105015;
            matches[1] = 13105469;
            matches[2] = 13105896;
            matches[3] = 13106313;
            matches[4] = 13106964;
            matches[5] = 13107363;
            matches[6] = 13107753;
            matches[7] = 13108128;

            try {
                Datagen.gen(folder, matches, playerList, 2);
            } catch (IOException e) {
                System.out.println("Error generating match data");
                return;
            }
        }

        JSONObject config = Api.readJSON(Paths.get(folder, "config.json").toFile());

        Render r = new Render(60);

        Grid leaderboard = new Grid(playerList.length());

        leaderboard.promCount = config.optInt("prom", 1);
        leaderboard.demCount = config.optInt("dem", 0);

        int seedCount = config.getInt("seeds");
        league = config.getInt("league");

        System.out.println("Seeds: " + seedCount);

        //Collector gc = new Collector();
        //new Thread(gc).start();

        System.out.println("Adding players");

        for (int i = 0; i < playerList.length(); i++) {
            leaderboard.addPlayer(new Player(playerList.getString(i)));
        }

        System.out.println("Added " + leaderboard.players.size() + " players");

        int delayTime = leaderboard.players.size() / 2 * 6;

        Scanner scanner = new Scanner(System.in);

        int seed = 1;

        System.out.println("Ready to start");
        scanner.nextLine();

        int prevCount = 0;

        r.pause();
        r.qWait(60);
        new Thread(r).start();

        while (true) {
            System.out.println("Seed " + seed);

            JSONObject comp = Api.readJSON(Paths.get(folder, "seed" + seed + ".json").toFile());
            JSONArray completions = comp.getJSONArray("completions");

            Grid completionGrid = new Grid(completions.length());

            completionGrid.demCount = 0;
            completionGrid.promCount = 0;

            for (int i = 0; i < completions.length(); i++) {
                JSONObject o = completions.getJSONObject(i);
                String player = o.getString("player");

                leaderboard.getPlayer(player).addCompletion(o.getInt("time"), o.getInt("points"), seed);

                Player pll = new Player(player);
                completionGrid.addPlayer(pll);
                pll.addCompletion(o.getInt("time"), o.getInt("points"), 1);
                pll.updateAvg(1);
                pll.block.appeared = true;
                pll.block.setPoints(pll.newPoints);
                pll.block.setAverage(pll.avgStr);
            }

            int wuh = leaderboard.width;

            if (getColumns(completions.length()) > 1) wuh = completionGrid.width;

            Title compTitle = new Title(wuh, 100, "Completions        ", seed + "/" + seedCount, mainColor, 80, 15);

            completionGrid.drawBlocks();

            ArrayList<BufferedImage> compFadeIn = new ArrayList<>();
            ArrayList<BufferedImage> compFadeOut = new ArrayList<>();

            for (int i = 0; i <= 30; i++) {
                compFadeIn.add(getFrame(completionGrid.img, (i + 0f) / 30, compTitle));
                compFadeOut.add(getFrame(completionGrid.img, (30f - i) / 30, compTitle));
            }

            r.qRender(compFadeIn);
            r.qWait(240);

            r.render();

            r.qRender(compFadeOut);

            completionGrid = null;

            JSONArray dnfs = comp.getJSONArray("dnfs");
            for (int i = 0; i < dnfs.length(); i++) {
                Player player = leaderboard.getPlayer(dnfs.getString(i));
                if (player != null) {
                    player.setNewPoints(leaderboard.getPlayer(dnfs.getString(i)).points);
                }
            }

            for (int i = 0; i < leaderboard.players.size(); i++) leaderboard.players.get(i).updateAvg(seed);

            leaderboard.updateBlockCount();

            Title lbTitle = new Title(leaderboard.width, 100, "Leaderboard        ", seed + "/" + seedCount, mainColor, 80, 15);

            ArrayList<BufferedImage> fadeIn = new ArrayList<>();

            for (int i = 0; i <= 30; i++) {
                fadeIn.add(getFrame(leaderboard.img, (i + 0f) / 30, lbTitle));
            }

            r.qRender(fadeIn);

            ArrayList<BufferedImage> initial = new ArrayList<>();

            if (leaderboard.blockCount > prevCount) {
                System.out.println(leaderboard.blockCount + " > " + prevCount);
                for (int frame = 0; frame <= 31; frame++) {
                    leaderboard.drawBlocks();
                    initial.add(getFrame(leaderboard.img, lbTitle));
                }
                prevCount = leaderboard.blockCount;
                r.qRender(initial);
            } else {
                r.qWait(32);
            }
            Long t = System.currentTimeMillis();
            System.out.println("Prep point animation");
            leaderboard.prepPointAnimations();
            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            int frameCount = 91 + delayTime;

            ArrayList<BufferedImage> pointsAnimation = new ArrayList<>();

            t = System.currentTimeMillis();
            System.out.println("Start point animation");
            for (int frame = 0; frame <= frameCount; frame++) {
                leaderboard.drawPointAnimations(frame);
                pointsAnimation.addLast(getFrame(leaderboard.img, lbTitle));
            }
            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            t = System.currentTimeMillis();
            System.out.println("Prep avg fade");
            leaderboard.prepAvgFade();
            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            t = System.currentTimeMillis();
            System.out.println("Start avg fade");
            for (int frame = 0; frame <= 61; frame++) {
                leaderboard.avgFade(frame);
                pointsAnimation.addLast(getFrame(leaderboard.img, lbTitle));
            }
            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            r.qRender(pointsAnimation);
            r.qWait(30);

            t = System.currentTimeMillis();
            System.out.println("Prep move animation");
            leaderboard.updateBlockElements();
            leaderboard.update();
            leaderboard.prepMoveAnimation();
            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            ArrayList<BufferedImage> moveAnimation = new ArrayList<>();

            t = System.currentTimeMillis();
            System.out.println("Start move animation");

            int moveLength = 60;

            for (int frame = 0; frame <= moveLength + 1; frame++) {
                //leaderboard.drawBlocks();
                leaderboard.drawMoveAnimation(frame, moveLength);
                moveAnimation.addLast(getFrame(leaderboard.img, lbTitle));
            }
            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            r.qRender(moveAnimation);

            leaderboard.finishUpdate();

            t = System.currentTimeMillis();
            System.out.println("Color fade");
            leaderboard.prepColors();

            ArrayList<BufferedImage> colorAnimation = new ArrayList<>();
            
            for (int frame = 0; frame < 30; frame++) {
                for (int i = 0; i < leaderboard.players.size(); i++) {
                    leaderboard.players.get(i).fadeUpdate();
                }
                //leaderboard.drawBlocks();
                leaderboard.drawColors();
                colorAnimation.addLast(getFrame(leaderboard.img, lbTitle));
            }
            //leaderboard.drawBlocks();
            //colorAnimation.addLast(getFrame(leaderboard.img, lbTitle));

            System.out.println("Took: " + (System.currentTimeMillis() - t));
            System.out.println();

            r.qRender(colorAnimation);

            r.qWait(300);

            ArrayList<BufferedImage> fadeOut = new ArrayList<>();

            for (int i = 0; i <= 30; i++) {
                fadeOut.add(getFrame(leaderboard.img, (30f - i) / 30, lbTitle));
            }

            r.render();

            r.qRender(fadeOut);

            seed++;

            if (seed > seedCount) break;

            while (r.renderQ.size() > 10) {
                Thread.sleep(100);
            }
        }

        r.resume();

        leaderboard = null;

        System.out.println("Render complete");

        r.waitRender();

        System.out.println("Play complete");

        r.stop();
        //gc.stop();

        System.out.println("Finished, enter to close");

        scanner.nextLine();

        r.close();
        scanner.close();
    }

    static int getColumns(int pl) {
        if (pl <= 6) return 1;
        if (pl <= 12) return 2;
        if (pl <= 24) return 3;
        if (pl <= 44) return 4;
        if (pl <= 65) return 5;
        if (pl <= 96) return 6;
        if (pl <= 126) return 7;
        if (pl <= 160) return 8;
        if (pl <= 198) return 9;
        if (pl <= 240) return 10;
        if (pl <= 275) return 11;
        if (pl <= 324) return 12;

        throw new Error("Too many players!");
    }

    static int getRows(int pl, int columns) {
        int i = pl / columns;
        if (i * columns < pl) i++;
        if (i < 1) i = 1;
        return i; 
    }

    static BufferedImage getFrame(BufferedImage img) {
        return getFrame(img, false, 1.0f, false, null);
    }

    static BufferedImage getFrame(BufferedImage img, float alpha) {
        return getFrame(img, true, alpha, false, null);
    }

    static BufferedImage getFrame(BufferedImage img, Title title) {
        return getFrame(img, false, 1.0f, true, title);
    }

    static BufferedImage getFrame(BufferedImage img, float alpha, Title title) {
        return getFrame(img, true, alpha, true, title);
    }

    private static BufferedImage getFrame(BufferedImage img, boolean fade, float alpha, boolean hasTitle, Title title) {

        int heightMargin = 0;
        int titleX = 0;
        int titleY = 10;
        if (hasTitle) {
            heightMargin = title.img.getHeight() + 10;
            titleX = (1920 - title.img.getWidth()) / 2;
        }


        BufferedImage frame = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);

        int x = (1920 - img.getWidth()) / 2;
        int y = heightMargin + ((1080 - heightMargin - img.getHeight()) / 2);

        Graphics2D g = frame.createGraphics();

        if (hasTitle) {
            if (fade) {
                g.drawImage(renderWithAlpha(title.img, alpha), titleX, titleY, null);
            } else {
                g.drawImage(title.img, titleX, titleY, null);
            }
        }

        if (fade) {
            g.drawImage(renderWithAlpha(img, alpha), x, y, null);
        } else {
            g.drawImage(img, x, y, null);
        }
        g.dispose();
        return frame;
    }

    static Color fadeColor(Color a, Color b, int x, int n) {
        Double d = (x * 1d) / n;

        int aR = a.getRed();
        int aG = a.getGreen();
        int aB = a.getBlue();
        int aA = a.getAlpha();

        int bR = b.getRed();
        int bG = b.getGreen();
        int bB = b.getBlue();
        int bA = b.getAlpha();

        int dR = aR - bR;
        int dG = aG - bG;
        int dB = aB - bB;
        int dA = aA - bA;

        int nR = aR - (int) Math.round(dR * d);
        int nG = aG - (int) Math.round(dG * d);
        int nB = aB - (int) Math.round(dB * d);
        int nA = aA - (int) Math.round(dA * d);

        return new Color(nR, nG, nB, nA);
    }

    static RescaleOp getOp(int a, int b) {
        float alpha;
        alpha = (a * 1.0f) / (b * 1.0f);
        return getOp(alpha);
    }

    static RescaleOp getOp(float alpha) {
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;
        return new RescaleOp(new float[]{1f, 1f, 1f, alpha}, new float[]{0f, 0f, 0f, 0f}, null);
    }

    static BufferedImage renderWithAlpha(BufferedImage img, float alpha) {
        return getOp(alpha).filter(img, null);
    }
}