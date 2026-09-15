package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class Datagen {
    public static void gen(String folder, int[] matches, JSONArray playerlist, int league) throws IOException {

        ArrayList<JSONObject> list = new ArrayList<>();

        int playercount = playerlist.length();

        JSONObject config = new JSONObject();

        int prom = 1;
        int dem = 0;

        if (league == 1) {
            dem = (int) Math.round(playercount * 0.2);
        }

        if (league > 1 && league < 6) {
            prom = (int) Math.round(playercount * 0.15);
            dem = prom;
        }

        if (league == 6) {
            prom = (int) Math.round(playercount * 0.15);
        }

        config.put("league", league);
        config.put("prom", prom);
        config.put("dem", dem);
        config.put("seeds", matches.length);

        File configFile = Paths.get(folder, "config.json").toFile();
        BufferedWriter wr = new BufferedWriter(new FileWriter(configFile));
        wr.write(config.toString());
        wr.close();

        for (int i = 0; i < matches.length; i++) {
            File file = Paths.get("lb_data", "matches", matches[i] + ".json").toFile();
            if (!file.exists()) {
                System.out.println("Match not found, downloading");
                try {
                    Api.downloadSeedUnsafe(matches[i]);
                } catch (IOException | URISyntaxException e) {
                    System.out.println("Download failed for seed" + (i + 1));
                    return;
                }
            }

            list.add(Api.readJSON(file));
        }

        for (int i = 0; i < list.size(); i++) {
            JSONObject o = list.get(i);
            JSONObject data = o.getJSONObject("data");
            JSONArray completions = data.getJSONArray("completions");
            JSONArray players = data.getJSONArray("players");

            JSONObject fileData = new JSONObject();
            int seed = i + 1;
            fileData.put("seedNumber", seed);
            fileData.put("completionCount", completions.length());
            JSONArray compData = new JSONArray();

            for (int i2 = 0; i2 < completions.length(); i2++) {
                JSONObject completion = completions.getJSONObject(i2);

                String player = playerFromUuid(completion.getString("uuid"), players);

                if (isRegistered(player, playerlist)) {
                    JSONObject comp = new JSONObject();
                    comp.put("player", player);
                    int placement = i2 + 1;
                    comp.put("placement", placement);
                    comp.put("points", pointsFromPlacement(placement, playercount));
                    comp.put("time", completion.getInt("time"));
                    compData.put(comp);
                } else {
                    completions.remove(i2);
                    i2--;
                }
            }

            JSONArray dnf = new JSONArray();

            for (int i3 = 0; i3 < players.length(); i3++) {
                JSONObject player = players.getJSONObject(i3);
                String name = player.getString("nickname");

                if (!hasCompletion(name, compData) && isRegistered(name, playerlist)) {
                    dnf.put(name);
                }
            }

            fileData.put("completions", compData);
            fileData.put("dnfs", dnf);

            File outFile = Paths.get(folder, "seed" + seed + ".json").toFile();

            BufferedWriter w = new BufferedWriter(new FileWriter(outFile));
            w.write(fileData.toString());
            w.close();
        }

    }

    static boolean isRegistered(String player, JSONArray players) {
        for (int i = 0; i < players.length(); i++) {
            String name = players.getString(i);
            if (player.equals(name)) return true;
        }

        return false;
    }

    static String playerFromUuid(String uuid, JSONArray players) {
        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            String playerUuid = player.getString("uuid");
            if (uuid.equals(playerUuid)) return player.getString("nickname");
        }

        return null;
    }

    static boolean hasCompletion(String name, JSONArray completions) {
        for (int i = 0; i < completions.length(); i++) {
            if (name.equals(completions.getJSONObject(i).getString("player"))) return true;
        }
        return false;
    }

    static int pointsFromPlacement(int placement, int playerCount) {

        int points = (playerCount / 2) - (placement - 1);

        if (placement == 1) points += 5;
        if (placement == 2) points += 3;
        if (placement == 3) points += 1;
        
        if (points < 1) points = 1;

        return points;
    }
}
