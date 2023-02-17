package com.example.signalling2.utils;

import com.example.signalling2.domain.User;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Component;

/**
 * WebRtcEndpoint, MediaPipeline 객체의 복원이 필요할 때
 * 사용하는 메서드를 제공합니다.
 */
@Component
@RequiredArgsConstructor
public class KurentoUtil {

    private final KurentoClient kurento;

    /**
     * WebRtcEndpoint 객체 복원
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

    /**
     * MediaPipeline 객체 복원
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
}
