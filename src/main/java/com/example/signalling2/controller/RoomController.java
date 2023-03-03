package com.example.signalling2.controller;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.Session;
import com.example.signalling2.domain.User;
import com.example.signalling2.controller.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.controller.dto.Request.RoomJoinRequestDto;
import com.example.signalling2.common.Response;
import com.example.signalling2.controller.dto.Response.RoomResponseDto;
import com.example.signalling2.common.exception.KurentoException;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.SessionService;
import com.example.signalling2.service.UserService;
import com.example.signalling2.utils.ResponseUtil;
import com.example.signalling2.utils.ServiceUtil;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;
    private final MediaService mediaService;
    private final SessionService sessionService;
    private final ServiceUtil util;

    @GetMapping("/list")
    public ResponseEntity<Object> getRooms() {
        ArrayList<RoomResponseDto> responseDto = roomService.findAll();
        return Response.ok(responseDto);
    }

    /**
     * 방을 있는지 확인하고 방에 참가
     */
    @PostMapping("/view")
    public ResponseEntity<RoomResponseDto> joinRoom(@RequestHeader("email") String email, @RequestBody RoomJoinRequestDto joinDto) throws KurentoException {
        String roomId = joinDto.getRoomId();

        // 스트리머/방 조회
        User artist = userService.findById(roomId);
        Room room = roomService.findById(roomId);

        // 엔드포인트 생성/연결 (여기서 발생한 예외는 kurento 예외로 처리)
        WebRtcEndpoint presenterEndpoint = mediaService.getEndpoint(artist.getWebRtcEndpoint());
        WebRtcEndpoint viewerEndpoint = mediaService.createEndpoint(email, room.getMediaPipeline());
        mediaService.connectEndpoint(presenterEndpoint, viewerEndpoint);

        // 유저 생성 및 업데이트
        userService.createById(email, roomId, viewerEndpoint.getId());
        roomService.addViewer(roomId, email);

        // response
        RoomResponseDto roomResponseDto = roomService.createRoomResponseDto(roomId, room);

        return Response.created(roomResponseDto);
    }

    @DeleteMapping("/view")
    public ResponseEntity<Void> leaveRoom(@RequestHeader("email") String email) {
        User user = userService.findById(email);
        Room room = roomService.findById(user.getRoomId());
        String roomId = room.getId();
        String sessionId = user.getSessionId();

        if (roomService.isViewerExist(roomId, email)) { // viewer라면
            roomService.subViewer(roomId, email);
            userService.leaveRoom(email);
            sessionService.deleteSessionById(sessionId);
        }
        return Response.noContent();
    }

    @PostMapping("/live")
    public ResponseEntity<RoomResponseDto> createRoom(@RequestHeader("email") String email, @RequestBody @Valid RoomCreateRequestDto roomDto) throws KurentoException {
        String roomId = roomDto.getRoomId();

        // 미디어 파이프라인, 엔드포인트 생성 (여기서 발생한 예외는 kurento 예외로 처리)
        MediaPipeline pipeline = mediaService.createPipeline(email);
        WebRtcEndpoint endpoint = mediaService.createEndpoint(email, pipeline.getId());

        // 유저세션, 방 생성
        userService.createById(email, roomId, endpoint.getId());
        Room room = roomService.createById(roomDto);

        // 레코더 엔드포인트 생성, 연결, 녹화 시작
        RecorderEndpoint recorderEndpoint = mediaService.createRecorderEndpoint(pipeline, room);
        mediaService.connectRecorderEndpoint(endpoint, recorderEndpoint);
        mediaService.beginRecording(recorderEndpoint);

        // 정보 업데이트
        roomService.updateById(pipeline.getId(), roomId);

        // response
        RoomResponseDto responseDto = roomService.createRoomResponseDto(roomId, room);
        return Response.created(responseDto);
    }

    /**
     * webSocket 핸들러의 stop 함수를 컨트롤러로 옮긴 이유
     * 클라이언트와의 websocket 연결은 sdp negotiate, candidate gathering을 진행하기 위해 사용되었는데,
     * 1. stop 메서드에서는 단지 client에 stopCommunication 메세지만 보냄
     * 2. webSocket 세션은 이미 서버에 저장되어 있음 -> 핸들러에서 webSocket 메세지를 받지 않아도 됨
     * -> stop 기능이 webSocket 핸들러 안에서 구현될 필요가 없음 & webSocket 핸들러 안에서 예외가 생기는 상황을 피하고자 컨트롤러로 옮김
     */
    @DeleteMapping("/live")
    public ResponseEntity<Void> deleteRoom(@RequestHeader("email") String email) {
        roomService.findById(email); // check room exists
        String presenterSessionId = userService.getSessionIdById(email); // check presenter session-id exists

        Set<String> viewers = roomService.findViewers(email); // refactor:
        for (String viewerId : viewers) { // notice: viewerId는 이메일
            String viewerSessionId = userService.getSessionIdById(viewerId); // check null
            Session viewerSession = sessionService.findSessionById(viewerSessionId);
            JsonObject response = ResponseUtil.messageResponse("stop", "exit");
            util.sendMessage(viewerSession.getSession(), response);
            userService.leaveRoom(viewerId);
            sessionService.deleteSessionById(viewerSessionId);
        }

        // release media pipeline/endpoint and remove user
        userService.leaveRoom(email);
        sessionService.deleteSessionById(presenterSessionId);

        // remove room
        roomService.delete(email);
        return Response.noContent();
    }
}
