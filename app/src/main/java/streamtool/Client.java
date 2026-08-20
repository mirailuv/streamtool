package streamtool;

import java.io.File;
import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

class Client extends WebSocketClient {

    boolean enable = true;
    Player[] povs = new Player[4];
    boolean useStreamlink;
    Streamlink[] feeds = new Streamlink[4];
    Log log;

    public Client(URI uri, boolean enable, boolean useStreamlink) {
        super(uri);
        log = new Log(new File("websocket_log.txt"), true);
        for (int i = 0; i < povs.length; i++) povs[i] = null;
        this.enable = enable;
        this.useStreamlink = useStreamlink;
        if (!enable) this.close();
    }

    int showPlayer(Player player, int place, String imagePath, int portRange) {
        return showPlayer(player, place, imagePath, portRange, true);
    }

    int showPlayer(Player player, int place, String imagePath, int portRange, boolean allowReplace) {
        if (!enable) return 5;
        if (place < 1 | place > 4) return 2;
        if (!player.live) return 3;
        if (povs[place - 1] != null) {
            if (povs[place - 1].name.equals(player.name)) return 4;
        }

        String nameSlot = "pov" + place + "name";
        String feedInput = "feed" + place;
        int feedPort = portRange + place;
        String feedLink = "http://localhost:" + feedPort;
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
                if (swapPlayer != null) showPlayer(swapPlayer, check, imagePath, portRange, false);
            }
        }

        povs[place - 1] = player;

        this.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"head" + place + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + image + "\"}}}}");

        this.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + nameSlot + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + name + "\"}}}}");

        if (useStreamlink) {
            if (feeds[place - 1] != null) feeds[place - 1].stop();
            feeds[place - 1] = new Streamlink(twitch, feedPort);
            feeds[place - 1].start();
            this.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \""+ feedInput +"\", \"overlay\": true, \"inputSettings\": {\"input\":\"" + feedLink + "\"}}}}");
        } else {
            this.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \""+ linkSlot +"\", \"overlay\": true, \"inputSettings\": {\"url\":\"" + link + "\"}}}}");
        }

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