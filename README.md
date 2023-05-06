# Signal Server
# [1] 개요

### 1. 소개

1) 주제:  실시간 스트리밍 서비스 Weverse 어플의 클론 코딩

2) 인원: iOS 1명, Back-end 3명

3) 기간: 2022.12.19 ~ 2023.03.03

### 2. 담당 업무

실시간 스트리밍을 위한 Back-end 개발을 담당하며 아래와 같은 업무를 수행했습니다.

- 시그널 서버 개발
    - WebRTC 연결에 필요한 시그널링 기능 구현
    - 실시간 방송 생성/종료 및 시청/떠나기 기능 구현
    - 미디어 서버 메모리 관리를 위한 기능 구현
- 클라이언트/시그널 서버/미디어 서버 사이의 통신을 고려한 서버 아키텍처 설계

# [2] 아키텍처

![archi](https://user-images.githubusercontent.com/66466798/236638329-b2b397ae-97ae-43ce-9ea9-b6dfe051704b.png)

---

### 1. 통신 환경 및 대상

사설 망 안의 Signal/Media Server는 다른 사설 망 안의 특정 Client에 대해서만 IPsec을 통해 제한적으로 통신해야하는 규정이 있었습니다.

- iOS 클라이언트
- 시그널 서버
- 미디어 서버 (오픈 소스 사용)

### 2. 통신 타입

**1) Client ↔  Signal Server**

- **HTTPS/TCP**: 스트리머의 방송 생성/종료 및 시청자의 방송 참여/떠나기 요청을 받고 요청에 응답합니다.
- **WebSocket/TCP**: 시그널 서버를 통해 클라이언트와 미디어 서버 사이의 WebRTC 연결에 필요한 SDP/ICE Candidate을 교환합니다.
    - WebRTC의 시그널링 과정에 반드시 WebSocket을 사용해야하는 것은 아니지만, 다수의 candidate 후보를 여러번 교환하기 위해 WebSocket을 사용했습니다.

**2) Signal Server ↔ Media Server**

- **WebSocket/TCP**: 오픈소스사에서 제공하는 WebSocket 기반의 Java Client를 통해 미디어 서버를 제어합니다.
    - 시그널 서버는 클라이언트를 대신하여 Media Server에 방송 생성/시청에 필요한 미디어 자원(MediaPipeline, WebRtcEndpoint)을 요청합니다.
        - MediaPipeline은 방송 생성에 필요한 미디어 자원이고, WebRtcEndpoint는 방송 시청에 필요한 자원입니다.

**3) Client ↔ Media Server**

- **RTP/UDP**: 송출자는 자신의 음성 및 영상의 스트림 패킷을 미디어 서버로 보내고, 수신자는 미디어 서버를 통해 송출자의 미디어 스트림을 수신합니다.
    - 미디어 서버를 추가함으로써 P2P 구조를 기본으로 하는 WebRTC 기술을 서버-클라이언트 구조인 SFU 구조로 사용할 수 있습니다.
        - N:M 상황이 아닌 1:N 상황에서의 스트리밍을 위해서 P2P가 아닌 SFU 구조를 택했습니다.
        
        
<img width="601" alt="p2p" src="https://user-images.githubusercontent.com/66466798/236638365-38e087f1-4c61-4a61-9cc4-d1f5068a5c9a.png">
        

# [3] 소스 코드

미디어 서버 자원 관리를 위해 작성한 소스 코드입니다.

### 1. 비정상적인 종료에 대한 자원 회수 기능

```java
**// WebSocketHandler.java**

@Override
public void **afterConnectionClosed**(WebSocketSession session, CloseStatus closeStatus) {
		// 1. 정상적인 종료는 API 요청을 통해 이뤄지고, 정상 종료시 유저세션은 삭제된다 (유저세션은 웹소켓 세션과 다름)
		// -> 유저세션이 남아있는데 웹소켓 세션이 종료되면 비정상적인 종료(앱 강제종료 등)로 간주한다
		// 2. 웹소켓 연결이 끊어졌을 때 호출되는 afterConnectionClosed 함수의 인자엔 끊어진 웹소켓 세션 정보만 존재한다
		// -> 어느 유저의 연결이 끊어졌는지 알기 위해 유저의 웹소켓 세션 id를 유저세션의 id로 지정하고, 유저세션 property에 유저정보(email 등) 저장한다
    if (serviceUtil.sessionExist(session.getId())) {  
        serviceUtil.**deleteSession**(session);
    }
}
```

