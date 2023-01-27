package com.example.signalling2.handler;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;

@Slf4j
public class IceEventHandler implements EventListener<IceCandidateFoundEvent> {
    WebSocketSession session;
    IceEventHandler(WebSocketSession s) {
        this.session = s;
    }
    @Override
    public void onEvent(IceCandidateFoundEvent event) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "iceCandidate");
      response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
      try {
        synchronized (session) {
          session.sendMessage(new TextMessage(response.toString()));
        }
      } catch (IOException e) {
        log.debug(e.getMessage());
      }
    }
}