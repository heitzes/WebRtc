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
import lombok.*;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.data.annotation.Id;

import java.io.IOException;

@Getter
@Setter
@AllArgsConstructor
public class Session {
  @Id
  private String id; // refactor: sessionId로 변경 (왜냐면 이제 Session은 웹소켓 관리용으로 쓰일테니)
  private WebSocketSession session; // 어쩔수 없이 signal 서버에서 관리 해야됨
  private String roomId; // 들어가 있는 roomId
  private String email; // refactor: 이 웹소켓 세션 주인의 email
}