```java
**// ServiceUtil.java**

public void **deleteSession**(WebSocketSession session) {
    String sessionId = session.getId();
    try {
				// 웹소켓 세션 id로 유저세션을 조회한다
        Session userSession = sessionService.findSessionById(sessionId);
        String email = userSession.getEmail(); // 웹소켓 연결 끊어진 유저의 email
        String roomId = userSession.getRoomId(); // 웹소켓 연결 끊어진 유저가 속한 room
        if (email.equals(roomId)) { // 스트리머가 끊어진 경우 (이 경우, 시청자 자원도 다 정리해야 한다)
            deletePresenterSession(email, sessionId);
        } else { // 시청자가 끊어진 경우 
            **deleteViewerSession**(email, roomId, sessionId);
        }
    } catch(ServiceException e) { // 유저세션 정보를 찾을 수 없는 경우
			  log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
    } 
}

public void **deleteViewerSession**(String email, String roomId, String sessionId) {
  try {
      roomService.subViewer(roomId, email); // 방에서 시청자를 삭제한다
      userService.**leaveRoom**(email); // 시청자가 방을 떠난다 (미디어 자원 회수 과정이 포함되어 있다)
      sessionService.deleteSessionById(sessionId); // 시청자의 유저세션을 삭제한다
  } catch (ServiceException e) {
      log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
  }
}
```

```java
**// UserService**

public void **leaveRoom**(String email) {
    **releaseEndpoint**(email);
    deleteById(email);
}
public void **releaseEndpoint**(String email) {
    User user = findById(email);
    if (user.getWebRtcEndpoint() != null) {
        mediaService.**releaseEndpoint**(user.getWebRtcEndpoint()); // mediaService는 미디어 자원 회수를 담당한다
    }
}
```

```java
**// MediaService**

public void **releaseEndpoint**(String endpoint) {
    try {
        WebRtcEndpoint webRtcEndpoint = kurento.getById(endpoint, WebRtcEndpoint.class);
        webRtcEndpoint.**release**(); // 시청자의 미디어 자원인 WebRtcEndpoint의 자원을 회수한다
    } catch (Exception e) {
        throw new KurentoException(KurentoErrCode.KMS_RELEASE_ENDPOINT);
    }
}
```

- WebSocketHandler
    - [https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/handler/WebSocketHandler.java](https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/handler/WebSocketHandler.java)
- ServiceUtil
    - [https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/utils/ServiceUtil.java](https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/utils/ServiceUtil.java)
- UserService
    - [https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/service/UserService.java](https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/service/UserService.java)
