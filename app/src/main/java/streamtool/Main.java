package streamtool;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

    public static void main(String[] args) throws URISyntaxException, InterruptedException, IOException {

        Files.createDirectories(Paths.get("lb_data"));
        Files.createDirectories(Paths.get("out_heads"));
        Files.createDirectories(Paths.get("output"));

        int[] overrides = {-1, -1, -1, -1};

        JSONObject obsLayout = readJSON(new File("obs_layout.json"));

        int portRange = obsLayout.getInt("portRange"); // ports for streamlink feeds, they are this value + playerId
        boolean useStreamlink = obsLayout.getBoolean("useStreamlink");

        JSONArray bH = obsLayout.getJSONArray("boardHeight");
        int[] boardHeight = new int[bH.length()];
        for (int i = 0; i < boardHeight.length; i++) boardHeight[i] = bH.getInt(i);

        JSONArray pzH = obsLayout.getJSONArray("promoteHeight");
        int[] promoteZoneHeight = new int[pzH.length()];
        for (int i = 0; i < promoteZoneHeight.length; i++) promoteZoneHeight[i] = pzH.getInt(i);

        JSONArray dZ = obsLayout.getJSONArray("demoteZonePos");
        int[] demoteZonePos = new int[dZ.length()];
        for (int i = 0; i < demoteZonePos.length; i++) demoteZonePos[i] = dZ.getInt(i);

        JSONObject scenes = (JSONObject) obsLayout.get("scenes");
        JSONObject audio = (JSONObject) obsLayout.get("audio");
        JSONObject paths = (JSONObject) obsLayout.get("paths");

        String imagePath = (String) paths.get("heads");
        String seediconPath = (String) paths.get("seed_icons");
        String seedimagePath = (String) paths.get("seed_images");

        JSONObject seedList = new JSONObject();
        int currentSeed = 1;

        boolean enableSocket = true;
        boolean muted = true;

        if (args.length > 0) enableSocket = false;

        FileSelect fs = new FileSelect();

        Client client = new Client(new URI("ws://127.0.0.1:4455"), enableSocket, useStreamlink);
        if (client.enable) client.connect();

        Scanner scanner = new Scanner(System.in);

        Thread.sleep(1000);

        if (client.enable) client.send("{\"op\": 1, \"d\": {\"rpcVersion\": 1, \"eventSubscriptions\": 0}}");

        Data data = new Data();

        String hostName = "";

        boolean needSave = false;

        Snapshot snapshot = new Snapshot();
        Timer timer = new Timer(client);

        while(true) {

            if (needSave) {
                System.out.println("Saving data");
                saveData(data, seedList, currentSeed, hostName);
                needSave = false;
            }

            System.out.println();
            System.out.print("> ");
            String s = scanner.next();
            if (s.equals("quit")) break;

            if (s.equals("restore")) {
                System.out.println("Loading previous values");

                JSONObject object = getData();
                seedList = object.getJSONObject("seedList");
                data.weekNumber = object.getInt("week");
                data.leagueNumber = object.getInt("league");
                hostName = object.getString("host");
                currentSeed = object.getInt("currentSeed");

                data.clearPlayers();
                JSONArray array = object.getJSONArray("players");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    String name = o.getString("name");
                    String twitch = o.getString("twitch");
                    int id = o.getInt("id");
                    System.out.println(id + " " + name);
                    Player player = new Player(name, twitch, id);
                    player.live = o.getBoolean("live");
                    data.addPlayer(player);
                }

                System.out.println("Import complete");
            }

            if (s.equals("reload")) {
                System.out.println("Reloading obs layout");

                obsLayout = readJSON(new File("obs_layout.json"));

                bH = obsLayout.getJSONArray("boardHeight");
                boardHeight = new int[bH.length()];
                for (int i = 0; i < boardHeight.length; i++) boardHeight[i] = bH.getInt(i);

                pzH = obsLayout.getJSONArray("promoteHeight");
                promoteZoneHeight = new int[pzH.length()];
                for (int i = 0; i < promoteZoneHeight.length; i++) promoteZoneHeight[i] = pzH.getInt(i);

                dZ = obsLayout.getJSONArray("demoteZonePos");
                demoteZonePos = new int[dZ.length()];
                for (int i = 0; i < demoteZonePos.length; i++) demoteZonePos[i] = dZ.getInt(i);

                scenes = (JSONObject) obsLayout.get("scenes");
                audio = (JSONObject) obsLayout.get("audio");
                paths = (JSONObject) obsLayout.get("paths");

                imagePath = (String) paths.get("heads");
                seediconPath = (String) paths.get("seed_icons");
                seedimagePath = (String) paths.get("seed_images");
            }

            if (s.equals("override")) {
                int index = scanner.nextInt();
                if (index == -1) {
                    System.out.println("Reset overrides");
                    for (int i = 0; i < overrides.length; i++) {
                        overrides[i] = -1;
                    }
                } else {
                    if (index >= 0 & index < 4) {
                        int num = scanner.nextInt();
                        overrides[index] = num;
                        System.out.println("Set override");

                    } else System.out.println("index out of range");
                }
            }

            if (s.equals("toggleMute")) {
                if (muted) muted = false; else muted = true;

                setMute(client, audio, muted);
            }

            if (s.equals("main")) {
                String by = scanner.next();
                String[] povs = new String[0];

                switch (by) {
                    case "p", "points":
                        System.out.println("Points");
                        povs = data.topPoints();
                        break;
                    case "s", "splits":
                        System.out.println("Splits");
                        povs = data.topSplits();
                        break;
                    case "h", "hybrid":
                        System.out.println("Hybrid");
                        povs = data.topHybrid();
                        break;
                    case "r", "random":
                        System.out.println("Random");
                        povs = data.randomPovs();
                        break;
                    default:
                        System.out.println("valid arguments: p / points, s / splits, h / hybrid");
                        break;
                }

                if (povs.length > 0) {

                    int safety = povs.length;
                    if (safety > 4) safety = 4;

                    for (int i = 0; i < safety; i++) {
                        client.showPlayer(data.getPlayer(povs[i]), i + 1, imagePath, portRange, false);
                    }

                    client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) scenes.get("gameplayScenes")).get("gameplay" + safety) + "\"}}}");
                }
            }

            if (s.equals("show")) {
                String a = scanner.next();
                int b = 10000000;
                try {
                    b = Integer.parseInt(a);
                } catch (Exception e) {}
                String c = scanner.next();
                int d = 10000000;
                try {
                    d = Integer.parseInt(c);
                } catch (Exception e) {}
                if (b < data.players.length & d >= 1 & d <= 4) {
                    if (data.players[b].live) if (client.enable) client.showPlayer(data.players[b], d, imagePath, portRange);
                    System.out.println("Done!");
                } else System.out.println("Index out of range");
            }

            if (s.equals("import")) {
                needSave = true;
                System.out.println("Select file");
                File file = fs.select();

                String fileName = file.getName();
                String lnum = fileName.substring(5, 6);
                String wnum = fileName.substring(7, 9);

                int leagueNumber = Integer.parseInt(lnum);
                int weekNumber = Integer.parseInt(wnum);

                data.leagueNumber = leagueNumber;
                data.weekNumber = weekNumber;

                System.out.println("League " + leagueNumber + " Week " + weekNumber);

                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"League\", \"overlay\": true, \"inputSettings\": {\"text\":\"League " + leagueNumber + "\"}}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Week\", \"overlay\": true, \"inputSettings\": {\"text\":\"Week " + weekNumber + "\"}}}}");

                if (file.exists()) {
                    data.clearPlayers();
                    JSONArray array = readJSONArray(file);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject o = array.getJSONObject(i);
                        String name = o.getString("ign");
                        String twitch = fixLink(o.getString("twitch_username"));
                        int id = data.players.length;
                        System.out.println(id + " " + name);
                        Player player = new Player(name, twitch, id);
                        data.addPlayer(player);
                    }

                    System.out.println("Import complete");
                    System.out.println("Type list and make sure the twitch names are formatted correctly and edit with editTwitch if needed");
                } else System.out.println("Invalid file");

                int playerCount = data.players.length;
                int timeLimit = getTimeLimit(leagueNumber);

                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"numPlayers\", \"overlay\": true, \"inputSettings\": {\"text\":\"Players: " + playerCount + "\"}}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"time text\", \"overlay\": true, \"inputSettings\": {\"text\":\"Time limit: " + timeLimit + " minutes\"}}}}");
            }

            if (s.equals("getOrder")) {
                System.out.println("Downloading seed order from api and formatting it");
                downloadOrder(data.leagueNumber, data.weekNumber);
            }

            if (s.equals("importSeeds")) {
                needSave = true;
                System.out.println("Select file");
                File file = fs.select();
                seedList = readJSON(file);
                JSONArray seed_data = (JSONArray) seedList.get("data");

                for (int i = 0; i < seed_data.length(); i++) {
                    int seed_number = i + 1;
                    JSONObject this_seed = (JSONObject) seed_data.get(i);
                    String seed_type = (String) this_seed.get("type");
                    String seedTypeText = seed_type.replace("_", " ");

                    System.out.println("Seed " + seed_number + " = " + seedTypeText);

                    client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"text " + seed_number + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
                    client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"icon " + seed_number + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + seediconPath + seed_type + ".png" + "\"}}}}");
                }

                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + currentSeed + "\"}}}}");

            }

            if (s.equals("next")) {
                needSave = true;

                unHide(data);

                currentSeed++;

                System.out.println("Seed " + currentSeed);

                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + currentSeed + "\"}}}}");

                // load seed information
                int seed_number = currentSeed;
                JSONArray seed_data = (JSONArray) seedList.get("data");
                JSONObject this_seed = (JSONObject) seed_data.get(seed_number - 1);
                String seed_type = (String) this_seed.get("type");
                String seedTypeText = seed_type.replace("_", " ");

                // update seed number
                        
                // set seed information
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed number\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seed_number + "\"}}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + currentSeed + "\"}}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype text\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype icon\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + seediconPath + seed_type + ".png" + "\"}}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed_background\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + seedimagePath + seed_type + ".png" + "\"}}}}");

                // switch to next seed scene
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("nextseedScene") + "\"}}}");

            }

            if (s.equals("setSeed")) {
                needSave = true;
                currentSeed = scanner.nextInt();
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + currentSeed + "\"}}}}");
            }

            if (s.equals("scene")) {
                String scene = scanner.next();
                switch(scene) {
                    case "im", "intermission":
                        System.out.println("Intermission");
                        muted = true;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("intermissionScene") + "\"}}}");
                        break;
                    case "ns", "nextseed":
                        System.out.println("Next seed");
                        muted = false;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + currentSeed + "\"}}}}");

                        // load seed information
                        int seed_number = currentSeed;
                        JSONArray seed_data = (JSONArray) seedList.get("data");
                        JSONObject this_seed = (JSONObject) seed_data.get(seed_number - 1);
                        String seed_type = (String) this_seed.get("type");
                        String seedTypeText = seed_type.replace("_", " ");
                        
                        // set seed information
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed number\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seed_number + "\"}}}}");
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + currentSeed + "\"}}}}");
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype text\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype icon\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + seediconPath + seed_type + ".png" + "\"}}}}");
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed_background\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + seedimagePath + seed_type + ".png" + "\"}}}}");

                        // switch to next seed scene
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("nextseedScene") + "\"}}}");
                        break;
                    case "sl", "seedlist":
                        System.out.println("Seed list");
                        muted = false;
                        setMute(client, audio, muted);

                        int seed_count = (int) seedList.get("seedcount");
                        JSONObject seedlistScenes = (JSONObject) scenes.get("seedlistScenes");
                        String seedlistScene = (String) seedlistScenes.get("seeds" + seed_count);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + seedlistScene + "\"}}}");

                        break;
                    case "sp", "spec", "spectator":
                        System.out.println("Spectator");
                        muted = false;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("spectatorScene") + "\"}}}");
                        break;
                    case "pl", "players", "main":
                        System.out.println("Players");
                        muted = false;
                        setMute(client, audio, muted);
                        int pov_count = 4;
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) scenes.get("gameplayScenes")).get("gameplay" + pov_count) + "\"}}}");
                        break;
                    case "lb", "leaderboard":
                        System.out.println("Leaderboard");
                        muted = false;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("leaderboardScene") + "\"}}}");
                        break;
                    case "co", "com", "commentators": 
                        System.out.println("Commentators");
                        muted = false;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("commentatorScene") + "\"}}}");
                        break;
                    case "cm", "completions":
                        System.out.println("Completions");
                        muted = false;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("completionScene") + "\"}}}");
                        break;
                    case "iv", "interview":
                        System.out.println("Interview");
                        muted = false;
                        setMute(client, audio, muted);
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("interviewScene") + "\"}}}");
                        break;
                    default:
                        System.out.println("Unknown scene");
                        break;
                }
            }

            if (s.equals("interview")) {
                String player = scanner.next();

                System.out.println("Interview: " + player);

                GetImg.getInterviewImg(player);
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"interview player\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + player + "\"}}}}");

                muted = false;
                setMute(client, audio, muted);
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("interviewScene") + "\"}}}");

            }

            if (s.equals("host")) {
                needSave = true;
                hostName = scanner.next();
                System.out.println("Set host to " + hostName);
            }

            if (s.equals("autodl")) {
                int seedNumber = currentSeed;
                if (!hostName.equals("")) {
                    int[] ids = Leaderboard.getMatchIds(hostName, 1);
                    if (ids != null) {
                        int matchId = ids[0];
                        System.out.println("Downloading seed " + seedNumber + " from id " + matchId);
                        Leaderboard.downloadSeed(seedNumber, matchId);
                    }
                }
            }

            if (s.equals("dl")) {
                int seedNumber = scanner.nextInt();
                int matchId = scanner.nextInt();
                System.out.println("Downloading seed " + seedNumber + " from id " + matchId);
                Leaderboard.downloadSeed(seedNumber, matchId);
            }

            if (s.equals("clear")) {
                Leaderboard.clearSeeds();
            }

            if (s.equals("getid")) {
                String host = scanner.next();
                int count = 1;
                int[] ids = Leaderboard.getMatchIds(host, count);
                System.out.println("Match ids");
                if (ids != null) for (int i = ids.length - 1; i >= 0; i--) System.out.println(ids[i]);
            }

            if (s.equals("getids")) {
                String host = scanner.next();
                int count = scanner.nextInt();
                int[] ids = Leaderboard.getMatchIds(host, count);
                System.out.println("Match ids");
                if (ids != null) for (int i = ids.length - 1; i >= 0; i--) System.out.println(ids[i]);
            }

            if (s.equals("lb")) {

                int seedcount = scanner.nextInt();

                System.out.println("Showing leaderboard");
                Leaderboard.genLeaderboard(seedcount, data);

                // update leaderboard txt files
                JSONObject lbData = Leaderboard.loadLeaderboard(overrides);

                // import return data
                int promCount = lbData.getInt("promotions");
                int demCount = lbData.getInt("demotions");
                int page1 = lbData.getInt("page1");
                int page2 = lbData.getInt("page2");

                // enter overrides
                if (overrides[0] != -1) promCount = overrides[0];
                if (overrides[1] != -1) demCount = overrides[1];

                // get leaderboard height data

                int page1height = 2000; int page2height = 2000;

                if (page1 > 0) page1height = boardHeight[page1 - 1];
                if (page2 > 0) page2height = boardHeight[page2 - 1];

                int promHeight = 2000;
                if (promCount > 0) promHeight = promoteZoneHeight[promCount - 1];

                int demPos = 750;
                int demMath = page2 - demCount + 1;
                if (demMath > 0 & demMath <= 13) demPos = demoteZonePos[demMath - 1];

                //System.out.println(promCount + " " + promHeight);
                //System.out.println(demCount + " " + demHeight + " " + demPos);

                JSONObject lbIds = scenes.getJSONObject("lbIds");

                // leaderboard transformation
                client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + scenes.get("leaderboardScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + lbIds.get("lb1") + ", \"sceneItemTransform\": {\"cropBottom\": " + page1height +"}}}}");
                client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + scenes.get("leaderboardScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + lbIds.get("lb2") + ", \"sceneItemTransform\": {\"cropBottom\": " + page2height +"}}}}");

                // promotion transformation
                int promId = lbIds.getInt("prom");
                client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"lb1\", \"sceneUuid\": \"\", \"sceneItemId\": " + promId + ", \"sceneItemTransform\": {\"cropBottom\": " + promHeight + "}}}}");

                // demotion transformation
                int demId = lbIds.getInt("dem");
                client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"lb2\", \"sceneUuid\": \"\", \"sceneItemId\": " + demId + ", \"sceneItemTransform\": {\"positionY\": " + demPos + "}}}}");

                Thread.sleep(1000);

                // switch to lb scene
                muted = false;
                setMute(client, audio, muted);
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("leaderboardScene") + "\"}}}");
            }

            if (s.equals("killFeeds")) {
                Streamlink.killAll();
            }

            if (s.equals("comp")) {
                
                JSONObject layoutData = Comp.updateCompletions(data.players, scanner.nextInt());

                System.out.println("Showing completions");

                int page1 = (int) layoutData.get("page1");
                int page2 = (int) layoutData.get("page2");
                boolean showPage2 = true;
                if (page2 == 0) showPage2 = false;

                int page1height = 2000; int page2height = 2000;

                if (page1 > 0) page1height = boardHeight[page1 - 1];
                if (page2 > 0) page2height = boardHeight[page2 - 1];

                JSONObject compIds = scenes.getJSONObject("compIds");

                // set page heights
                client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + scenes.get("completionScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + compIds.get("comp1") + ", \"sceneItemTransform\": {\"cropBottom\": " + page1height +"}}}}");
                client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + scenes.get("completionScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + compIds.get("comp2") + ", \"sceneItemTransform\": {\"cropBottom\": " + page2height +"}}}}");

                // wait 1 second for text sources to update
                Thread.sleep(1000);

                // switch to comp scene and set page 2 visibility

                muted = false;
                setMute(client, audio, muted);

                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("completionScene") + "\"}}}");
                client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetSceneItemEnabled\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + scenes.get("completionScene") + "\", \"sceneItemId\": " + (scenes.getJSONObject("compIds")).get("comp2") + ", \"sceneItemEnabled\": " + showPage2 + "}}}");
            }

            if (s.equals("split")) {
                System.out.println("Splits");
                data.updateSplits();

                data.printSplits();
            }

            if (s.equals("addPlayer")) {
                needSave = true;
                String a = scanner.next();

                String apiTwitch = getTwitch(a);
                String b;

                if (apiTwitch != null) {
                    b = apiTwitch;
                    System.out.println("got twitch from ranked api");
                    System.out.println("twitch = " + b);
                } else {
                    System.out.println("their twitch / none");
                    System.out.print("twitch = ");
                    b = scanner.next();
                }

                data.addPlayer(a, b);
                System.out.println("Done!");
                System.out.println();
            }

            if (s.equals("toggleLive")) {
                needSave = true;
                String a = scanner.next();
                int b = 10000000;
                try {
                    b = Integer.parseInt(a);
                } catch (Exception e) {}
                if (b < data.players.length) {
                    data.toggleLive(b);
                } else System.out.println("Index out of range");
            }

            if (s.equals("editTwitch")) {
                needSave = true;
                String a = scanner.next();
                int b = 10000000;
                try {
                    b = Integer.parseInt(a);
                } catch (Exception e) {}
                if (b < data.players.length) {
                    System.out.println("their twitch / none");
                    System.out.print("twitch = ");
                    String c = scanner.next();
                    System.out.println("Done!");
                    data.editTwitch(b, c);
                } else System.out.println("Index out of range");
            }

            if (s.equals("getTwitch")) {
                needSave = true;
                int a = scanner.nextInt();
                if (a < data.players.length) {
                    System.out.println("Trying to get twitch from ranked api");
                    String twitch = getTwitch(data.players[a].name);
                    if (twitch != null) {
                        System.out.println("Twitch: " + twitch);
                        System.out.println("accept (Y/n)");
                        String n = scanner.next();
                        if (n.toLowerCase().equals("n")) {
                            System.out.println("Not replacing twitch");
                        } else {
                            System.out.println("Replacing twitch");
                            data.players[a].twitch = twitch;
                        }
                    } else System.out.println("Twitch is null, either command failed or it's not linked");

                } else System.out.println("Index out of range");
            }

            if (s.equals("list")) {
                System.out.println("Player list:");
                for (int i = 0; i < data.players.length; i++) System.out.println(data.players[i].id + " " + data.players[i].name + " " + data.players[i].twitch + " " + data.players[i].live);
                System.out.println();
            }

            if (s.equals("live")) {
                System.out.println("Live player list:");
                for (int i = 0; i < data.players.length; i++) if (data.players[i].live) System.out.println(i + " " + data.players[i].name + " " + data.players[i].twitch + " " + data.players[i].live);
                System.out.println();
            }

            if (s.equals("hide")) {
                int playerId = scanner.nextInt();

                data.updateSplits();

                for (int i = 0; i < data.players.length; i++) if (data.players[i].id == playerId) {
                    Player player = data.players[i];
                    JSONObject split = data.currentSplit(player.name);
                    int splitNumber = split.getInt("split");

                    if (splitNumber >= 0) {
                        System.out.println("Hiding player " + player.name + " for split " + split.getInt("split"));
                        data.players[i].hideSplit = splitNumber;
                    } else {
                        System.out.println("Player " + player.name + " is not playing");
                    }                    
                }

            }

            if (s.equals("unhide")) {
                System.out.println("Unhiding all hidden players");
                unHide(data);
            }

            if (s.equals("snapshot")) {
                String arg = scanner.next();

                switch (arg) {
                    case "start":
                        snapshot.start();
                        break;
                    case "stop":
                        snapshot.stop();
                        break;
                    case "status":
                        snapshot.status();
                        break;
                    default:
                        System.out.println("Valid arguments: start, stop, status");
                        break;
                }
            }

            if (s.equals("timer")) {
                String arg = scanner.next();

                switch (arg) {
                    case "game", "up":
                        int limit = getTimeLimit(data.leagueNumber) * 60;
                        timer.start(limit, false, "Timer2", "none");
                        break;
                    case "im", "down":
                        int arg2 = scanner.nextInt();
                        timer.start(arg2, true, "Timer", "Starting soon");
                        break;
                    case "advance":
                        int arg3 = scanner.nextInt();
                        timer.add(arg3);
                        break;
                    case "stop":
                        timer.stop();
                        break;
                    default:
                        System.out.println("Valid arguments: game/up, im/down <time>, advance <time>, stop");
                        break;
                }
            }
        }

        scanner.close();
        snapshot.stop();
        timer.stop();
        Streamlink.killAll();

        if (client.enable) client.close();
    }

    static void downloadOrder(int leagueNumber, int weekNumber) throws MalformedURLException, IOException, URISyntaxException {
        File file = new File("seed_order.json");
        
        BufferedInputStream in = new BufferedInputStream(new URI("https://pastel-shrimp-251.convex.site/api/seeds/order?leagueNumber=" + leagueNumber).toURL().openStream());
        FileOutputStream out = new FileOutputStream(file);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONArray apiData = readJSONArray(file);
        JSONArray data = new JSONArray();

        for (int i = 0; i < apiData.length(); i++) {
            JSONObject api = apiData.getJSONObject(i);
            JSONObject seed = new JSONObject();
            seed.put("number", api.getInt("order"));
            seed.put("type", api.getString("type").toLowerCase());

            data.put(seed);

            System.out.println(seed.getInt("number") + " " + seed.getString("type").replace("_", " "));
        }

        JSONObject seedList = new JSONObject();

        int seedcount = getSeedcount(leagueNumber);
        int timelimit = getTimeLimit(leagueNumber);

        seedList.put("league", leagueNumber);
        seedList.put("week", weekNumber);
        seedList.put("seedcount", seedcount);
        seedList.put("timelimit", timelimit);
        seedList.put("data", data);

        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        w.write(seedList.toString());
        w.close();
    }

    static void unHide(Data data) {
        for (int i = 0; i < data.players.length; i++) {
            data.players[i].hideSplit = -1;
        }
    }

    static void setMute(Client client, JSONObject audio, boolean muted) {
        String mic = audio.getString("mic");
        String disc = audio.getString("disc");

        boolean micEnabled = audio.getBoolean("micEnabled");
        boolean discEnabled = audio.getBoolean("discEnabled");

        if (micEnabled) client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputMute\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + mic + "\", \"inputMuted\": " + muted + "}}}");
        if (discEnabled) client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputMute\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + disc + "\", \"inputMuted\": " + muted + "}}}");

        System.out.println("Muted = " + muted);
    }

    static int getTimeLimit(int leagueNumber) {
        if (leagueNumber == 1) return 13;
        if (leagueNumber == 2) return 15;
        if (leagueNumber == 3) return 17;
        if (leagueNumber == 4) return 20;
        if (leagueNumber == 5) return 25;
        if (leagueNumber == 6) return 30;
        return 60;
    }

    static int getSeedcount(int leagueNumber) {
        if (leagueNumber == 1) return 8;
        if (leagueNumber == 2) return 8;
        if (leagueNumber == 3) return 8;
        if (leagueNumber == 4) return 6;
        if (leagueNumber == 5) return 5;
        if (leagueNumber == 6) return 5;
        return 2;
    }

    public static String fixLink(String twitch) {
        String result = twitch;
        while(result.contains("/")) {
            result = result.substring(1);
        }
        return result;
    }

    public static JSONObject readJSON(File file) {
        JSONObject result = null;

        for (int i = 0; i < 10; i++) {
            try {
                result = readJSONunsafe(file);
            } catch (JSONException e) {
            result = null;
            } catch (IOException e) {
                result = null;
            }

            if (result != null) return result;
        }

        return null;
    }

    public static JSONArray readJSONArray(File file) {
        JSONArray result = null;

        for (int i = 0; i < 10; i++) {
            try {
                result = readJSONArrayunsafe(file);
            } catch (JSONException e) {
                result = null;
            } catch (IOException e) {
                result = null;
            }

            if (result != null) return result;
        }

        return null;
    }

    public static JSONObject readJSONunsafe(File file) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
	        stringBuilder.append(line);
	        stringBuilder.append(ls);
        }

        if (stringBuilder.length() == 0) {
            reader.close();
            return null;
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        reader.close();

        String content = stringBuilder.toString();

        JSONObject o = new JSONObject(content);

        return o;
    }

    public static JSONArray readJSONArrayunsafe(File file) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
	        stringBuilder.append(line);
	        stringBuilder.append(ls);
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        reader.close();

        String content = stringBuilder.toString();

        JSONArray o = new JSONArray(content);

        return o;
    }

    static void saveData(Data data, JSONObject seedList, int currentSeed, String hostName) throws IOException {

        Player[] players = data.players;
        int leagueNumber = data.leagueNumber;
        int weekNumber = data.weekNumber;

        File file = new File("runtime_data.json");
        JSONObject object = new JSONObject();
        JSONArray playerList = new JSONArray();

        for (int i = 0; i < players.length; i++) {
            JSONObject pl = new JSONObject();
            pl.put("id", players[i].id);
            pl.put("name", players[i].name);
            pl.put("twitch", players[i].twitch);
            pl.put("live", players[i].live);
            playerList.put(pl);
        }

        object.put("league", leagueNumber);
        object.put("week", weekNumber);
        object.put("players", playerList);
        object.put("seedList", seedList);
        object.put("currentSeed", currentSeed);
        object.put("host", hostName);

        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        w.write(object.toString());
        w.close();
    }

    static JSONObject getData() {
        File file = new File("runtime_data.json");
        JSONObject object = readJSON(file);
        return object;
    }

    static String getTwitch(String username) {
        String result;

        try {
            result = getTwitchFromRanked(username);
        } catch (MalformedURLException e){
            result = null;
        } catch (IOException e) {
            result = null;
        } catch (URISyntaxException e) {
            result = null;
        }

        return result;
    }

    static String getTwitchFromRanked(String username) throws MalformedURLException, IOException, URISyntaxException {
        File file = new File("player_request.json");

        BufferedInputStream in = new BufferedInputStream(new URI("https://api.mcsrranked.com/users/" + username).toURL().openStream());
        FileOutputStream out = new FileOutputStream(file);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        out.close();

        JSONObject user = readJSON(file);
        if (user == null) return null;
        String status = user.getString("status");
        if (status != null && status.equals("success")) {
            JSONObject data = user.getJSONObject("data");
            JSONObject connections;
            try {
                connections = data.getJSONObject("connections");
            } catch (JSONException e) {
                connections = null;
            }
            if (connections != null) {
                JSONObject twitch;
                try {
                    twitch = connections.getJSONObject("twitch");
                } catch (JSONException e) {
                    twitch = null;
                }
                if (twitch != null) {
                    String id;
                    try {
                        id = twitch.getString("id");
                    } catch (JSONException e) {
                        id = null;
                    }
                    if (id != null) return id;
                }
            }
        }

        return null;
    }

}

class Player {

    String name;
    String twitch;
    boolean live;
    String uuid;
    int id;

    int lb_points = 0;
    boolean lb_played = false;
    int lb_comps = 0;
    int lb_time = 0;

    int hideSplit = -1; // not hide = -1, hide = {0, 1, 2, 3, 4, 5, 6} for the specific split, once they get the next one they're shown again
    
    boolean playing = false;

    public Player(String name, String twitch, int id) {
        this.name = name;
        this.twitch = twitch;
        this.id = id;
        if (twitch.equals("none")) twitch = "";
        if (twitch.equals("")) live = false; else live = true;

        try {
            GetImg.getImg(name);
        } catch (Exception e) {}
    }
}

class Split {
    String player;
    int time;

    public Split(String player, int time) {
        this.player = player;
        this.time = time;
    }
}
