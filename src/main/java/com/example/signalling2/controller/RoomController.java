package com.example.signalling2.controller;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.dto.ResponseDto;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.UserService;
import com.example.signalling2.utils.ResponseUtil;
import com.example.signalling2.utils.SignalUtil;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;
    private final MediaService mediaService;
    private final SignalUtil util;

    @GetMapping("/rooms")
    public List<String> getRooms() {
        return roomService.findAll();
    }


    /** 방을 있는지 확인하고 방에 참가 */
    @PostMapping("/view")
    public ResponseEntity<String> joinRoom(@RequestHeader("email") String email, @RequestHeader("roomId") String roomId) throws KurentoException {
        // 유저세션 생성 및 방 조회
        userService.createById(email);
        roomService.findById(roomId);

        // 엔드포인트 생성/연결 (여기서 발생한 예외는 webSocket 예외로 처리)
        UserSession presenterSession = userService.findById(roomId);
        MediaPipeline pipeline = presenterSession.getMediaPipeline();
        WebRtcEndpoint nextWebRtc = mediaService.createEndpoint(pipeline);
        mediaService.connectEndpoint(presenterSession.getWebRtcEndpoint(), nextWebRtc);

        // 정보 업데이트
        userService.updateById(email, pipeline, nextWebRtc);
        roomService.addViewer(roomId, email);

        return ResponseDto.ok(email);
    }

    @DeleteMapping("/view")
    public ResponseEntity<String> leaveRoom(@RequestHeader("email") String email, @RequestHeader("roomId") String roomId) {
        if (roomService.isViewerExist(roomId, email)) { // viewer라면
            UserSession viewer = userService.findById(email);
            roomService.subViewer(roomId, email);
            userService.releaseViewer(viewer, email);
        }
        return ResponseDto.ok(email);
    }

    @PostMapping
    public ResponseEntity<String> createRoom(@RequestHeader("email") String email) throws KurentoException { // notice: test 코드임
        // 유저세션, 방 생성
        userService.createById(email);
        roomService.createById(email);

        // 미디어 파이프라인, 엔드포인트 생성 (여기서 발생한 예외는 webSocket 예외로 처리)
        MediaPipeline pipeline = mediaService.createPipeline();
        WebRtcEndpoint presenterWebRtc = mediaService.createEndpoint(pipeline);

        // 정보 업데이트
        userService.updateById(email, pipeline, presenterWebRtc);

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
     * */
    @DeleteMapping
    public ResponseEntity<String> deleteRoom(@RequestHeader("email") String email, @RequestHeader("roomId") String roomId) throws KurentoException {
        ArrayList<String> viewers = roomService.findViewers(roomId);
        for (String viewerId : viewers) {
          UserSession viewer = userService.findById(viewerId);
          JsonObject response = ResponseUtil.messageResponse("stop", "");
          util.sendMessage(viewer.getSession(), response); // 실패시 webSocketException 발생, exceptionHandler가 catch한다
          userService.releaseViewer(viewer, email);
        }

        // release media pipeline
        UserSession presenter = userService.findById(email);
        userService.releasePresenter(presenter, email);

        // remove room
        roomService.remove(roomId);
        return ResponseDto.ok(email);
    }
}
