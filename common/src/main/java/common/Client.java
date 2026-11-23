package common;

import java.net.URI;
import java.nio.file.Paths;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class Client extends WebSocketClient {

    Log log;
    Message message;

    public Client(URI uri, Message message) {
        super(uri);
        log = new Log(Paths.get("websocket_log.txt").toFile(), true);
        this.message = message;
    }

    @Override
    public void onOpen(ServerHandshake data) {
        System.out.println("Connected");
    }

    @Override
    public void onMessage(String msg) {
        message.receive(msg);
        log.write(msg);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(code + " " + reason + " " + remote);
    }

    @Override
    public void onError(Exception e) {

    }
}

