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

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.RoomSession;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.RoomSessionRepository;
import com.example.signalling2.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
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
  private final RoomSessionRepository roomSessionRepository;
  private final UserService userService;
  private final RoomService roomService;

  @Autowired
  public CallHandler(KurentoClient kurento, RoomSessionRepository roomSessionRepository, UserRepository userRepository, UserService userService, RoomService roomService) {
    this.kurento = kurento;
    this.roomSessionRepository = roomSessionRepository;
    this.userService = userService;
    this.roomService = roomService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    System.out.println("session ID: " + session.getId());
    session.sendMessage(new TextMessage(session.getId()));
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {


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
        // refactor
//        if (!roomService.isEmpty()) {
        if (!presenters.isEmpty()) {
          if (presenters.containsKey(session.getId())) { // presenters에 이 session의 id가 있으면 presenter임
            user = presenters.get(session.getId());
          } else {
            String roomId = viewers.get(session.getId()); // viewerId - roomId 해시맵인 viewers로부터 roomId 가져옴
            user = rooms.get(roomId).get(session.getId());
          }
          // refactor (추후 onIceCandidate 웹소켓 통신시 roomId도 달라고 해야할듯..)
//          user = userService.findById(session.getId());
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
    if (!presenters.containsKey(session.getId())) { // sessionId not in presenters map

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

      //// refactor
      userService.save(presenter);
      Room room = new Room(presenter);
      roomService.save(room);
      ///


      System.out.println("presenters keys: " + presenters.keys());
      System.out.println("presenters values: " + presenters.values());

      // 4. Create room
      rooms.put(session.getId(), new ConcurrentHashMap<String, UserSession>());


      // Save in UserRepository
      RoomSession roomSession = new RoomSession(session.getId());
      roomSessionRepository.save(roomSession);


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
        presenters.get(session.getId()).sendMessage(response);
        //// refactor
        userService.findById(session.getId()).sendMessage(response);
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

    //// refactor
//    if (roomService.isEmpty()) {
    if (presenters.isEmpty()) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "viewerResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "No active sender now. Become sender or . Try again later ...");
      session.sendMessage(new TextMessage(response.toString()));
    } else {


      String roomId = jsonMessage.get("roomId").getAsString(); // 요청한 방id
      ConcurrentHashMap<String, UserSession> room = null; // 방 정보
      UserSession presenterSession = null;
      WebRtcEndpoint nextWebRtc = null;

      //// refactor
//      Room liveRoom = null;


      // refactor
//       if (!roomService.isPresent(roomId)) {
      if (!rooms.containsKey(roomId)) { // 일단 키가 없으면 (방이 없으면) 맨 첫번째 방세션 가져옴
        System.out.println("Can't find requested room. Connected to first room...");
        HashMap.Entry<String,UserSession> entry = presenters.entrySet().iterator().next();
        room = rooms.get(entry.getKey()); // 첫번째 방
        presenterSession = entry.getValue();
        roomId = entry.getKey();

        // refactor
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message",
                "Can't find room you requested. Room doesn't exist!");
        session.sendMessage(new TextMessage(response.toString()));

      } else { // show requested streamer
        System.out.println("Found room!");
        room = rooms.get(roomId); // 요청한 방
        presenterSession = presenters.get(roomId);

        // refactor
//        liveRoom = roomService.findById(roomId);
//        presenterSession = liveRoom.getOwner();
        presenterSession = roomService.findById(roomId).getOwner();
      }

      // refactor
//      if (roomService.isViewerExist(roomId, session.getId())) {
      if (room.containsKey(session.getId())) { // 이미 방에 viewer가 존재하는지 확인
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message", "You are already viewing in this session. "
            + "Use a different browser to add additional viewers.");
        session.sendMessage(new TextMessage(response.toString()));
        return;
      }

      // Create Viewer Session
      UserSession viewer = new UserSession(session);
      nextWebRtc = new WebRtcEndpoint.Builder(presenterSession.getMediaPipeline()).build();
      viewer.setWebRtcEndpoint(nextWebRtc);
      presenterSession.getWebRtcEndpoint().connect(nextWebRtc);

      // Save Viewer
      room.put(session.getId(), viewer);
      viewers.put(session.getId(), roomId);

      //// refactor
      userService.save(viewer);
      roomService.addViewer(roomId, session.getId());

      // Add view
      Optional<RoomSession> roomSession = roomSessionRepository.findById(roomId);
      roomSession.get().addView();

      System.out.println("viewWebRtcEndpoint: " + nextWebRtc);
      System.out.println("view pipeline: " + presenterSession.getMediaPipeline());

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

      // set sdpOffer
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
    String roomId = viewers.get(sessionId); // 속한 방이 어딘지 찾기
    // refactor
//    String roomId = userService.findRoomId(sessionId);

    ConcurrentHashMap<String, UserSession> room = rooms.get(roomId);
    //// refactor
//    if (roomService.isPresent(roomId)){
    if (room != null){ // 방이 있다면
//      if (roomId.equals(sessionId)) {
      if (presenters.containsKey(sessionId)) { // presenter라면

          // refactor
//        ArrayList<String> viewers = roomService.findViewers(roomId);
//        for (String viewerId : viewers) {
//          JsonObject response = new JsonObject();
//          response.addProperty("id", "stopCommunication");
//          userService.findById(viewerId).sendMessage(response);
//        }

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

        // refactor
//        MediaPipeline mediaPipeline = userService.findById(sessionId).getMediaPipeline();
//        if (mediaPipeline != null) {
//          mediaPipeline.release();
//        }

        thisPipeline = null;
        // mediaPipeline = null;
        presenters.remove(sessionId); // presenters에서 session 삭제
        roomSessionRepository.delete(sessionId);
        rooms.remove(sessionId);

        // refactor
//        userService.remove(sessionId);
//        roomService.remove(roomId);


        System.out.println("=======presenter session closed========");
        // refactor
//      } else if (roomService.isViewerExist(roomId, sessionId)) { // viewer라면
      } else if (room.containsKey(sessionId)) { // viewer라면
        // refactor
//        WebRtcEndpoint viewerWebRtcEndpoint = userService.findById(sessionId).getWebRtcEndpoint();
//        if (viewerWebRtcEndpoint != null) {
        if (room.get(sessionId).getWebRtcEndpoint() != null) { // viewer의 webrtcep
          room.get(sessionId).getWebRtcEndpoint().release();

          // refactor
          // viewerWebRtcEndpoint.release();

          // Sub view
          Optional<RoomSession> roomSession = roomSessionRepository.findById(roomId);
          roomSession.get().subView();

          //refactor
          roomService.subViewer(roomId, sessionId);


        }
        room.remove(sessionId);
      }
    } else {
      System.out.println("There is no ROOM!");
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
  }

}
