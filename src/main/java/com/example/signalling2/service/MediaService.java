package com.example.signalling2.service;

import com.example.signalling2.common.Constant;
import com.example.signalling2.controller.dto.Response.MessageDto;
import com.example.signalling2.domain.Room;
import com.example.signalling2.controller.dto.Response.VodResponseDto;
import com.example.signalling2.common.exception.KurentoException;
import com.example.signalling2.common.exception.ServiceException;
import com.example.signalling2.common.exception.errcode.KurentoErrCode;
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
 * 미디어 서버 관련 객체(media pipeline, webRtcEndpoint, recordEndpoint 등)의
 * 생성/복구/연결/메모리해제 관련 로직을 담당하는 클래스 입니다.
 *
 * HttpClient를 사용하여 녹화한 영상을 업로드 서버에 저장하거나,
 * 미디어 서버의 cpu/memory 사용량을 확인하여 사내 메신저로 메세지를 보내는
 * 부가기능도 수행합니다.
 *
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

    @Value("${smileHub.url}")
    private String smileHubUrl;

    public String cpuUsage() {
        Float cpu = kurento.getServerManager().getUsedCpu(1000);
        String percent = String.format("%.2f", cpu);
        return percent;
    }

    public String memUsage() {
        Float mem = (float) kurento.getServerManager().getUsedMemory();
        mem = (float) (((mem / 1024) / 1024) * 7.87);
        String percent = String.format("%.2f", mem);
        return percent;
    }

    public MediaPipeline createPipeline(String email) throws KurentoException {
        // 미디어 파이프라인, 엔드포인트 생성
        try {
            MediaPipeline mediaPipeline = kurento.createMediaPipeline();
            mediaPipeline.setName(email);
            String cpu = cpuUsage();
            String mem = memUsage();
            if (Float.parseFloat(cpu) > 0 | Float.parseFloat(mem) > 0) {
                String text = String.format("**WARNING** CPU USAGE: %s MEMORY USAGE: %s", cpu + "%", mem + "%");
                sendToSmileHub(text);
            }
            return mediaPipeline;
        } catch (Exception e) { // change unknown err to webSocketException
            String text = String.format("**ERROR** " + KurentoErrCode.KMS_NO_PIPELINE.getMessage());
            sendToSmileHub(text);
            throw new KurentoException(KurentoErrCode.KMS_NO_PIPELINE);
        }
    }

    public void sendToSmileHub(String text) {
        MessageDto messageDto = new MessageDto(text);
        var json = new GsonBuilder().create().toJson(messageDto, MessageDto.class);
        var entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        HttpClientUtils.sendPostRequest(smileHubUrl, entity);
    }


    public WebRtcEndpoint createEndpoint(String email, String pipelineId) throws KurentoException {
        try {
            final int bandWidth = Constant.BAND_WIDTH;
            MediaPipeline pipeline = getPipeline(pipelineId);
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoint.setMinVideoSendBandwidth(bandWidth);
            webRtcEndpoint.setMaxVideoSendBandwidth(bandWidth);
            webRtcEndpoint.setMinVideoRecvBandwidth(bandWidth);
            webRtcEndpoint.setMaxVideoRecvBandwidth(bandWidth);

            webRtcEndpoint.setName(email + "_WebEndpoint");
            return webRtcEndpoint;
        } catch (Exception e) {
            String text = String.format("**ERROR** " + KurentoErrCode.KMS_NO_ENDPOINT.getMessage());
            sendToSmileHub(text);
            throw new KurentoException(KurentoErrCode.KMS_NO_ENDPOINT);
        }
    }

    /**
     * WebRtcEndpoint 객체 복원
     * 만약 복원 못하면 새로 만듬
     *
     * @param endpoint
     * @return WebRtcEndpoint
     */
    public WebRtcEndpoint getEndpoint(String endpoint) {
        try {
            return kurento.getById(endpoint, WebRtcEndpoint.class);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_RESTORE_ENDPOINT);
        }
    }

    public RecorderEndpoint getRecorderEndpoint(String endpoint) {
        try {
            return kurento.getById(endpoint, RecorderEndpoint.class);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_RESTORE_ENDPOINT);
        }
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
            throw new KurentoException(KurentoErrCode.KMS_RESTORE_PIPELINE);
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
            WebRtcEndpoint webRtcEndpoint = kurento.getById(endpoint, WebRtcEndpoint.class);
            webRtcEndpoint.release();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_RELEASE_ENDPOINT);
        }
    }

    public void releaseRecorderEndpoint(String recorderEndpointId) {
        try {
            RecorderEndpoint recorderEndpoint = getRecorderEndpoint(recorderEndpointId);
            recorderEndpoint.release();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_RELEASE_ENDPOINT);
        }
    }

    public void releasePipeline(MediaPipeline pipeline) {
        try {
            pipeline.release();
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_RELEASE_PIPELINE);
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
