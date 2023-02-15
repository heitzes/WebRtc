package com.example.signalling2;

import com.example.signalling2.domain.RoomRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.ServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@DisplayName("Kurento server managing test")
@SpringBootTest
@ActiveProfiles("dev")
class Signalling2ApplicationTests {
    @Autowired
    private KurentoClient kurentoClient;

    @Test
    void contextLoads() {
        ServerManager serverManager = kurentoClient.getServerManager();
        List<MediaPipeline> mediaPipelineList = serverManager.getPipelines();
        System.out.println(mediaPipelineList);
        for (MediaPipeline pipeline : mediaPipelineList) {
            System.out.println(pipeline);
            System.out.println(pipeline.getId());
            pipeline.release();
        }
    }

}
