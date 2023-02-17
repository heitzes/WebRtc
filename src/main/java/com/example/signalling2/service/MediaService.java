package com.example.signalling2.service;

import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.exception.errcode.KurentoErrCode;
import com.example.signalling2.utils.HttpClientUtils;
import com.example.signalling2.utils.JsonRpcUtil;
import com.example.signalling2.utils.KurentoUtil;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.kurento.client.*;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.RestController;

/**
 * to handle exceptions related to kms
 *
 * mediaPipeline, webRtcEndpoint를 같은 kurento Client Session을 사용하여 생성하기 위해
 * Json-RPC Request로 요청을 보냅니다.
 *
 * connect, release 등 mediaPipeline/WebRtcEndpoint 클래스의 메서드를 실행할 때
 * 객체의 id(String)를 매개변수로하여 Json-RPC API를 통해 실행합니다.
 * 즉, 시그널 서버에서 더이상 미디어 서버 관련 객체를 저장하고 있을 필요가 없습니다.
 *
 * 미디어 객체 복구가 필요한 경우 (ex. WebRtcEndpoint에 IceCandidateFound Event Listener 추가해야하는 경우 등)
 * WebSocketUtil 클래스에 있는 메서드를 사용하여 객체를 복구할 수 있습니다.
 *
 */
@RestController
@RequiredArgsConstructor
public class MediaService {

    private static final String RECORDING_PATH = "file:///video/";
    private static final String EXT_MP4 = ".mp4";
    private final KurentoClient kurento;
    private final KurentoUtil kurentoUtil;

    @Value("${upload.server.url}")
    private String uploadServerUrl;

    public Pair<String, String> createPipeline() throws KurentoException {
        // 미디어 파이프라인, 엔드포인트 생성
        try {
            Request<JsonObject> request = JsonRpcUtil.pipelineRequest();
            Response<JsonElement> response = kurento.sendJsonRpcRequest(request);
            JsonObject result = response.getResult().getAsJsonObject();
            String pipeline = result.get("value").getAsString();
            String kurentoSessionId = response.getSessionId();
            return Pair.of(pipeline, kurentoSessionId);
        } catch (Exception e) { // change unknown err to webSocketException
            throw new KurentoException(KurentoErrCode.KMS_NO_PIPELINE);
        }
    }

    public String createEndpoint(String pipeline, String sessionId) throws KurentoException {
        try {
            Request<JsonObject> request = JsonRpcUtil.endpointRequest(pipeline, sessionId);
            Response<JsonElement> response = kurento.sendJsonRpcRequest(request);
            JsonObject result = response.getResult().getAsJsonObject();
            String endpoint = result.get("value").getAsString();
            return endpoint;
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    public RecorderEndpoint createRecorderEndpoint(String pipelineId, final RoomCreateRequestDto roomDto) throws KurentoException {
        try {
            MediaPipeline pipeline = kurentoUtil.getPipeline(pipelineId); // notice: pipeline 복원
            final var uri = RECORDING_PATH + roomDto.getRoomId() + EXT_MP4;
            final var recorderEp = new RecorderEndpoint.Builder(pipeline, uri).withMediaProfile(MediaProfileSpecType.MP4).build();

            recorderEp.addMediaFlowInStateChangedListener(e -> {
                if (e.getState().equals(MediaFlowState.NOT_FLOWING)) {
                    recorderEp.stopAndWait(new Continuation<>() {
                        @Override
                        public void onSuccess(Void result) {
                            // make request entity (json)
                            var json = new GsonBuilder().create().toJson(roomDto, RoomCreateRequestDto.class);
                            var entity = new StringEntity(json, ContentType.APPLICATION_JSON);

                            // send upload request to upload server (VM) & release MediaPipeline
                            HttpClientUtils.sendPostRequest(uploadServerUrl, entity);
                            pipeline.release();
                        }

                        @Override
                        public void onError(Throwable cause) throws Exception {
                        }
                    });
                }
            });

            return recorderEp;
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    public void connectEndpoint(String artistEp, String fanEp, String kurentoId) throws KurentoException {
        try {
            Request<JsonObject> request = JsonRpcUtil.connectRequest(artistEp, fanEp, kurentoId);
            kurento.sendJsonRpcRequest(request);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_CONNECT);
        }
    }

    public void connectRecorderEndpoint(String endpointId, RecorderEndpoint recorderEp) throws KurentoException {
        try {
            WebRtcEndpoint presenterEp = kurentoUtil.getEndpoint(endpointId); // notice: ep 복원
            presenterEp.connect(recorderEp, MediaType.AUDIO);
            presenterEp.connect(recorderEp, MediaType.VIDEO);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_CONNECT);
        }
    }

    public void releaseMedia(String mediaObject, String kurentoId) {
        try {
            Request<JsonObject> request = JsonRpcUtil.releaseRequest(mediaObject, kurentoId);
            kurento.sendJsonRpcRequest(request);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_PIPELINE);
        }
    }

    public void beginRecording(RecorderEndpoint recorderEp) {
        recorderEp.record();
    }
}
