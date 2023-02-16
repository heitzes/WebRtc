package com.example.signalling2.controller;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.dto.Request.RoomJoinRequestDto;
import com.example.signalling2.dto.Response.ResponseDto;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RedisRoomService;
import com.example.signalling2.service.UserService;
import com.example.signalling2.utils.ResponseUtil;
import com.example.signalling2.utils.SignalUtil;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RedisRoomController {

    private final RedisRoomService roomService;
    private final UserService userService;
    private final MediaService mediaService;
    private final SignalUtil util;

    @GetMapping("/list")
    public ResponseEntity<Object> getRooms() {

        return ResponseDto.ok(roomService.findAll());
    }


    /**
     * 방을 있는지 확인하고 방에 참가
     */
    @PostMapping("/view")
    public ResponseEntity<String> joinRoom(@RequestHeader("email") String email, @RequestBody RoomJoinRequestDto joinDto) throws KurentoException {
        String roomId = joinDto.getRoomId();
        // 유저세션 생성 및 방 조회
        userService.createById(email);
        roomService.findById(roomId);

        // 엔드포인트 생성/연결 (여기서 발생한 예외는 webSocket 예외로 처리)
        UserSession presenterSession = userService.findById(roomId);
        MediaPipeline pipeline = presenterSession.getMediaPipeline();
        WebRtcEndpoint nextWebRtc = mediaService.createEndpoint(pipeline);
        mediaService.connectEndpoint(presenterSession.getWebRtcEndpoint(), nextWebRtc);

        // 정보 업데이트
        userService.updateById(nextWebRtc, roomId, email);
        roomService.addViewer(roomId, email);

        return ResponseDto.ok(email);
    }

    @DeleteMapping("/view")
    public ResponseEntity<String> leaveRoom(@RequestHeader("email") String email) {
        UserSession user = userService.findById(email);
        String roomId = user.getRoomId();
        String sessionId = user.getSessionId();

        if (roomService.isViewerExist(roomId, email)) { // viewer라면
            roomService.subViewer(roomId, email);
            userService.leaveRoom(sessionId, email);
        }
        return ResponseDto.ok(email);
    }

    @PostMapping("/live")
    public ResponseEntity<String> createRoom(@RequestHeader("email") String email, @RequestBody RoomCreateRequestDto roomDto) throws KurentoException {
        // 유저세션, 방 생성
        userService.createById(email);
        roomService.createById(roomDto);

        // 미디어 파이프라인, 엔드포인트 생성 (여기서 발생한 예외는 webSocket 예외로 처리)
        MediaPipeline pipeline = mediaService.createPipeline();
        WebRtcEndpoint presenterWebRtc = mediaService.createEndpoint(pipeline);
        RecorderEndpoint recorderEndpoint = mediaService.createRecorderEndpoint(pipeline, roomDto);

        // 레코더 엔드포인트 연결, 녹화 시작
        mediaService.connectRecorderEndpoint(presenterWebRtc, recorderEndpoint);
        mediaService.beginRecording(recorderEndpoint);

        // 정보 업데이트
        userService.updateById(presenterWebRtc, email, email);
        userService.updateById(pipeline, email);

        return ResponseDto.created(email);
    }

    /**
     * webSocket 핸들러의 stop 기능을 컨트롤러로 옮긴 이유
     * 클라이언트와의 websocket 연결은 sdp negotiate, candidate gathering을 진행하기 위해 사용되었는데,
     * 1. stop 메서드에서는 단지 client에 stopCommunication 메세지만 보냄
     * 2. webSocket 세션은 이미 서버에 저장되어 있음 -> webSocket 핸들러에서 세션을 받지 않아도 됨
     * -> stop 기능이 webSocket 핸들러 안에서 구현될 필요가 없음 & webSocket 핸들러 안에서 예외가 생기는 상황을 피하고자 컨트롤러로 옮김
     * BUT, webSocket 핸들러에서 webSocket 커넥션이 끊어졌을 때 stop 기능을 수행했었는데,
     * stop 기능을 컨트롤러로 옮기면 커넥션 끊어진 상황에 대한 대처가 필요.
     */
    @DeleteMapping("/live")
    public ResponseEntity<String> deleteRoom(@RequestHeader("email") String email) {

        Set<String> viewers = roomService.findViewers(email); // refactor:
        for (String viewerId : viewers) {
            UserSession viewer = userService.findById(viewerId);
            JsonObject response = ResponseUtil.messageResponse("stop", "");
            util.sendMessage(viewer.getSession(), response); // 실패시 webSocketException 발생, exceptionHandler가 catch한다
            userService.leaveRoom(viewer.getSession().getId(), email);
        }

        // release media pipeline/endpoint and remove user
        UserSession presenter = userService.findById(email);
        userService.deleteRoom(presenter.getSessionId(), email);

        // remove room
        roomService.delete(email);
        return ResponseDto.ok(email);
    }
}
