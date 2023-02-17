package com.example.signalling2.controller;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.Session;
import com.example.signalling2.domain.User;
import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.dto.Request.RoomJoinRequestDto;
import com.example.signalling2.dto.Response.ResponseDto;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.SessionService;
import com.example.signalling2.service.UserService;
import com.example.signalling2.utils.ResponseUtil;
import com.example.signalling2.utils.UserSessionUtil;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.RecorderEndpoint;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;
    private final MediaService mediaService;
    private final SessionService sessionService;
    private final UserSessionUtil util;

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
        userService.createById(email, roomId);
        Room room = roomService.findById(roomId);

        // 엔드포인트 생성/연결 (여기서 발생한 예외는 webSocket 예외로 처리)
        User artist = userService.findById(roomId);
        String pipeline = room.getMediaPipeline();
        String kurentoId = room.getKurentoSessionId();
        String endpoint = mediaService.createEndpoint(pipeline, kurentoId);
        mediaService.connectEndpoint(artist.getWebRtcEndpoint(), endpoint, kurentoId);

        // 정보 업데이트
        userService.updateEndpointById(endpoint, email);
        roomService.addViewer(roomId, email);

        return ResponseDto.ok(email);
    }

    @DeleteMapping("/view")
    public ResponseEntity<String> leaveRoom(@RequestHeader("email") String email) {
        User user = userService.findById(email);
        Room room = roomService.findById(user.getRoomId());
        String roomId = user.getRoomId();
        String sessionId = user.getSessionId();

        if (roomService.isViewerExist(roomId, email)) { // viewer라면
            roomService.subViewer(roomId, email);
            userService.leaveRoom(room.getKurentoSessionId(), email);
            sessionService.deleteSessionById(sessionId);
        }
        return ResponseDto.ok(email);
    }

    @PostMapping("/live")
    public ResponseEntity<String> createRoom(@RequestHeader("email") String email, @RequestBody RoomCreateRequestDto roomDto) throws KurentoException {

        // 유저세션, 방 생성
        userService.createById(email, email);
        roomService.createById(roomDto);

        // 미디어 파이프라인, 엔드포인트 생성 (여기서 발생한 예외는 webSocket 예외로 처리)
        Pair<String, String> result = mediaService.createPipeline();
        String pipeline = result.getFirst();
        String kurentoId = result.getSecond();
        String endpoint = mediaService.createEndpoint(pipeline, kurentoId);
        System.out.println("api endpoint: " + endpoint);
        RecorderEndpoint recorderEndpoint = mediaService.createRecorderEndpoint(pipeline, roomDto);

        // 레코더 엔드포인트 연결, 녹화 시작
        mediaService.connectRecorderEndpoint(endpoint, recorderEndpoint);
        mediaService.beginRecording(recorderEndpoint);

        // 정보 업데이트
        userService.updateEndpointById(endpoint, email);
        roomService.updateById(pipeline, kurentoId, email);

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
        Room room = roomService.findById(email);
        Set<String> viewers = roomService.findViewers(email); // refactor:
        for (String viewerId : viewers) { // notice: viewerId는 이메일
            User viewer = userService.findById(viewerId);
            Session viewerSession = sessionService.findSessionById(viewer.getSessionId());
            JsonObject response = ResponseUtil.messageResponse("stop", "");
            util.sendMessage(viewerSession.getSession(), response);
            userService.leaveRoom(room.getKurentoSessionId(), viewerId);
            sessionService.deleteSessionById(viewer.getSessionId());
        }

        // release media pipeline/endpoint and remove user
        User presenter = userService.findById(email);
        userService.leaveRoom(room.getKurentoSessionId(), email);
        sessionService.deleteSessionById(presenter.getSessionId());

        // remove room
        roomService.delete(email);
        return ResponseDto.ok(email);
    }
}
