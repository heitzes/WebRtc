package com.example.signalling2.handler;
import com.google.gson.JsonObject;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;


public class iceEventHandler implements EventListener<IceCandidateFoundEvent> {
    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);
    WebSocketSession session;
    iceEventHandler(WebSocketSession s) {
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