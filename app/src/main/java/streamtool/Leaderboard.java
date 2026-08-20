package streamtool;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Leaderboard {

    public static boolean downloadSeed(int seedNumber, int matchId, RuntimeData run) {
        try {
            downloadSeedUnsafe(matchId);
            boolean setId = run.setMatchId(seedNumber, matchId);
            if (!setId) {
                System.out.println("Failed to save match id");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Download error");
            return false;
        }

        return true;
    }

    static void downloadSeedUnsafe(int matchId) throws MalformedURLException, IOException, URISyntaxException {
        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mcsrranked.com/matches/" + matchId).toURL().openStream());
        FileOutputStream out = new FileOutputStream("lb_data/matches/" + matchId + ".json");
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject matchData = Main.readJSON(new File("lb_data/matches/" + matchId + ".json"));
        JSONObject data = matchData.getJSONObject("data");
        JSONArray completions = data.getJSONArray("completions");
        JSONArray players = data.getJSONArray("players");

        System.out.println("Completions:");

        for (int i = 0; i < completions.length(); i++) {
            JSONObject completion = completions.getJSONObject(i);
            String uuid = completion.getString("uuid");
            int time = completion.getInt("time");
            String name = "";

            for (int b = 0; b < players.length(); b++) {
                JSONObject player = players.getJSONObject(b);
                String playerUuid = player.getString("uuid");
                if (uuid.equals(playerUuid)) {
                    name = player.getString("nickname");
                    break;
                }
            }

            System.out.println(name + " " + time);
        }

    }

    public static int[] getMatchIds(String host, int count) throws MalformedURLException, IOException, URISyntaxException {
        int[] result = new int[count];

        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mcsrranked.com/users/" + host + "/matches?type=3&count=" + count).toURL().openStream());
        File file = new File("lb_data/id_request.json");
        FileOutputStream out = new FileOutputStream(file);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject idRequest = Main.readJSON(file);

        String status = (String) idRequest.get("status");
        if (!status.equals("success")) return null;

        JSONArray data = (JSONArray) idRequest.get("data");

        for (int i = 0; i < result.length; i++) result[i] = data.getJSONObject(i).getInt("id");

        return result;
    }

    public static void clearSeeds() {
        for (int i = 1; i <= 8; i++) {
            File file = new File("lb_data/seeds/seed" + i + ".json");
            if (file.exists()) file.delete();
        }
    }

    public static JSONObject loadLeaderboard(int[] overrides) throws IOException {
        JSONObject leaderboard = Main.readJSON(new File("lb_data/leaderboard.json"));
        JSONArray players = (JSONArray) leaderboard.get("players");

        int playerCount = players.length();

        int leagueNumber = (int) leaderboard.get("league");
        int promotions = getPromotions(leagueNumber, playerCount);
        int demotions = getDemotions(leagueNumber, playerCount);

        int page1, page2;

        if (playerCount > 26) {
            page1 = 13; page2 = 13;
        } else {
            page2 = playerCount / 2; page1 = playerCount - page2;
        }

        if (overrides[2] != -1) page1 = overrides[2];
        if (overrides[3] != -1) page2 = overrides[3];

        JSONObject result = new JSONObject();
        result.put("page1", page1);
        result.put("page2", page2);
        result.put("promotions", promotions);
        result.put("demotions", demotions);

        BufferedWriter w11 = new BufferedWriter(new FileWriter(new File("output/lb11.txt")));
        BufferedWriter w12 = new BufferedWriter(new FileWriter(new File("output/lb12.txt")));
        BufferedWriter w13 = new BufferedWriter(new FileWriter(new File("output/lb13.txt")));

        BufferedWriter w21 = new BufferedWriter(new FileWriter(new File("output/lb21.txt")));
        BufferedWriter w22 = new BufferedWriter(new FileWriter(new File("output/lb22.txt")));
        BufferedWriter w23 = new BufferedWriter(new FileWriter(new File("output/lb23.txt")));

        for (int i = 0; i < page1; i++) {
            JSONObject player = (JSONObject) players.get(i);
            w11.write((String) player.get("name")); w11.newLine();
            w12.write((int) player.get("points") + ""); w12.newLine();
            w13.write(((String) player.get("average")).substring(0, 5)); w13.newLine();
        }

        for (int i = 0; i < page2; i++) {
            JSONObject player = (JSONObject) players.get(playerCount - page2 + i);
            w21.write((String) player.get("name")); w21.newLine();
            w22.write((int) player.get("points") + ""); w22.newLine();
            w23.write(((String) player.get("average")).substring(0, 5)); w23.newLine();
        }

        w11.close(); w12.close(); w13.close(); w21.close(); w22.close(); w23.close();

        return result;
    }

    public static boolean genLeaderboard(int seedcount, Data data, RuntimeData run) {

        Player[] regList = data.players;

        int completionPoints = regList.length / 2;

        if (run.overrides[4] != -1) completionPoints = run.overrides[4] / 2;

        if (completionPoints < 0) completionPoints = 0;

        for (int i = 0; i < regList.length; i++) {
            regList[i].lb_points = 0;
            regList[i].lb_comps = 0;
            regList[i].lb_played = false;
            regList[i].lb_time = 0;
        }

        for (int i = 1; i <= seedcount; i++) {

            int matchId = run.getMatchId(i);

            if (matchId == -1) {
                return false;
            }

            File file = new File("lb_data/matches/"+matchId+".json");
            String compKey = "completions";
            String uuidKey = "uuid";

            JSONObject o = Main.readJSON(file);
            o = (JSONObject) o.get("data");
            JSONArray comp = (JSONArray) o.get(compKey);
            JSONArray players = (JSONArray) o.get("players");

            // remove players who are not registered
            for (int eee = 0; eee < players.length(); eee++) {
                JSONObject pl4 = (JSONObject) players.get(eee);
                if (!Comp.isRegistered((String) pl4.get("nickname"), regList)) {
                    players.remove(eee);
                    eee--;
                }
            }

            // mark players as having played
            for (int aw = 0; aw < players.length(); aw++) {
                JSONObject pl5 = (JSONObject) players.get(aw);
                Player player = data.getPlayer((String) pl5.get("nickname"));
                player.lb_played = true;
            }

            // remove completions from players who are not registered
            for (int i2 = 0; i2 < comp.length(); i2++) {
                JSONObject pl3 = (JSONObject) comp.get(i2);
                if (!Comp.isRegistered(Comp.getName((String) pl3.get(uuidKey), players), regList)) {
                    comp.remove(i2);
                    i2--;
                }
            }

            // add points to players
            for (int num = 0; num < comp.length(); num++) {
                JSONObject pl = (JSONObject) comp.get(num);
                Player player = data.getPlayer(Comp.getName((String) pl.get(uuidKey), players));
                int points = 0;
                if (num == 0) points += 5;
                if (num == 1) points += 3;
                if (num == 2) points += 1;
                if (completionPoints > num) points += completionPoints - num;
                if (points == 0) points = 1;
                player.lb_points += points;
                player.lb_comps++;
                player.lb_played = true;
                player.lb_time += (int) pl.get("time");
            }
        }

        JSONObject leaderboard = new JSONObject();

        leaderboard.put("league", data.leagueNumber);
        leaderboard.put("week", data.weekNumber);

        JSONArray lbPlayers = new JSONArray();

        for (int i = 0; i < regList.length; i++) {
            if (regList[i].lb_played) {

                int average = regList[i].lb_time;
                int comps = regList[i].lb_comps;
                int timeLimit = getTimeLimit(data.leagueNumber);
                if (comps < seedcount) average += timeLimit * (seedcount - comps);

                average *= 10;
                average /= seedcount;
                average += 5;
                average /= 10;

                int avgMin = average / 60000;
                int avgSec = (average / 1000) - (avgMin * 60);
                int avgMs = average - (avgMin * 60000) - (avgSec * 1000);

                String avgMinStr, avgSecStr, avgMsStr;
                if (avgMin < 10) avgMinStr = "0" + avgMin; else avgMinStr = "" + avgMin;
                if (avgSec < 10) avgSecStr = "0" + avgSec; else avgSecStr = "" + avgSec;
                if (avgMs < 10) avgMsStr = "00" + avgMs; else if (avgMs < 100) avgMsStr = "0" + avgMs; else avgMsStr = "" + avgMs;


                String avgStr = avgMinStr + ":" + avgSecStr + "." + avgMsStr;

                JSONObject player = new JSONObject();

                player.put("name", regList[i].name);
                player.put("points", regList[i].lb_points);
                player.put("avg_ms", average);
                player.put("average", avgStr);

                lbPlayers.put(player);
            }            
        }

        List<JSONObject> jsonList = new ArrayList<>();
        for (int a = 0; a < lbPlayers.length(); a++) jsonList.add((JSONObject) lbPlayers.get(a));

        Collections.sort(jsonList, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                int aaa = Integer.compare(b.getInt("points"), a.getInt("points"));
                if (aaa != 0) return aaa; else return Integer.compare(a.getInt("avg_ms"), b.getInt("avg_ms"));
            }
        });

        JSONArray sortedLeaderboard = new JSONArray();
        for (JSONObject obj : jsonList) {
            obj.remove("avg_ms");
            sortedLeaderboard.put(obj);
        }

        leaderboard.put("players", sortedLeaderboard);

        File file = new File("lb_data/leaderboard.json");

        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            w.write(leaderboard.toString());
            w.close();
        } catch (IOException e) {
            System.out.println("Failed to write leaderboard");
            return false;
        }

        return true;
    }

    static int getTimeLimit(int leagueNumber) {
        if (leagueNumber == 1) return 780000;
        if (leagueNumber == 2) return 900000;
        if (leagueNumber == 3) return 1020000;
        if (leagueNumber == 4) return 1200000;
        if (leagueNumber == 5) return 1500000;
        if (leagueNumber == 6) return 1800000;
        return 0;
    }

    static int getPromotions(int leagueNumber, int playerCount) {
        if (leagueNumber == 1) return 0;
        int result = playerCount * 15 + 50;
        return result /= 100;
    }

    static int getDemotions(int leagueNumber, int playerCount) {
        if (leagueNumber == 6) return 0;
        if (leagueNumber == 1) {
            int result = playerCount * 20 + 50;
            return result /= 100;
        } else {
            int result = playerCount * 15 + 50;
            return result /= 100;
        }
    }
}
