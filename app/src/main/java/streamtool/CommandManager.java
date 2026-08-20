package streamtool;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

public class CommandManager {

    Data data;
    RuntimeData run;

    public CommandManager(Data data, RuntimeData run) {
        this.data = data;
        this.run = run;
    }

    public int execute(JSONObject commandObject) {
        int invalid = commandObject.getInt("invalid");

        if (invalid != 0) return 0;

        String command = commandObject.getString("command");
        switch (command) {
            case "quit": return quitCommand();
            case "restore": return restoreCommand();
            case "reload": return reloadCommand();
            case "override": return overrideCommand(commandObject);
            case "mute": return muteCommand(commandObject);
            case "import": return importCommand();
            case "order": return orderCommand(commandObject);
            case "scene": return sceneCommand(commandObject);
            case "next": return nextCommand();
            case "setseed": return setseedCommand(commandObject);
            case "interview": return interviewCommand(commandObject);
            case "show": return showCommand(commandObject);
            case "match": return matchCommand(commandObject);
            case "leaderboard": return leaderboardCommand(commandObject);
            case "killfeeds": return killfeedsCommand();
            case "completions": return completionsCommand(commandObject);
            case "splits": return splitsCommand();
            case "update": return updateCommand(commandObject);
            case "timer": return timerCommand(commandObject);
            case "setlive": return setliveCommand(commandObject);
            case "addplayer": return addplayerCommand(commandObject);
            case "twitch": return twitchCommand(commandObject);
            case "list": return listCommand();
            case "live": return liveCommand();
            case "hide": return hideCommand(commandObject);
            case "unhide": return unhideCommand();
            case "snapshot": return snapshotCommand(commandObject);
            
        }


        return 0;
    }

    int quitCommand() {
        run.stop = true;
        return 1;
    }

    int restoreCommand() {
        System.out.println("Loading previous values");

        JSONObject object = Main.getData();
        run.seedList = object.getJSONObject("seedList");
        data.weekNumber = object.getInt("week");
        data.leagueNumber = object.getInt("league");
        run.hostName = object.getString("host");
        data.currentSeed = object.getInt("currentSeed");

        JSONArray idList = object.getJSONArray("matchIds");
        int[] matchIds = new int[idList.length()];
        for (int i = 0; i < matchIds.length; i++) matchIds[i] = idList.getInt(i);
        run.matchIds = matchIds;

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
        return 1;
    }

    int reloadCommand() {
        System.out.println("Reloading obs layout");

        run.obsLayout = Main.readJSON(new File("obs_layout.json"));

        JSONArray bH = run.obsLayout.getJSONArray("boardHeight");
        int[] bh2 = new int[bH.length()];
        for (int i = 0; i < bh2.length; i++) bh2[i] = bH.getInt(i);
        run.boardHeight = bh2;

        JSONArray pzH = run.obsLayout.getJSONArray("promoteHeight");
        int[] pzh2 = new int[pzH.length()];
        for (int i = 0; i < pzh2.length; i++) pzh2[i] = pzH.getInt(i);
        run.promoteZoneHeight = pzh2;

        JSONArray dZ = run.obsLayout.getJSONArray("demoteZonePos");
        int[] dz2 = new int[dZ.length()];
        for (int i = 0; i < dz2.length; i++) dz2[i] = dZ.getInt(i);
        run.demoteZonePos = dz2;

        run.scenes = (JSONObject) run.obsLayout.get("scenes");
        run.audio = (JSONObject) run.obsLayout.get("audio");
        run.paths = (JSONObject) run.obsLayout.get("paths");

        run.imagePath = (String) run.paths.get("heads");
        run.seediconPath = (String) run.paths.get("seed_icons");
        run.seedimagePath = (String) run.paths.get("seed_images");
    
        return 1;
    }

    // "promotions", "demotions", "lb1", "lb2", "points", "reset"
    int overrideCommand(JSONObject commandObject) {

        String var = commandObject.getString("var");

        if (var.equals("reset")) {
            System.out.println("Reset overrides");
            for (int i = 0; i < run.overrides.length; i++) {
                run.overrides[i] = -1;
            }
            return 1;
        }

        int value = commandObject.getInt("value");

        switch (var) {
            case "promotions":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[0] = value;
                return 1;
            case "demotions":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[1] = value;
                return 1;
            case "lb1":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[2] = value;
                return 1;
            case "lb2":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[3] = value;
                return 1;
            case "points":
                System.out.println("Set override: " + var + " = " + value);
                run.overrides[4] = value;
                return 1;
            default:
                return 0;
        }
    }

