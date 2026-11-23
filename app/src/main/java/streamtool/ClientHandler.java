package streamtool;

import java.net.URI;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.JSONObject;

import common.Client;

public class ClientHandler {

    boolean connected = false;

    private Client client;
    MessageHandler message = new MessageHandler();

    RuntimeData run;

    public ClientHandler(URI uri, RuntimeData run) {
        client = new Client(uri, message);
        this.run = run;
    }

    void send(String message) {
        if (connected) try {
            client.send(message);
        } catch (WebsocketNotConnectedException e) {
            connected = false;
        }

        if (!connected) reconnect();
    }

    void setSceneItemEnabled(String scene, int id, boolean b) {
        JSONObject request = new JSONObject();
        request.put("op", 6);
        JSONObject d = new JSONObject();
        d.put("requestType", "SetSceneItemEnabled");
        d.put("requestId", "ie" + scene + id + b);
        JSONObject requestData = new JSONObject();
        requestData.put("sceneName", scene);
        requestData.put("sceneItemId", id);
        requestData.put("sceneItemEnabled", b);
        d.put("requestData", requestData);
        request.put("d", d);
        send(request.toString());
    }

    void setFeedLink(String feed, String link) {
        JSONObject request = new JSONObject();
        request.put("op", 6);
        JSONObject d = new JSONObject();
        d.put("requestType", "SetInputSettings");
        d.put("requestId", "cf" + feed.substring(4));
        JSONObject requestData = new JSONObject();
        requestData.put("inputName", feed);
        requestData.put("overlay", true);
        JSONObject inputSettings = new JSONObject();
        inputSettings.put("input", link);
        requestData.put("inputSettings", inputSettings);
        d.put("requestData", requestData);
        request.put("d", d);
        send(request.toString());
    }

    void requestFeedStatus(String feedName) {
        JSONObject request = new JSONObject();
        request.put("op", 6);
        JSONObject d = new JSONObject();
        d.put("requestType", "GetMediaInputStatus");
        d.put("requestId", feedName);
        JSONObject requestData = new JSONObject();
        requestData.put("inputName", feedName);
        d.put("requestData", requestData);
        request.put("d", d);
        send(request.toString());
    }

    void updatePlayerInfo(int p, Player player) {
        // figure out the names of things
        String nameSlot = "pov" + p + "name";
        String headSlot = "head" + p;
        String pronounSlot = "pronouns" + p;
        String image = run.imagePath + player.name + "-2d.png";

        // change head, name, pronouns
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + headSlot + "\", \"overlay\": true, \"inputSettings\": {\"file\":\"" + image + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + nameSlot + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + player.name + "\"}}}}");
        run.client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + pronounSlot + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + player.pronouns + "\"}}}}");
    }

    void reconnect() {
        connected = false;
        try {
            if (client.reconnectBlocking()) connected = true;
        } catch (InterruptedException e) {}
        if (connected) client.send("{\"op\": 1, \"d\": {\"rpcVersion\": 1, \"eventSubscriptions\": 0}}");
    }

    void connect() {
        try {
            client.connectBlocking();
        } catch (InterruptedException e) {}
        connected = true;
        client.send("{\"op\": 1, \"d\": {\"rpcVersion\": 1, \"eventSubscriptions\": 0}}");
    }

    void close() {
        connected = false;
        try {
            client.closeBlocking();
        } catch (InterruptedException e) {}
    }
}