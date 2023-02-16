package com.example.signalling2.service;

import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.exception.errcode.KurentoErrCode;
import com.example.signalling2.utils.HttpClientUtils;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.kurento.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

/**
 * to handle exceptions related to kms
 */
@RestController
@RequiredArgsConstructor
public class MediaService {

    private static final String RECORDING_PATH = "file:///video/";
    private static final String EXT_MP4 = ".mp4";
    private final KurentoClient kurento;

    @Value("${upload.server.url}")
    private String uploadServerUrl;

    public MediaPipeline createPipeline() throws KurentoException {
        // 미디어 파이프라인, 엔드포인트 생성
        try {
            return kurento.createMediaPipeline();
        } catch (Exception e) { // change unknown err to webSocketException
            throw new KurentoException(KurentoErrCode.KMS_NO_PIPELINE);
        }
    }

    public WebRtcEndpoint createEndpoint(MediaPipeline pipeline) throws KurentoException {
        try {
            return new WebRtcEndpoint.Builder(pipeline).build();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    public RecorderEndpoint createRecorderEndpoint(final MediaPipeline pipeline, final RoomCreateRequestDto roomDto) throws KurentoException {
        try {
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

    public void connectEndpoint(WebRtcEndpoint presenterEp, WebRtcEndpoint viewerEp) throws KurentoException {
        try {
            presenterEp.connect(viewerEp);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_CONNECT);
        }
    }

    public void connectRecorderEndpoint(WebRtcEndpoint presenterEp, RecorderEndpoint recorderEp) throws KurentoException {
        try {
            presenterEp.connect(recorderEp, MediaType.AUDIO);
            presenterEp.connect(recorderEp, MediaType.VIDEO);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_CONNECT);
        }
    }

    public void beginRecording(RecorderEndpoint recorderEp) {
        recorderEp.record();
    }
}
