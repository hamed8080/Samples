package WebSocket;

import com.google.gson.Gson;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;

@ClientEndpoint
public class WebSocketClass {
    URI url;
    WebSocketContainer container;
    Session session;
    Gson gson = new Gson();
    String deviceId;
    AsyncListener listener;

    public WebSocketClass(URI url, AsyncListener listener) {
        this.listener = listener;
        this.url = url;
        container = ContainerProvider.getWebSocketContainer();
    }

    public void connect() {
        try {
            listener.onSocketStateChanged(AsyncState.Connecting);
            session = container.connectToServer(this, url);
        } catch (DeploymentException | IOException e) {
            listener.onSocketStateChanged(AsyncState.Closed);
            throw new RuntimeException(e);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println(message);
        AsyncMessage response = gson.fromJson(message, AsyncMessage.class);
        AsyncType type = AsyncType.check(response.getType());
        if (type != null) {
            switch (type) {
                case Ping:
                    deviceId = response.getContent();
                    onPing();
                    break;
                case DeviceRegister:
                    serverRegister(response);
                    break;
                case ServerRegister:
                    if (Objects.equals(response.senderName, "chat-server")) {
                        listener.onSocketStateChanged(AsyncState.Ready);
                    }
                    break;
            }
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        listener.onSocketStateChanged(AsyncState.Connected);
        System.out.println("webSocket open");
    }

    @OnClose
    public void onClose(Session session) {
        listener.onSocketStateChanged(AsyncState.Closed);
        System.out.println("webSocket closed");
    }

    @OnMessage
    public void byt(ByteBuffer b) {
        System.out.println("handling bytebuffer");
    }

    public void onPing() {
        RegisterDevice registerDevice = new RegisterDevice(false, "PodChat", deviceId);
        String content = getWrapperVo(AsyncType.DeviceRegister, gson.toJson(registerDevice));
        send(content);
    }

    public String getWrapperVo(AsyncType asyncType, String content) {
        if (content != null) {
            WrapperModel model = new WrapperModel();
            model.setContent(content);
            model.setType(asyncType);
            return gson.toJson(model);
        } else return null;
    }

    public void send(String s) {
        System.out.println(s);
        session.getAsyncRemote().sendText(s);
    }

    public void serverRegister(AsyncMessage response) {
        String peerId = response.getContent();
        System.out.println(peerId);
        ServerRegister serverRegister = new ServerRegister("chat-server");
        String content = getWrapperVo(AsyncType.ServerRegister, gson.toJson(serverRegister));
        send(content);
    }

}
