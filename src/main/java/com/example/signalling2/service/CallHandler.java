/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example.signalling2.service;

import com.example.signalling2.domain.RoomSession;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.MemoryRoomRepository;
import com.example.signalling2.repository.RoomRepository;
import com.example.signalling2.repository.UserRedisRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protocol handler for 1 to N video call communication.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@Component
//@RequiredArgsConstructor
public class CallHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(CallHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, String> viewers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UserSession> presenters = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, ConcurrentHashMap<String, UserSession>> rooms = new ConcurrentHashMap<>();


  private final KurentoClient kurento;
  // private final UserRedisRepository userRedisRepository;
  // private final UserService userService;
  private final RoomRepository roomRepository;
  @Autowired
  public CallHandler(KurentoClient kurento, RoomRepository roomRepository) {
    this.kurento = kurento;
    this.roomRepository = roomRepository;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    System.out.println("session ID: " + session.getId());
    session.sendMessage(new TextMessage(session.getId()));
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    //System.out.println("session: " + session);
    //System.out.println("exception message: " + exception.getMessage());
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // System.out.println(" ----- Text Message ----- ");
    //System.out.println(message.toString());

    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
    log.debug("Incoming message from session '{}': {}", session.getId(), jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
      case "presenter":
        try {
          presenter(session, jsonMessage);
        } catch (Throwable t) {
          handleErrorResponse(t, session, "presenterResponse");
        }
        break;
      case "viewer":
        try {
          viewer(session, jsonMessage);
        } catch (Throwable t) {
          handleErrorResponse(t, session, "viewerResponse");
        }
        break;
      case "onIceCandidate": {
        JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

        UserSession user = null;
        if (!presenters.isEmpty()) {
          if (presenters.get(session.getId()) != null && presenters.get(session.getId()).getSession() == session) { // webSocketSession과 session이 같은지 비교
            user = presenters.get(session.getId());
          } else {
            String roomId = viewers.get(session.getId());
            user = rooms.get(roomId).get(session.getId());
          }
        }
        if (user != null) {
          IceCandidate cand =
              new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid")
                  .getAsString(), candidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(cand);
        }
        break;
      }
      case "stop":
        stop(session);
        break;
      default:
        break;
    }
  }

  private void handleErrorResponse(Throwable throwable, WebSocketSession session, String responseId)
      throws IOException {
    stop(session);
    log.error(throwable.getMessage(), throwable);
    JsonObject response = new JsonObject();
    response.addProperty("id", responseId);
    response.addProperty("response", "rejected");
    response.addProperty("message", throwable.getMessage());
    session.sendMessage(new TextMessage(response.toString()));
  }

  private synchronized void presenter(final WebSocketSession session, JsonObject jsonMessage)
      throws IOException {
    if (presenters.get(session.getId()) == null) {

      // presenter setting

      // 1. Media logic (webRtcEndpoint in loopback)
      MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint presenterWebRtc = new WebRtcEndpoint.Builder(pipeline).build();

      // 2. Store user session
      UserSession presenter = new UserSession(session);
      presenter.setWebRtcEndpoint(presenterWebRtc);
      presenter.setMediaPipeline(pipeline);

      // 3. Save presenter
      presenters.put(session.getId(), presenter);
      viewers.put(session.getId(), session.getId());
      System.out.println("presenters keys: " + presenters.keys());
      System.out.println("presenters values: " + presenters.values());

      // 4. Create room
      rooms.put(session.getId(), new ConcurrentHashMap<String, UserSession>());

      // Save in UserRepository
      // userService.save(presenter);
      RoomSession roomSession = new RoomSession(session.getId());
      roomRepository.save(roomSession);


      presenterWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

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
      });

      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
      String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);
      JsonObject response = new JsonObject();
      response.addProperty("id", "presenterResponse");
      response.addProperty("response", "accepted");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
        //userRedisRepository.findById(session.getId()).get().sendMessage(response);
        presenters.get(session.getId()).sendMessage(response);
      }
      presenterWebRtc.gatherCandidates();

    } else {
      JsonObject response = new JsonObject();
      response.addProperty("id", "presenterResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "Another user is currently acting as sender. Try again later ...");
      session.sendMessage(new TextMessage(response.toString()));
    }
  }

  private synchronized void viewer(final WebSocketSession session, JsonObject jsonMessage)
      throws IOException {

    if (presenters.isEmpty()) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "viewerResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "No active sender now. Become sender or . Try again later ...");
      session.sendMessage(new TextMessage(response.toString()));
    } else {

      UserSession presenterSession = null;
      WebRtcEndpoint nextWebRtc = null;

      String roomId = String.valueOf(jsonMessage.get("roomId"));
      if (!presenters.containsKey(roomId)) { // 일단 키가 없으면 (방이 없으면) 맨 첫번째 방에 넣어줌
        System.out.println("Can't find requested room. Connected to first room...");
        HashMap.Entry<String,UserSession> entry = presenters.entrySet().iterator().next();
        presenterSession = entry.getValue();
      } else { // show requested streamer
        System.out.println("Found room!");
        presenterSession = presenters.get(roomId);
      }
      nextWebRtc = new WebRtcEndpoint.Builder(presenterSession.getMediaPipeline()).build();

      // find specific room in rooms
      ConcurrentHashMap<String, UserSession> room = rooms.get(roomId);
      if (room.containsKey(session.getId())) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message", "You are already viewing in this session. "
            + "Use a different browser to add additional viewers.");
        session.sendMessage(new TextMessage(response.toString()));
        return;
      }

      UserSession viewer = new UserSession(session);
      room.put(session.getId(), viewer);
      viewers.put(session.getId(), roomId);


      System.out.println("viewWebRtcEndpoint: " + nextWebRtc);

      //// viewer
      System.out.println("view pipeline: " + presenterSession.getMediaPipeline());
      viewer.setWebRtcEndpoint(nextWebRtc);
      // Connect !!!!!!
      presenterSession.getWebRtcEndpoint().connect(nextWebRtc);
      //userRedisRepository.findById(session.getId()).get().getWebRtcEndpoint().connect(nextWebRtc);

      nextWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

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
      });


      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
      String sdpAnswer = nextWebRtc.processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "viewerResponse");
      response.addProperty("response", "accepted");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
        viewer.sendMessage(response);
      }
      nextWebRtc.gatherCandidates();
    }
  }

  private synchronized void stop(WebSocketSession session) throws IOException {
    String sessionId = session.getId(); // user who requested stop
    String roomId = viewers.get(sessionId);
    ConcurrentHashMap<String, UserSession> room = rooms.get(roomId);

//    String roomId = String.valueOf(jsonMessage.get("roomId"));
//    ConcurrentHashMap<String, UserSession> room = rooms.get(roomId);

//    HashMap.Entry<String,UserSession> entry = presenters.entrySet().iterator().next();
//    String presenterId = entry.getKey(); // session-id
//    UserSession presenterSession =entry.getValue();
//    MediaPipeline thisPipeline = presenterSession.getMediaPipeline();

//    if (presenterUserSession != null && presenterUserSession.getSession().getId().equals(sessionId)) {
    if (presenters.get(sessionId) != null && presenters.containsKey(sessionId)) {
      for (UserSession viewer : room.values()) { // 이 코드 수정 안하면 방 하나 터질때 모든 뷰어 방 다터짐... // 수정완료
        JsonObject response = new JsonObject();
        response.addProperty("id", "stopCommunication");
        viewer.sendMessage(response);
      }

      MediaPipeline thisPipeline = presenters.get(sessionId).getMediaPipeline();
      log.info("Releasing media pipeline");
      if (thisPipeline != null) {
        thisPipeline.release();
      }
      thisPipeline = null;
      presenters.remove(sessionId); // presenters에서 session 삭제
      roomRepository.delete(sessionId);
      rooms.remove(sessionId);

      System.out.println("=======presenter session closed========");
    } else if (room.containsKey(sessionId)) { // viewer가 stop 했음
      if (room.get(sessionId).getWebRtcEndpoint() != null) {
        room.get(sessionId).getWebRtcEndpoint().release();
      }
      room.remove(sessionId);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
  }

}
