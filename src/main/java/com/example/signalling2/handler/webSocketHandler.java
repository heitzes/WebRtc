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

package com.example.signalling2.handler;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.UserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Protocol handler for 1 to N video call communication.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@Component
//@RequiredArgsConstructor
public class webSocketHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(webSocketHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final KurentoClient kurento;
  private final UserService userService;
  private final RoomService roomService;

  private static String firstRoomId;

  @Autowired
  public webSocketHandler(KurentoClient kurento, UserService userService, RoomService roomService) {
    this.kurento = kurento;
    this.userService = userService;
    this.roomService = roomService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
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
        if (!roomService.isEmpty()) {
          // refactor (추후 onIceCandidate 웹소켓 통신시 roomId도 달라고 해야할듯..)
          user = userService.findById(session.getId());
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
    //refactor
    if (!roomService.isPresent(session.getId())){

      // presenter setting

      // 1. Media logic (webRtcEndpoint in loopback)
      MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint presenterWebRtc = new WebRtcEndpoint.Builder(pipeline).build();
      presenterWebRtc.setTurnUrl("13ce6e6d6f5d2accbc52f389:W56mkybnOQK+u4Yh@216.39.253.11:443"); // turn

      // 2. Store user session
      UserSession presenter = new UserSession(session);
      presenter.setWebRtcEndpoint(presenterWebRtc);
      presenter.setMediaPipeline(pipeline);
      presenter.setRoomId(session.getId());

      // 3. Save presenter
      //// refactor
      userService.save(presenter);
      Room room = new Room(presenter);
      firstRoomId = session.getId();
      roomService.save(room);
      ///
      
      // refactor
      presenterWebRtc.addIceCandidateFoundListener(new iceEventHandler(session));

      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
      String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);
      JsonObject response = new JsonObject();
      response.addProperty("id", "presenterResponse");
      response.addProperty("response", "accepted");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
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
    if (roomService.isEmpty()) {
//    if (presenters.isEmpty()) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "viewerResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "No active sender now. Become sender or . Try again later ...");
      session.sendMessage(new TextMessage(response.toString()));
    } else {

      String roomId = jsonMessage.get("roomId").getAsString(); // 요청한 방id

      //// refactor
      UserSession presenterSession = null;
      WebRtcEndpoint nextWebRtc = null;

      // refactor
       if (!roomService.isPresent(roomId)) {

        System.out.println("Can't find requested room. Connected to first room...");
        roomId = firstRoomId; // 첫번째 방
         
        // refactor2 - 추후 방 없으면 아예 못들어가게 할거임
//        JsonObject response = new JsonObject();
//        response.addProperty("id", "viewerResponse");
//        response.addProperty("response", "rejected");
//        response.addProperty("message",
//                "Can't find room you requested. Room doesn't exist!");
//        session.sendMessage(new TextMessage(response.toString()));

      } else { // show requested streamer
        System.out.println("Found room!");
      }
       // refactor
      presenterSession = roomService.findOwner(roomId);

      // refactor
      if (roomService.isViewerExist(roomId, session.getId())) { // 이미 방에 viewer가 존재하는지 확인
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
      nextWebRtc.setTurnUrl("13ce6e6d6f5d2accbc52f389:W56mkybnOQK+u4Yh@216.39.253.11:443"); // turn

      viewer.setWebRtcEndpoint(nextWebRtc);
      presenterSession.getWebRtcEndpoint().connect(nextWebRtc);
      // refactor - viewerSession에 roomId 저장
      viewer.setRoomId(roomId);

      // Save Viewer
      //// refactor
      userService.save(viewer);
      roomService.addViewer(roomId, session.getId());
      System.out.println("Viewer in this room: "+ roomService.findViewers(roomId));

      // refactor
      nextWebRtc.addIceCandidateFoundListener(new iceEventHandler(session));

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
    // refactor
    String roomId = userService.findRoomId(sessionId); // 방 아무것도 없으면 에러발생

    //// refactor
    if (roomService.isPresent(roomId)){
      if (roomId.equals(sessionId)) { // presenter라면
        // refactor
        ArrayList<String> viewers = roomService.findViewers(roomId);
        for (String viewerId : viewers) {
          // refactor - 방을 떠나도 viewerSession은 사라지지 않는다
          UserSession viewer = userService.findById(viewerId);
          userService.leaveRoom(viewer);

          JsonObject response = new JsonObject();
          response.addProperty("id", "stopCommunication");
          viewer.sendMessage(response);
        }
        System.out.println("Viewer in this room: "+ roomService.findViewers(roomId));

        // refactor
        MediaPipeline mediaPipeline = userService.findById(sessionId).getMediaPipeline();
        if (mediaPipeline != null) {
          mediaPipeline.release();
        }

        // refactor
        roomService.remove(roomId);
//        userService. (유저부분에서도 뭔가 처리해야할거같은데... 아닌가?)


        System.out.println("=======live room closed========");

        // refactor
      } else if (roomService.isViewerExist(roomId, sessionId)) { // viewer라면
        // refactor
        UserSession viewer = userService.findById(sessionId);
        userService.leaveRoom(viewer);
        roomService.subViewer(roomId, sessionId);
        System.out.println("Viewer in this room: "+ roomService.findViewers(roomId));
      }
    } else {
      System.out.println("There is no ROOM!");
      System.out.println(roomService.findAll());
    }
  }


  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
  }
}
