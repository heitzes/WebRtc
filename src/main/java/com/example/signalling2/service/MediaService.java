package com.example.signalling2.service;

import com.example.signalling2.domain.Room;
import com.example.signalling2.dto.Response.VodResponseDto;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.KurentoErrCode;
import com.example.signalling2.utils.HttpClientUtils;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.kurento.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * to handle exceptions related to kms
 * <p>
 * mediaPipeline, webRtcEndpoint를 같은 kurento Client Session을 사용하여 생성하기 위해
 * Json-RPC Request로 요청을 보냅니다.
 * <p>
 * connect, release 등 mediaPipeline/WebRtcEndpoint 클래스의 메서드를 실행할 때
 * 객체의 id(String)를 매개변수로하여 Json-RPC API를 통해 실행합니다.
 * 즉, 시그널 서버에서 더이상 미디어 서버 관련 객체를 저장하고 있을 필요가 없습니다.
 * <p>
 * 미디어 객체 복구가 필요한 경우 (ex. WebRtcEndpoint에 IceCandidateFound Event Listener 추가해야하는 경우 등)
 * WebSocketUtil 클래스에 있는 메서드를 사용하여 객체를 복구할 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final String RECORDING_PATH = "file:///video/";
    private static final String EXT_MP4 = ".mp4";
    private static final int MEDIA_COUNT = 2;   // audio & video
    private final KurentoClient kurento;

    @Value("${upload.server.url}")
    private String uploadServerUrl;

    public MediaPipeline createPipeline(String email) throws KurentoException {
        // 미디어 파이프라인, 엔드포인트 생성
        try {
            MediaPipeline mediaPipeline = kurento.createMediaPipeline();
            mediaPipeline.setName(email + "_Pipeline");
            return mediaPipeline;
        } catch (Exception e) { // change unknown err to webSocketException
            throw new KurentoException(KurentoErrCode.KMS_NO_PIPELINE);
        }
    }

    public WebRtcEndpoint createEndpoint(String email, String pipelineId) throws KurentoException {
        try {
            final int bandWidth = 1000;
            MediaPipeline pipeline = getPipeline(pipelineId);
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoint.setMinVideoSendBandwidth(bandWidth);
            webRtcEndpoint.setMaxVideoSendBandwidth(bandWidth);
            webRtcEndpoint.setMinVideoRecvBandwidth(bandWidth);
            webRtcEndpoint.setMaxVideoRecvBandwidth(bandWidth);

            webRtcEndpoint.setName(email + "_WebEndpoint");
            return webRtcEndpoint;
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    /**
     * WebRtcEndpoint 객체 복원
     *
     * @param endpoint
     * @return WebRtcEndpoint
     */
    public WebRtcEndpoint getEndpoint(String endpoint) {
        try {
            WebRtcEndpoint webRtcEndpoint = kurento.getById(endpoint, WebRtcEndpoint.class);
            return webRtcEndpoint;
        } catch (ServiceException e) {
            return null;  // fixme: 없으면 어칼건데?
        }
    }

    public RecorderEndpoint getRecorderEndpoint(String endpoint) {
        try {
            return kurento.getById(endpoint, RecorderEndpoint.class);
        } catch (ServiceException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    /**
     * MediaPipeline 객체 복원
     *
     * @param pipeline
     * @return MediaPipeline
     */
    public MediaPipeline getPipeline(String pipeline) {
        try {
            MediaPipeline mediaPipeline = kurento.getById(pipeline, MediaPipeline.class);
            return mediaPipeline;
        } catch (Exception e) {
            return null; // fixme: 없으면 어쩔꺼?
        }
    }

    public RecorderEndpoint createRecorderEndpoint(MediaPipeline pipeline, Room room) throws KurentoException {
        try {
            VodResponseDto vodResponseDto = new VodResponseDto(room.getUuid(), room.getId(), room.getTitle(), room.getProfileUrl());
            final var uri = RECORDING_PATH + vodResponseDto.getRoomId() + EXT_MP4;
            final var recorderEp = new RecorderEndpoint.Builder(pipeline, uri).withMediaProfile(MediaProfileSpecType.MP4).build();
            final var endCount = new AtomicInteger(0);

            recorderEp.addMediaFlowInStateChangedListener(e -> {
                log.info("[{}] - Media flow in state changed {}", recorderEp.getId(), e.getState());

                if (e.getState().equals(MediaFlowState.NOT_FLOWING)) {
                    endCount.set(endCount.get() + 1);

                    if (endCount.get() == MEDIA_COUNT) {
                        recorderEp.stopAndWait(new Continuation<>() {
                            @Override
                            public void onSuccess(Void result) {
                                // make request entity (json)
                                log.info("[{}] - Success to store media", recorderEp.getId());
                                var json = new GsonBuilder().create().toJson(vodResponseDto, VodResponseDto.class);
                                var entity = new StringEntity(json, ContentType.APPLICATION_JSON);

                                // send upload request to upload server (VM) & release MediaPipeline
                                HttpClientUtils.sendPostRequest(uploadServerUrl, entity);

                                releaseRecorderEndpoint(recorderEp.getId());
                                releasePipeline(pipeline);
                            }

                            @Override
                            public void onError(Throwable cause) throws Exception {
                            }
                        });
                    }
                }
            });

            return recorderEp;
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    public void releaseEndpoint(String endpoint) {
        try {
            WebRtcEndpoint webRtcEndpoint = getEndpoint(endpoint);
            webRtcEndpoint.release();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    public void releaseRecorderEndpoint(String recorderEndpointId) {
        try {
            RecorderEndpoint recorderEndpoint = getRecorderEndpoint(recorderEndpointId);
            recorderEndpoint.release();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    public void releasePipeline(MediaPipeline pipeline) {
        try {
            pipeline.release();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_PIPELINE);
        }
    }

    public void connectEndpoint(WebRtcEndpoint artistEp, WebRtcEndpoint fanEp) {
        try {
            artistEp.connect(fanEp);
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
        recorderEp.record(new Continuation<>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("[{}] - Success to Start Recording", recorderEp.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.info("[{}] - Fail to Start Recording", recorderEp.getId());
            }
        });
    }
}
