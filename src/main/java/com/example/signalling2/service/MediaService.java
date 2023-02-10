package com.example.signalling2.service;

import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.exception.errcode.KurentoErrCode;
import lombok.RequiredArgsConstructor;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.bind.annotation.RestController;
/**
 * to handle exceptions related to kms */
@RestController
@RequiredArgsConstructor
public class MediaService {
    private final KurentoClient kurento;
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

    public void connectEndpoint(WebRtcEndpoint presenterEp, WebRtcEndpoint viewerEp) throws KurentoException {
        try {
            presenterEp.connect(viewerEp);
        } catch (Exception e) {
            throw new KurentoException(KurentoErrCode.KMS_NO_CONNECT);
        }
    }
}
