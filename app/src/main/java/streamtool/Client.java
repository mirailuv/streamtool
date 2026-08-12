package streamtool;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

class Client extends WebSocketClient {

    boolean enable = true;
    Player[] povs = new Player[4];
    Log log;

    public Client(URI uri, boolean enable) {
        super(uri);
        log = new Log(new File("websocket_log.txt"), true);
        for (int i = 0; i < povs.length; i++) povs[i] = null;
        this.enable = enable;
        if (!enable) this.close();
    }

    int showPlayer(Player player, int place, String imagePath) throws IOException {
        return showPlayer(player, place, imagePath, true);
    }

    int showPlayer(Player player, int place, String imagePath, boolean allowReplace) throws IOException {
        if (!enable) return 5;
        if (place < 1 | place > 4) return 2;
        if (!player.live) return 3;

        String nameSlot = "pov" + place + "name";
        String name = player.name;
        String image = imagePath + name + ".png";
        String twitch = player.twitch;
        String linkSlot = "pov" + place;
        String link = "https://player.twitch.tv/?channel=" + twitch + "&enableExtensions=true&muted=true&parent=twitch.tv&player=popout&quality=chunked";
        
        if (allowReplace) {
            int check = 0;
            for (int i = 0; i < 4; i++) if (povs[i] != null) {
                if (player.name.equals(povs[i].name)) {
                    check = i + 1;
                }
            }

            if (check > 0) {
                Player swapPlayer = povs[place - 1];
                if (swapPlayer != null) showPlayer(swapPlayer, check, imagePath, false);
            }
        }

        povs[place - 1] = player;


        this.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"head" + place + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + image + "\"}}}}");

        JSONObject a = new JSONObject("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"pov1name\", \"overlay\": true, \"inputSettings\": {\"text\":\"NAME\"}}}}");
        JSONObject b = (JSONObject) a.get("d");
        JSONObject c = (JSONObject) b.get("requestData");

        JSONObject d = (JSONObject) c.get("inputSettings");
        d.put("text", name);

        c.put("inputName", nameSlot);
        c.put("inputSettings", d);

        b.put("requestData", c);
        a.put("d", b);

        this.send(a.toString());

        JSONObject e = new JSONObject("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"pov1\", \"overlay\": true, \"inputSettings\": {\"url\":\"https://player.twitch.tv/?channel=NAME&enableExtensions=true&muted=true&parent=twitch.tv&player=popout&quality=chunked\"}}}}");
        JSONObject f = (JSONObject) e.get("d");
        JSONObject g = (JSONObject) f.get("requestData");
        JSONObject h = (JSONObject) g.get("inputSettings");

        h.put("url", link);

        g.put("inputName", linkSlot);
        g.put("inputSettings", h);

        f.put("requestData", g);
        e.put("d", f);

        this.send(e.toString());


        return 1;
    }


    public Client(URI uri) {
        super(uri);
    }

    @Override
    public void onOpen(ServerHandshake data) {
        System.out.println("Connected");
    }

    @Override
    public void onMessage(String message) {
        log.write(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(code + " " + reason + " " + remote);
    }

    @Override
    public void onError(Exception e) {

    }
}