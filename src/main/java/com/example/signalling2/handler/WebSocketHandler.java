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
import org.apache.commons.logging.Log;
import org.kurento.client.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Controller // refactor: exceptionHandler 사용 위해 component에서 controller로 변경
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
//    if (exception instanceof IOException) {
//      System.out.println("An IOException occurred: " + exception.getMessage());
//      session.close();
//    }

    try {
      handleErrorResponse(exception, session, "presenterResponse");
    } catch (Throwable t) {

    }
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
    log.debug("Incoming message from session '{}': {}", session.getId(), jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
      case "presenter":
        presenter(session, jsonMessage);
        // refactor: exception 처리를 handleTransportError에 위임한다
        break;
      case "viewer":
        viewer(session, jsonMessage);
//        try {
//          viewer(session, jsonMessage);
//        } catch (Throwable t) {
//          handleErrorResponse(t, session, "viewerResponse");
//        }
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
  private void presenter(final WebSocketSession session, JsonObject jsonMessage)
      throws IOException {
    String roomId = session.getId();

    // 현재 클라이언트가 이미 스트리밍중임
    if (roomService.existById(roomId)) {
      JsonObject response = ResponseHandler.messageResponse("presenter", "already_in");
      session.sendMessage(new TextMessage(response.toString()));
      return; // todo: throw exception
    }

    // 스트리머 유저 세션과 스트리밍 방 생성
    UserSession presenter = new UserSession(session);
    Room room = new Room(presenter);
    firstRoomId = roomId;

    // 1. Media logic - pipeline과 webRtcEndpoint 생성
    MediaPipeline pipeline = kurento.createMediaPipeline();
    WebRtcEndpoint presenterWebRtc = new WebRtcEndpoint.Builder(pipeline).build();

    // todo: same code
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

    synchronized (presenter) {
      presenter.sendMessage(response);
    }
    // ICE candidates gathering
    presenterWebRtc.gatherCandidates();
    // todo: same code

    // 4. Save UserSession and Room info in server memory
    userService.save(presenter);
    roomService.save(room);
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
  // todo: webSocket connection 관련해서 발생할 수 있는 exception과 business logic에서 발생하는 exception 분리하기
  private void viewer(final WebSocketSession session, JsonObject jsonMessage)
      throws IOException {

    String roomId = jsonMessage.get("roomId").getAsString(); // 요청한 방id
    // fixme: move this logic to service logic (to separate exception type)
    if (roomService.isEmpty()) {
      JsonObject response = ResponseHandler.messageResponse("viewer", "no_room");
      session.sendMessage(new TextMessage(response.toString()));
      return; // todo: throw exception
    }

    // fixme: move this logic to service logic (to separate exception type)
    if (!roomService.existById(roomId)) {
      System.out.println("Can't find requested room. Connected to first room...");
      roomId = firstRoomId; // 첫번째 방
      // todo: throw exception
      // refactor2 - 추후 방 없으면 아예 못들어가게 할거임
//        JsonObject response = ResponseHandler.messageResponse("viewer", "no_room");
//        session.sendMessage(new TextMessage(response.toString()));
//        return;
    }
    // fixme: move this logic to service logic (to separate exception type)
    if (roomService.isViewerExist(roomId, session.getId())) { // 이미 방에 viewer가 존재하는지 확인
      JsonObject response = ResponseHandler.messageResponse("viewer", "already_in");
      session.sendMessage(new TextMessage(response.toString()));
      return; // todo: throw exception
    }

    // viewer setting
    UserSession presenterSession = roomService.findOwner(roomId);
    UserSession viewer = new UserSession(session);

    // 1. Media logic
    MediaPipeline pipeline = presenterSession.getMediaPipeline();
    WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(pipeline).build();
    // 스트리머의 webRtcEndpoint와 뷰어의 webRtcEndpoint를 연결
    presenterSession.getWebRtcEndpoint().connect(nextWebRtc);

    // todo: 중복 코드 분리하기
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

    synchronized (viewer) {
      viewer.sendMessage(response); // 여기서 발생한 exception은 handleTransportError이 처리하게 됨
    }
    nextWebRtc.gatherCandidates();
    // todo: 중복 코드 분리하기

    // 3. Save Viewer
    userService.save(viewer);
    // 스트리밍 방에 viewer 추가
    roomService.addViewer(roomId, session.getId());
  }
//// 코드 리뷰 끝 부분

  private synchronized void stop(WebSocketSession session) throws IOException {
    String sessionId = session.getId(); // user who requested stop
    String roomId = userService.findRoomId(sessionId); // 방 아무것도 없으면 에러발생
    if (roomId == null) {
      return; // 이미 방을 나가서 roomId가 null임 // todo: throw exception
    }

    if (roomService.existById(roomId)){
      if (roomId.equals(sessionId)) { // presenter라면
        ArrayList<String> viewers = roomService.findViewers(roomId);
        for (String viewerId : viewers) {
          UserSession viewer = userService.findById(viewerId);
          userService.leaveRoom(viewer);
          JsonObject response = ResponseHandler.messageResponse("stop", "");
          viewer.sendMessage(response);
        }

        // release media pipeline
        UserSession presenter = userService.findById(sessionId);
        MediaPipeline mediaPipeline = presenter.getMediaPipeline();
        if (mediaPipeline != null) {
          mediaPipeline.release();
        }

        // leave room and remove room
        userService.leaveRoom(presenter);
        roomService.remove(roomId);
        System.out.println("=======live room closed========");

      } else if (roomService.isViewerExist(roomId, sessionId)) { // viewer라면
        UserSession viewer = userService.findById(sessionId);
        userService.leaveRoom(viewer);
        roomService.subViewer(roomId, sessionId);
        System.out.println("Viewer in this room: "+ roomService.findViewers(roomId));
      }
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
  }
}