- MediaService
    - [https://github.com/heitzes/WebRtc/blob/f56670c69352f69589f105bd27cbddedd39f2e86/src/main/java/com/example/signalling2/service/MediaService.java#L187](https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/service/MediaService.java)

### 2. 잉여 자원 조회 및 자원 회수 기능

```java
**// KurentoController**

// 잉여 자원의 목록을 조회할 수 있는 API
@GetMapping("/pipelines/exception")
public ResponseEntity<Object> getUnusedPipelines() {
    List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines(); // 현재 활성화된 미디어 자원
    ArrayList<String> roomList = roomService.findAllPipelines(); // 현재 방송에서 사용중인 미디어 자원
    List<PipelineResponseDto> piplineList = new ArrayList<>();
    for (MediaPipeline pipeline : pipelines) {
        if (!roomList.contains(pipeline.getId())) { // 방송에서 사용중이지 않은 미디어 자원은 잉여 자원이다
            PipelineResponseDto pipelineResponseDto = new PipelineResponseDto(pipeline.getName(), pipeline.getId());
            piplineList.add(pipelineResponseDto);
        }
    }
    return Response.ok(piplineList);
}

// 잉여 자원에 할당된 자원을 회수할 수 있는 API
@DeleteMapping("/pipelines/exception")
public ResponseEntity removePipelines() {
    List<MediaPipeline> pipelines = kurento.getServerManager().getPipelines();
    ArrayList<String> roomList = roomService.findAllPipelines();
    for (MediaPipeline pipeline : pipelines) {
        if (!roomList.contains(pipeline.getId())) {
            pipeline.release(); // 방송의 미디어 자원인 MediaPipeline의 메모리 자원을 회수
        }
    }
    return Response.noContent();
}
```

- KurentoController
    - [https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/controller/KurentoController.java](https://github.com/heitzes/WebRtc/blob/main/src/main/java/com/example/signalling2/controller/KurentoController.java)
    

# [4] Appendix.

### 1. 기술 선정 이유

<img width="618" alt="live" src="https://user-images.githubusercontent.com/66466798/236638372-cf8dbb7f-a016-434c-aebc-a4004de71469.png">
---

**1) 실시간 스트리밍 기술 조사**

리서치를 통해 RTMP와 WebRTC 기술이 실시간 스트리밍 서비스에서 가장 많이 사용되는 기술임을 알게 되었습니다.

|  | 장점 | 단점 |
| --- | --- | --- |
| RTMP-HLS | 확장성이 있어 대규모 스트리밍에 적합하다. | iOS 기기에는 RTMP 스트림을 수신하여 재생하는 플레이어(Adobe Flash)를 지원하지 않아 RTMP 스트림을 iOS 기기에서 재생 가능한 HLS로 변환하는 과정이 필요하고, 이로 인해 RTMP-HLS를 사용시 5초보다 더 긴 지연 시간을 갖는다. |
| WebRTC | UDP를 기반으로 동작하며 1초 미만의 짧은 지연 시간을 갖는다. | P2P 구조를 기본으로 하는 WebRTC는 송출자인 클라이언트에 많은 부하를 주게 되므로 대규모 스트리밍에는 적합하지 않다. |

**2) 오픈 소스 조사**

|  | 오픈 소스 | 특징 |
| --- | --- | --- |
| RTMP-HLS | https://github.com/arut/nginx-rtmp-module | RTMP로 받은 미디어 스트림을 iOS 클라이언트에서 재생 가능한 HLS로 변환해준다. |
| WebRTC | https://github.com/Kurento/kurento-media-server(미디어 서버) | 미디어 서버를 사용하여 WebRTC 연결을 SFU 구조로 구성하면 P2P 구조의 단점을 극복할 수 있다. 이 때, 미디어 서버를 사용하더라도 WebRTC 연결에 필요한 SDP/ICE candidate 교환을 위한 시그널 서버는 직접 개발해야한다. |

➡️ **개발 역량을 올리기 위해, 시그널 서버를 직접 개발해야하는 WebRTC 선택**

### 2. Document 리서치

시그널 서버에서 미디어 서버를 효율적으로 제어하기 위해, 오픈소스인 Kurento Media Server의 [공식 문서](https://doc-kurento.readthedocs.io/en/latest/)를 꼼꼼히 찾아보며 개발을 진행했습니다.

- release 메서드
    
    MediaPipeline과 WebRtcEndpoint는 KurentoObject 인터페이스를 확장한 인터페이스로, 메모리에서 미디어 객체의 자원을 회수하기 위해 release 메서드를 사용했습니다.
    
    ![스크린샷 2023-04-08 오후 2.08.19.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/57194bca-5b69-400a-ae61-ca108dee6bf1/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2023-04-08_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_2.08.19.png)
    
    [https://doc-kurento.readthedocs.io/en/stable/_static/client-javadoc/org/kurento/client/KurentoObject.html#release()](https://doc-kurento.readthedocs.io/en/stable/_static/client-javadoc/org/kurento/client/KurentoObject.html#release())
    
- connect 메서드
    
    WebRtcEndpoint는 MediaElement 인터페이스를 확장한 인터페이스로, 송출자의 WebRtcEndpoint가 수신하는 미디어 스트림을 시청자의 WebRtcEndpoint에 넘겨주도록 두 WebRtcEndpoint를 연결하는 connect 메서드를 사용했습니다.
    
    ![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/898b1108-3154-4e11-b71c-e927eaae17ea/Untitled.png)
    
    [https://doc-kurento.readthedocs.io/en/latest/_static/client-javadoc/org/kurento/client/MediaElement.html#connect(org.kurento.client.MediaElement)](https://doc-kurento.readthedocs.io/en/latest/_static/client-javadoc/org/kurento/client/MediaElement.html#connect(org.kurento.client.MediaElement))
