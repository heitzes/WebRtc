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

package com.example.signalling2.domain;

import com.google.gson.JsonObject;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import java.io.IOException;

@Getter
@Setter
@NoArgsConstructor
public class UserSession {

  @Id
  private String id; // 유저 이메일 -> 유저 세션 아이디로 바꿔야할지도 (커넥션 끊어지는거 대비)
  private String sessionId;
  private WebSocketSession session; // 처음엔 비어있음 (api)
  private WebRtcEndpoint webRtcEndpoint; // 처음엔 비어있음
  private MediaPipeline mediaPipeline;
  private String roomId;

  public UserSession(String email) {
    this.id = email;
  }

  public void addCandidate(IceCandidate candidate) {
    webRtcEndpoint.addIceCandidate(candidate);
  }

  public void sendMessage(JsonObject message) throws IOException {
    session.sendMessage(new TextMessage(message.toString()));
  }
}
