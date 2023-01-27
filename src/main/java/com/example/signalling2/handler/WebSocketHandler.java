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
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

  private final Gson gson;
  private final KurentoClient kurento;
  private final UserService userService;
  private final RoomService roomService;
  private static String firstRoomId; // TODO: remove this variable when refactor2 in viewer method is finished

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

  //// 코드 리뷰 시작 부분
  /**
   * client가 미디어 스트리밍을 시작하면 이 함수를 실행합니다
   * 이 함수는
   * - 스트리머 유저 세션을 생성
   * - 스트리밍 방을 생성(일단은 방id를 스트리머의 세션id로 지정)
   * - kurento client를 통해 미디어 서버에 pipeline 생성을 요청
   * - 생성한 pipeline으로 webRtcEndpoint 생성
   * - 위 정보들을 스트리머 유저 세션에 저장
   * - 생성한 webRtcEndpoint로 sdpAnswer을 생성하고 ICE candidates gathering 함수 실행
   * - 메모리에 스트리머 유저 세션과 스트리밍 방을 저장
   * 기능을 수행합니다.
   */
  private synchronized void presenter(final WebSocketSession session, JsonObject jsonMessage)
      throws IOException {

    // 현재 클라이언트가 스트리밍 중이지 않을때
    if (!roomService.isPresent(session.getId())){
      // 스트리머 유저 세션과 스트리밍 방 생성
      UserSession presenter = new UserSession(session);
      String roomId = session.getId();
      Room room = new Room(presenter);
      firstRoomId = roomId;

      // 1. Media logic - pipeline과 webRtcEndpoint 생성
      MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint presenterWebRtc = new WebRtcEndpoint.Builder(pipeline).build();

      // TODO: same code
      // 2. Change user session - 스트리머 유저 세션에 정보 저장
      presenterWebRtc.setTurnUrl("13ce6e6d6f5d2accbc52f389:W56mkybnOQK+u4Yh@216.39.253.11:443"); // turn
      presenter.setWebRtcEndpoint(presenterWebRtc);
      presenter.setMediaPipeline(pipeline);
      presenter.setRoomId(roomId);

      // 3. Set iceEventHandler and response to client - 스트리머에게 sdpAnswer 응답
      presenterWebRtc.addIceCandidateFoundListener(new IceEventHandler(session));
      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
      String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);
      JsonObject response = ResponseHandler.sdpResponse("presenter", sdpAnswer);

      synchronized (session) {
        presenter.sendMessage(response);
      }
      // ICE candidates gathering
      presenterWebRtc.gatherCandidates();
      // TODO: same code

      // 4. Save UserSession and Room info in server memory
      userService.save(presenter);
      roomService.save(room);
    } else {
      // 현재 클라이언트가 이미 스트리밍중임
      JsonObject response = ResponseHandler.messageResponse("presenter", "already_in");
      session.sendMessage(new TextMessage(response.toString()));
    }
  }

  /**
   * client가 특정 방에 입장을 시도할 때 이 함수를 실행합니다
   * 이 함수는
   * - client에게 받은 roomId로 스트리밍 방과 스트리머의 유저 세션을 찾음
   * - 뷰어 유저 세션을 생성
   * - 스트리머 유저 세션에 저장되어 있는 pipeline을 가져와 webRtcEndpoint 생성하고 스트리머의 webRtcEndpoint와 연결
   * - 위 정보들을 뷰어 유저 세션에 저장
   * - 생성한 webRtcEndpoint로 sdpAnswer을 생성하고 ICE candidates gathering 함수 실행
   * - 메모리에 뷰어 유저 세션을 저장하고 스트리밍 방의 뷰어 목록에 뷰어의 세션 id를 추가함
   */
  private synchronized void viewer(final WebSocketSession session, JsonObject jsonMessage)
      throws IOException {

    String roomId = jsonMessage.get("roomId").getAsString(); // 요청한 방id
    //// refactor
    if (roomService.isEmpty()) {
      JsonObject response = ResponseHandler.messageResponse("viewer", "no_room");
      session.sendMessage(new TextMessage(response.toString()));
    } else {
      // refactor
      if (!roomService.isPresent(roomId)) {
        System.out.println("Can't find requested room. Connected to first room...");
        roomId = firstRoomId; // 첫번째 방
        // refactor2 - 추후 방 없으면 아예 못들어가게 할거임
//        JsonObject response = ResponseHandler.messageResponse("viewer", "no_room");
//        session.sendMessage(new TextMessage(response.toString()));
//        return;
      }
      if (roomService.isViewerExist(roomId, session.getId())) { // 이미 방에 viewer가 존재하는지 확인
        JsonObject response = ResponseHandler.messageResponse("viewer", "already_in");
        session.sendMessage(new TextMessage(response.toString()));
        return;
      }

      // viewer setting
      UserSession presenterSession = roomService.findOwner(roomId);
      UserSession viewer = new UserSession(session);

      // 1. Media logic
      MediaPipeline pipeline = presenterSession.getMediaPipeline();
      WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(pipeline).build();
      // 스트리머의 webRtcEndpoint와 뷰어의 webRtcEndpoint를 연결
      presenterSession.getWebRtcEndpoint().connect(nextWebRtc);

      // TODO: same code
      // 2. Change user session
      nextWebRtc.setTurnUrl("13ce6e6d6f5d2accbc52f389:W56mkybnOQK+u4Yh@216.39.253.11:443"); // turn
      viewer.setWebRtcEndpoint(nextWebRtc);
      viewer.setMediaPipeline(pipeline);
      viewer.setRoomId(roomId);

      // 3. Set iceEventHandler and SDP offer
      nextWebRtc.addIceCandidateFoundListener(new IceEventHandler(session));
      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
      String sdpAnswer = nextWebRtc.processOffer(sdpOffer);
      JsonObject response = ResponseHandler.sdpResponse("viewer", sdpAnswer);

      synchronized (session) {
        viewer.sendMessage(response);
      }
      nextWebRtc.gatherCandidates();
      // TODO: same code

      // 3. Save Viewer
      userService.save(viewer);
      // 스트리밍 방에 viwer 추가
      roomService.addViewer(roomId, session.getId());
    }
  }
//// 코드 리뷰 끝 부분

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
        log.trace("Viewer in this room: "+ roomService.findViewers(roomId));

        // refactor
        MediaPipeline mediaPipeline = userService.findById(sessionId).getMediaPipeline();
        if (mediaPipeline != null) {
          mediaPipeline.release();
        }

        // refactor
        roomService.remove(roomId);
//        userService. (유저부분에서도 뭔가 처리해야할거같은데... 아닌가?)
        log.trace("=======live room closed========");

        // refactor
      } else if (roomService.isViewerExist(roomId, sessionId)) { // viewer라면
        // refactor
        UserSession viewer = userService.findById(sessionId);
        userService.leaveRoom(viewer);
        roomService.subViewer(roomId, sessionId);
        log.trace("Viewer in this room: "+ roomService.findViewers(roomId));
      }
    } else {
      log.warn("There is no ROOM!");
      log.info("Room list: " + roomService.findAll());
    }
  }


  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
  }
}
