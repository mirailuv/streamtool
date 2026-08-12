package streamtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Comp {
    public static JSONObject updateCompletions(Player[] regPlayers, int seedNumber) throws IOException {
        int completionPoints = regPlayers.length / 2;

        File file;
        String compKey = "completes";
        String uuidKey = "player";
        boolean needData = false;

        if (seedNumber > 0) {
            file = new File("lb_data/seeds/seed"+seedNumber+".json");
            compKey = "completions";
            uuidKey = "uuid";
            needData = true;
        } else file = new File("spectate_match.json");

        if (!file.exists()) return null;

        JSONObject o = Main.readJSON(file);
        if (needData) o = (JSONObject) o.get("data");
        JSONArray comp = (JSONArray) o.get(compKey);
        JSONArray players = (JSONArray) o.get("players");

        for (int i = 0; i < comp.length(); i++) {
            JSONObject pl3 = (JSONObject) comp.get(i);
            if (!isRegistered(getName((String) pl3.get(uuidKey), players), regPlayers)) {
                comp.remove(i);
                i--;
            }
        }

        BufferedWriter w11 = new BufferedWriter(new FileWriter(new File("output/comp11.txt")));
        BufferedWriter w12 = new BufferedWriter(new FileWriter(new File("output/comp12.txt")));
        BufferedWriter w13 = new BufferedWriter(new FileWriter(new File("output/comp13.txt")));

        BufferedWriter w21 = new BufferedWriter(new FileWriter(new File("output/comp21.txt")));
        BufferedWriter w22 = new BufferedWriter(new FileWriter(new File("output/comp22.txt")));
        BufferedWriter w23 = new BufferedWriter(new FileWriter(new File("output/comp23.txt")));

        int compLength = comp.length();
        if (compLength > 26) compLength = 26;
        int c1ln = 0;
        int c2ln = 0;


        if (compLength > 13) {
            c2ln = compLength / 2;
            c1ln = compLength - c2ln;
        } else c1ln = compLength;

        for (int i = 0; i < c1ln; i++) {
            JSONObject o2 = (JSONObject) comp.get(i);
            String uuid = (String) o2.get(uuidKey);
            int time = (int) o2.get("time");
            int seconds = time / 1000;
            int minutes = seconds / 60;
            seconds = seconds - (minutes * 60);
            String minuteString;
            String secondString;
            if (minutes < 10) minuteString = "0" + minutes; else minuteString = "" + minutes;
            if (seconds < 10) secondString = "0" + seconds; else secondString = "" + seconds;
            String timeString = minuteString + ":" + secondString;
            int points = 0;
            if (i < completionPoints) points += completionPoints - i;
            if (i == 0) points += 5;
            if (i == 1) points += 3;
            if (i == 2) points += 1;
            if (points <= 0) points = 1;
            String pointString = "" + points;

            String name = getName(uuid, players);

            w11.write(name); w11.newLine();
            w12.write(timeString); w12.newLine();
            w13.write(pointString); w13.newLine();
        }

        if (c2ln > 0) for (int i = c1ln; i < c2ln + c1ln; i++) {
            JSONObject o2 = (JSONObject) comp.get(i);
            String uuid = (String) o2.get(uuidKey);
            int time = (int) o2.get("time");
            int seconds = time / 1000;
            int minutes = seconds / 60;
            seconds = seconds - (minutes * 60);
            String minuteString;
            String secondString;
            if (minutes < 10) minuteString = "0" + minutes; else minuteString = "" + minutes;
            if (seconds < 10) secondString = "0" + seconds; else secondString = "" + seconds;
            String timeString = minuteString + ":" + secondString;
            int points = 0;
            if (i < completionPoints) points += completionPoints - i;
            if (i == 0) points += 5;
            if (i == 1) points += 3;
            if (i == 2) points += 1;
            if (points <= 0) points = 1;
            String pointString = "" + points;


            String name = getName(uuid, players);

            w21.write(name); w21.newLine();
            w22.write(timeString); w22.newLine();
            w23.write(pointString); w23.newLine();
        }

        w11.close(); w12.close(); w13.close(); w21.close(); w22.close(); w23.close();

        JSONObject returnData = new JSONObject();
        returnData.put("page1", c1ln);
        returnData.put("page2", c2ln);

        return returnData;
    }

    public static String getName(String uuid, JSONArray players) throws IOException {
        String output = "";

        for (int i = 0; i < players.length(); i++) {
            JSONObject pl = (JSONObject) players.get(i);
            String match = (String) pl.get("uuid");
            if (uuid.equals(match)) output = (String) pl.get("nickname");
        }

        return output;

    }

    public static boolean isRegistered(String name, Player[] regPlayers) {
        for (int i = 0; i < regPlayers.length; i++) if (name.equals(regPlayers[i].name)) return true;
        return false;
    }
}