    int muteCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        if (value == 0) {
            if (run.muted) run.muted = false; else run.muted = true;
        }
        
        if (value == 1) run.muted = true;
        if (value == 2) run.muted = false;

        Main.setMute(run.client, run.audio, run.muted);
        return 1;
    }

    int importCommand() {
        run.needSave = true;
        System.out.println("Select file");
        File file = run.fs.select();

        if (file == null) return 0;

        String fileName = file.getName();
        String lnum = fileName.substring(5, 6);
        String wnum = fileName.substring(7, 9);

        int leagueNumber;
        int weekNumber;

        try {
            leagueNumber = Integer.parseInt(lnum);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException with leagueNumber");
            leagueNumber = 0;
        }
        try {
            weekNumber = Integer.parseInt(wnum);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException with weekNumber");
            weekNumber = 0;
        }

        data.leagueNumber = leagueNumber;
        data.weekNumber = weekNumber;

        System.out.println("League " + leagueNumber + " Week " + weekNumber);

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"League\", \"overlay\": true, \"inputSettings\": {\"text\":\"League " + leagueNumber + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Week\", \"overlay\": true, \"inputSettings\": {\"text\":\"Week " + weekNumber + "\"}}}}");

        if (file.exists()) {
            data.clearPlayers();
            JSONArray array = Main.readJSONArray(file);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String name = o.getString("ign");
                String twitch = Main.fixLink(o.getString("twitch_username"));
                int id = data.players.length;
                System.out.println(id + " " + name);
                Player player = new Player(name, twitch, id);
                data.addPlayer(player);
            }
            System.out.println("Import complete");
            
        } else {
            System.out.println("Invalid file");
            return 0;
        }

        int playerCount = data.players.length;
        int timeLimit = Main.getTimeLimit(leagueNumber);

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"numPlayers\", \"overlay\": true, \"inputSettings\": {\"text\":\"Players: " + playerCount + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"time text\", \"overlay\": true, \"inputSettings\": {\"text\":\"Time limit: " + timeLimit + " minutes\"}}}}");

        return 1;
    }

    int orderCommand(JSONObject commandObject) {

        int value = commandObject.getInt("value");
        run.needSave = true;

        // print seed order
        if (value == 0 || value == 4) {
            if (run.seedList.isEmpty()) {
                System.out.println("No seed order has been imported");
                return 0;
            }

            JSONArray array = run.seedList.getJSONArray("data");

            System.out.println("Seed order:");

            // update the seed order
            for (int i = 0; i < array.length(); i++) {
                int seed_number = i + 1;
                JSONObject this_seed = (JSONObject) array.get(i);
                String seed_type = (String) this_seed.get("type");
                String seedTypeText = seed_type.replace("_", " ");

                System.out.println("Seed " + seed_number + " = " + seedTypeText);
            }


            if (value == 0) return 1;
        }

        // switch to scene
        if (value == 4) {
            run.commandManager.execute(run.commandParser.getCommand("scene order"));
            return 1;
        }

        // skip argument
        if (value == 2) {
            if (run.seedList.isEmpty()) {
                System.out.println("No seed order has been imported");
                return 0;
            }

            int var = commandObject.getInt("var");

            JSONArray array = run.seedList.getJSONArray("data");

            if (var > array.length()) {
                System.out.println("Seed index is out of range");
                return 0;
            }

            // remove the seed and put it at the end
            JSONObject seed = array.getJSONObject(var - 1);
            array.remove(var - 1);
            array.put(seed);
            System.out.println("Skipping seed " + var + " to be played at the end");
            System.out.println("New seed order:");

            // update the seed order
            for (int i = 0; i < array.length(); i++) {
                int seed_number = i + 1;
                JSONObject this_seed = (JSONObject) array.get(i);
                String seed_type = (String) this_seed.get("type");
                String seedTypeText = seed_type.replace("_", " ");

                System.out.println("Seed " + seed_number + " = " + seedTypeText);

                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"text " + seed_number + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"icon " + seed_number + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.seediconPath + seed_type + ".png" + "\"}}}}");
            }

            run.seedList.put("data", array);
            return 1;
        }

        File file = null;

        if (value == 1) {
            System.out.println("Select file");
            file = run.fs.select();
        } else {
            try {
                Main.downloadOrder(data.leagueNumber, data.weekNumber);
                file = new File("seed_order.json");
            } catch (IOException | URISyntaxException e) {
                file = null;
            }
        }
        
        if (file == null) {
            System.out.println("Unable to read file");
            return 0;
        }

        run.seedList = Main.readJSON(file);
        JSONArray seed_data = (JSONArray) run.seedList.get("data");

        System.out.println("Seed order:");

        for (int i = 0; i < seed_data.length(); i++) {
            int seed_number = i + 1;
            JSONObject this_seed = (JSONObject) seed_data.get(i);
            String seed_type = (String) this_seed.get("type");
            String seedTypeText = seed_type.replace("_", " ");

            System.out.println("Seed " + seed_number + " = " + seedTypeText);

            run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"text " + seed_number + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
            run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"icon " + seed_number + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.seediconPath + seed_type + ".png" + "\"}}}}");
        }

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + data.currentSeed + "\"}}}}");

        return 1;
    }

    int sceneCommand(JSONObject commandObject) {
        String scene = commandObject.getString("value");
        switch(scene) {
            case "intermission":
                System.out.println("Intermission");
                run.commandManager.execute(run.commandParser.getCommand("mute true"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("intermissionScene") + "\"}}}");
                return 1;

            case "nextseed":
                System.out.println("Next seed");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + data.currentSeed + "\"}}}}");

                // load seed information
                int seed_number = data.currentSeed;

                if (seed_number == 0) seed_number = 1;

                JSONArray seed_data = (JSONArray) run.seedList.get("data");
                JSONObject this_seed = (JSONObject) seed_data.get(seed_number - 1);
                String seed_type = (String) this_seed.get("type");
                String seedTypeText = seed_type.replace("_", " ");
                        
                // set seed information
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed number\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seed_number + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + data.currentSeed + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype text\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + seedTypeText + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seedtype icon\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.seediconPath + seed_type + ".png" + "\"}}}}");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"seed_background\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + run.seedimagePath + seed_type + ".png" + "\"}}}}");

                // switch to next seed scene
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("nextseedScene") + "\"}}}");
                return 1;
            
            case "order":
                System.out.println("Seed list");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));

                int seed_count = (int) run.seedList.get("seedcount");
                JSONObject seedlistScenes = (JSONObject) run.scenes.get("seedlistScenes");
                String seedlistScene = (String) seedlistScenes.get("seeds" + seed_count);
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + seedlistScene + "\"}}}");
                return 1;

            case "spectator":
                System.out.println("Spectator");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("spectatorScene") + "\"}}}");
                return 1;

            case "game":
                System.out.println("Players");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                int pov_count = commandObject.getInt("var");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) run.scenes.get("gameplayScenes")).get("gameplay" + pov_count) + "\"}}}");
                return 1;

            case "update":
                int pov_count2 = commandObject.getInt("var");
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + ((JSONObject) run.scenes.get("gameplayScenes")).get("gameplay" + pov_count2) + "\"}}}");
                return 1;

            case "leaderboard":
                System.out.println("Leaderboard");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("leaderboardScene") + "\"}}}");
                return 1;

            case "commentators": 
                System.out.println("Commentators");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("commentatorScene") + "\"}}}");
                return 1;

            case "completions":
                System.out.println("Completions");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("completionScene") + "\"}}}");
                return 1;

            case "interview":
                System.out.println("Interview");
                run.commandManager.execute(run.commandParser.getCommand("mute false"));
                run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetCurrentProgramScene\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("interviewScene") + "\"}}}");
                return 1;

            default:
                System.out.println("Unknown scene");
                return 0;
        }
    }

    int nextCommand() {
        run.needSave = true;

        Main.unHide(data);

        if (data.currentSeed < Main.getSeedcount(data.leagueNumber)) data.currentSeed++; else {
            System.out.println("All seeds have been played");
            return 0;
        }

        // update the seed number
        run.commandManager.execute(run.commandParser.getCommand("setseed " + data.currentSeed));

        // switch to next seed scene
        run.commandManager.execute(run.commandParser.getCommand("scene nextseed"));

        return 1;

    }

    int setseedCommand(JSONObject commandObject) {
        run.needSave = true;
        data.currentSeed = commandObject.getInt("value");
        System.out.println("Set seed to " + data.currentSeed);
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"Seed\", \"overlay\": true, \"inputSettings\": {\"text\":\"Seed " + data.currentSeed + "\"}}}}");
        return 1;
    }

    int interviewCommand(JSONObject commandObject) {
        int playerId = commandObject.getInt("id");
        Player player = data.getPlayerById(playerId);

        if (player == null) {
            System.out.println("Unable to find player");
            return 0;
        }

        System.out.println("Interview: " + player.name);

        // try to download player skin
        try {
            GetImg.getInterviewImg(player.name);
        } catch (IOException | URISyntaxException e) {
            System.out.println("Failed to download player skin");
        }

        // update player name
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"interview player\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + player.name + "\"}}}}");

        run.commandManager.execute(run.commandParser.getCommand("scene interview"));

        return 1;
    }

    int showCommand(JSONObject commandObject) {

        if (!run.client.enable) return 0;

        int id = commandObject.getInt("id");
        int pos = commandObject.getInt("pos");

        Player player = data.getPlayerById(id);

        if (player == null) {
            System.out.println("Unable to find player");
            return 0;
        }
        
        if (player.live) {
            run.client.showPlayer(player, pos, run.imagePath, run.portRange);
            return 1;
        } else {
            System.out.println("Player is not live");
            return 0;
        }        
    }

    int matchCommand(JSONObject commandObject) {
        String value = commandObject.getString("value");
        run.needSave = true;

        switch (value) {
            case "clear":
                System.out.println("Clearing match data");
                Leaderboard.clearSeeds();
                run.clearMatchIds(Main.getSeedcount(data.leagueNumber));
                return 1;
            case "host":
                run.hostName = commandObject.getString("name");
                System.out.println("Set host to " + run.hostName);
                return 1;
            case "autodl":
                if (run.hostName.equals("")) {
                    System.out.println("Host not set");
                    return 0;
                }

                int seedNumber = data.currentSeed;
                int[] ids;
                try {
                    ids = Leaderboard.getMatchIds(run.hostName, 1);
                } catch (IOException | URISyntaxException e) {
                    System.out.println("Unable to get match id");
                    return 0;
                }
                int matchId = ids[0];
                System.out.println("Downloading seed " + seedNumber + " from id " + matchId);
                boolean result = Leaderboard.downloadSeed(seedNumber, matchId, run);
                if (result) return 1; else return 0;
            case "dl":
                if (run.hostName.equals("")) {
                    System.out.println("Host not set");
                    return 0;
                }

                int seedNumber2 = commandObject.getInt("seed");
                int matchId2 = commandObject.getInt("match");
                System.out.println("Downloading seed " + seedNumber2 + " from id " + matchId2);
                boolean result2 = Leaderboard.downloadSeed(seedNumber2, matchId2, run);
                if (result2) return 1; else return 0;
            case "id":
                if (run.hostName.equals("")) {
                    System.out.println("Host not set");
                    return 0;
                }

                int count = commandObject.getInt("count");

                int[] ids2;
                try {
                    ids2 = Leaderboard.getMatchIds(run.hostName, count);
                } catch (IOException | URISyntaxException e) {
                    if (count == 1) System.out.println("Unable to get match id"); else System.out.println("Unable to get match ids");
                    return 0;
                }
                if (count == 1) System.out.println("Match id:"); else System.out.println("Match ids:");
                for (int i = ids2.length - 1; i >= 0; i--) System.out.println(ids2[i]);
        }

        return 0;
    }

    int leaderboardCommand(JSONObject commandObject) {

        int seedcount = commandObject.getInt("value");

        // check match ids

        boolean test = true;
        for (int i = 1; i <= seedcount; i++) {
            if (run.getMatchId(i) == -1) test = false;
        }

        if (!test) {
            // check again if only last id is missing
            boolean test2 = true;
            for (int i = 1; i < seedcount; i++) test2 = false;

            // run autodl if only missing last id and if seedcount is set to current seed
            if (test2 && seedcount == data.currentSeed) {
                int dl = run.commandManager.execute(run.commandParser.getCommand("match autodl"));
                if (dl != 1) {
                    System.out.println("Failed to download missing seed");
                    return 0;
                }
            } else {
                System.out.println("Missing match ids");
                return 0;
            }
        }

        boolean b = Leaderboard.genLeaderboard(seedcount, data, run);
        if (!b) {
            System.out.println("Failed to generate leaderboard");
            return 0;
        }

        // update leaderboard txt files
        JSONObject lbData;
        try {
            lbData = Leaderboard.loadLeaderboard(run.overrides);
        } catch (IOException e) {
            System.out.println("Failed to load leaderboard data");
            return 0;
        }

        // import return data
        int promCount = lbData.getInt("promotions");
        int demCount = lbData.getInt("demotions");
        int page1 = lbData.getInt("page1");
        int page2 = lbData.getInt("page2");

        // enter overrides
        if (run.overrides[0] != -1) promCount = run.overrides[0];
        if (run.overrides[1] != -1) demCount = run.overrides[1];

        // get leaderboard height data

        int page1height = 2000; int page2height = 2000;

        if (page1 > 0) page1height = run.boardHeight[page1 - 1];
        if (page2 > 0) page2height = run.boardHeight[page2 - 1];

        int promHeight = 2000;
        if (promCount > 0) promHeight = run.promoteZoneHeight[promCount - 1];

        int demPos = 750;
        int demMath = page2 - demCount + 1;
        if (demMath > 0 & demMath <= 13) demPos = run.demoteZonePos[demMath - 1];

        //System.out.println(promCount + " " + promHeight);
        //System.out.println(demCount + " " + demHeight + " " + demPos);

        JSONObject lbIds = run.scenes.getJSONObject("lbIds");

        // leaderboard transformation
        run.client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + run.scenes.get("leaderboardScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + lbIds.get("lb1") + ", \"sceneItemTransform\": {\"cropBottom\": " + page1height +"}}}}");
        run.client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + run.scenes.get("leaderboardScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + lbIds.get("lb2") + ", \"sceneItemTransform\": {\"cropBottom\": " + page2height +"}}}}");

        // promotion transformation
        int promId = lbIds.getInt("prom");
        run.client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"lb1\", \"sceneUuid\": \"\", \"sceneItemId\": " + promId + ", \"sceneItemTransform\": {\"cropBottom\": " + promHeight + "}}}}");

        // demotion transformation
        int demId = lbIds.getInt("dem");
        run.client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"lb2\", \"sceneUuid\": \"\", \"sceneItemId\": " + demId + ", \"sceneItemTransform\": {\"positionY\": " + demPos + "}}}}");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        // switch to lb scene
        run.commandManager.execute(run.commandParser.getCommand("scene leaderboard"));

        return 1;
    }

    int killfeedsCommand() {
        System.out.println("Killing all feeds");
        Streamlink.killAll();
        return 1;
    }

    int completionsCommand(JSONObject commandObject) {

        run.timer.stop();
        run.autoUpdate.stop();

        int value = commandObject.getInt("value");
        JSONObject layoutData = Comp.updateCompletions(data.players, value, run);

        if (layoutData == null) {
            System.out.println("Failed to update completions");
            return 0;
        }

        int page1 = (int) layoutData.get("page1");
        int page2 = (int) layoutData.get("page2");
        boolean showPage2 = true;
        if (page2 == 0) showPage2 = false;

        int page1height = 2000; int page2height = 2000;

        if (page1 > 0) page1height = run.boardHeight[page1 - 1];
        if (page2 > 0) page2height = run.boardHeight[page2 - 1];

        JSONObject compIds = run.scenes.getJSONObject("compIds");

        // set page heights
        run.client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + run.scenes.get("completionScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + compIds.get("comp1") + ", \"sceneItemTransform\": {\"cropBottom\": " + page1height +"}}}}");
        run.client.send("{\"op\":6, \"d\":{\"requestType\": \"SetSceneItemTransform\", \"requestId\": \"0\", \"requestData\": {\"canvasUuid\": \"\", \"sceneName\": \"" + run.scenes.get("completionScene") +"\", \"sceneUuid\": \"\", \"sceneItemId\": " + compIds.get("comp2") + ", \"sceneItemTransform\": {\"cropBottom\": " + page2height +"}}}}");

        // wait 1 second for text sources to update
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        // set page 2 visibility and switch to completion scene

        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetSceneItemEnabled\", \"requestId\": \"0\", \"requestData\": {\"sceneName\": \"" + run.scenes.get("completionScene") + "\", \"sceneItemId\": " + (run.scenes.getJSONObject("compIds")).get("comp2") + ", \"sceneItemEnabled\": " + showPage2 + "}}}");
        run.commandManager.execute(run.commandParser.getCommand("scene completions"));

        return 1;
    }

    int splitsCommand() {
        System.out.println("Splits");
        data.updateSplits();
        data.printSplits();
        return 1;
    }

    int updateCommand(JSONObject commandObject) {
        // TODO update

        int value = commandObject.getInt("value");
        String[] povs = new String[0];

        boolean msg = true;
        if (value == 4) {
            msg = false;
            value = 0;
        }

        switch (value) {
            case 0:
                if (msg) System.out.println("Updating by splits");
                povs = data.topSplits();

                if (povs.length > 0) {
                    int safety = povs.length;
                    if (safety > 4) safety = 4;

                    for (int i = 0; i < safety; i++) {
                        run.client.showPlayer(data.getPlayer(povs[i]), i + 1, run.imagePath, run.portRange, false);
                    }

                    run.commandManager.execute(run.commandParser.getCommand("scene update " + safety));
                }

                return 1;
            case 1:
                System.out.println("Activating auto-update");
                run.autoUpdate.start();
                return 1;
            case 2:
                System.out.println("Stopping auto-update");
                run.autoUpdate.stop();
                return 1;
            case 3:
                System.out.println("Showing random povs");
                povs = data.randomPovs();

                if (povs.length > 0) {
                    int safety = povs.length;
                    if (safety > 4) safety = 4;

                    for (int i = 0; i < safety; i++) {
                        run.client.showPlayer(data.getPlayer(povs[i]), i + 1, run.imagePath, run.portRange, false);
                    }

                    run.commandManager.execute(run.commandParser.getCommand("scene game " + safety));
                }

                return 1;
        }

        return 0;
    }

    int timerCommand(JSONObject commandObject) {
        String value = commandObject.getString("value");

        switch (value) {
            case "up":
                int limit = Main.getTimeLimit(data.leagueNumber) * 60;
                run.timer.start(limit, false, "Timer2", "none");
                System.out.println("Starting auto-update");
                run.autoUpdate.start();
                return 1;
            case "down":
                int arg2 = commandObject.getInt("var");
                run.timer.start(arg2, true, "Timer", "Starting soon");
                return 1;
            case "advance":
                int arg3 = commandObject.getInt("var");
                run.timer.add(arg3);
                return 1;
            case "stop":
                run.timer.stop();
                return 1;
            default:
                return 0;
            }
        }

    int setliveCommand(JSONObject commandObject) {
        run.needSave = true;
        int id = commandObject.getInt("id");
        Player player = data.getPlayerById(id);

        if (player == null) {
            System.out.println("Player not found");
            return 0;
        }


        int value = commandObject.getInt("value");
        if (value == 1) player.live = true;
        if (value == 2) player.live = false;
        if (value == 0) {
            if (player.live) player.live = false; else player.live = true;
        }

        return 1;
    }

    int addplayerCommand(JSONObject commandObject) {
        run.needSave = true;
        String name = commandObject.getString("value");

        String apiTwitch = Main.getTwitch(name);

        if (apiTwitch != null) {
            System.out.println("Got twitch from api");
            data.addPlayer(name, apiTwitch);
            return 1;
        }

        data.addPlayer(name, "none");
        return 1;
    }

    int twitchCommand(JSONObject commandObject) {
        run.needSave = true;
        int playerId = commandObject.getInt("id");
        Player player = data.getPlayerById(playerId);

        if (player == null) {
            System.out.println("Player not found");
            return 0;
        }

        String twitch;
        if (commandObject.has("value")) twitch = commandObject.getString("value"); else twitch = Main.getTwitch(player.name);

        if (twitch == null) {
            System.out.println("Unable to get player's twitch username");
            return 0;
        }

        System.out.println("Set twitch for " + player.name + " to " + twitch);
        player.twitch = twitch;
        player.live = true;

        return 1;
    }

    int listCommand() {
        System.out.println("Player list:");
        for (int i = 0; i < data.players.length; i++) System.out.println(data.players[i].id + " " + data.players[i].name + " " + data.players[i].twitch + " " + data.players[i].live);
        System.out.println();

        return 1;
    }

    int liveCommand() {
        System.out.println("Live player list:");
        for (int i = 0; i < data.players.length; i++) if (data.players[i].live) System.out.println(i + " " + data.players[i].name + " " + data.players[i].twitch + " " + data.players[i].live);
        System.out.println();

        return 1;
    }

    int hideCommand(JSONObject commandObject) {
        int playerId = commandObject.getInt("id");

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

        return 1;
    }

    int unhideCommand() {
        System.out.println("Unhiding all hidden players");
        Main.unHide(data);

        return 1;
    }

    int snapshotCommand(JSONObject commandObject) {
        int value = commandObject.getInt("value");

        switch (value) {
            case 1:
                run.snapshot.start();
                return 1;
            case 2:
                run.snapshot.stop();
                return 1;
            case 3:
                run.snapshot.load(commandObject.getInt("start"));
                return 1;
            case 0:
                run.snapshot.status();
                return 1;
            default:
                return 0;
        }
    }

    

}