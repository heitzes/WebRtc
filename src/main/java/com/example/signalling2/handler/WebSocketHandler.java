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

import com.example.signalling2.common.Constant;
import com.example.signalling2.utils.ResponseUtil;
import com.example.signalling2.utils.ServiceUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Controller
@RequestMapping("/ws")
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final Gson gson;
    private final ServiceUtil serviceUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "open");
        response.addProperty("session-id", session.getId());
        ServiceUtil.sendMessage(session, response);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
        String type = jsonMessage.get("id").getAsString();
        String roomId = jsonMessage.get("roomId").getAsString();
        switch (type) {
            case "presenter":
            case "viewer":
                String email = jsonMessage.get("email").getAsString();
                String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
                sdpICE(session, sdpOffer, roomId, email, type);
                break;
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                onIceCandidate(session, candidate, roomId);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        if (serviceUtil.sessionExist(session.getId())) {
            log.info("[{}] - 의도치 않게 웹소켓 연결이 끊어진 경우", session.getId());
            serviceUtil.deleteSession(session);
        }
    }

    /**
     * Endpoint 복구가 안되는 경우
     *
     * @param session
     * @param closeStatus
     */
    public void closeConnection(WebSocketSession session, CloseStatus closeStatus) {
        int code = closeStatus.getCode();
        switch(code) {
            case Constant.CLIENT:
                log.error("[{}] - Client Endpoint 복구/생성이 불가한 경우", session.getId());
                serviceUtil.deleteSession(session);
                break;
            case Constant.ARTIST:
                log.error("[{}] - Artist Endpoint 복구/생성이 불가한 경우", session.getId());
                serviceUtil.findAndDeleteArtistSession(session);
                break;
        }
    }


    private void sdpICE(final WebSocketSession session, String sdpOffer, String roomId, String email, String type) {
        log.info("[{}] - sdpICE", session.getId());
        serviceUtil.saveSession(session, roomId, email); // notice: webSocket 저장
        WebRtcEndpoint webRtcEndpoint = serviceUtil.getEndpoint(email); // notice: 복원
        if (webRtcEndpoint == null){ // notice: 복구 불가능하다면 웹소켓 세션 끊고 방/유저 삭제
            closeConnection(session, new CloseStatus(Constant.CLIENT));
        } else {
            webRtcEndpoint.addIceCandidateFoundListener(new IceEventHandler(session));
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
            JsonObject response = ResponseUtil.sdpResponse(type, sdpAnswer);
            synchronized (session) {
                serviceUtil.sendMessage(session, response);
            }
            webRtcEndpoint.gatherCandidates();
        }
    }

    private void onIceCandidate(WebSocketSession session, JsonObject candidate, String roomId) {
        log.info("[{}] - candidate", candidate.get("candidate").getAsString());
        WebRtcEndpoint artistEndpoint = serviceUtil.getEndpoint(roomId);
        if (artistEndpoint == null){
            closeConnection(session, new CloseStatus(Constant.ARTIST));
        } else {
            IceCandidate cand =
                    new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid")
                            .getAsString(), candidate.get("sdpMLineIndex").getAsInt());
            artistEndpoint.addIceCandidate(cand);
        }
    }
}