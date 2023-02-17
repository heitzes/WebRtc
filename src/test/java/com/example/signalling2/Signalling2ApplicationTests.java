package com.example.signalling2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kurento.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;

@DisplayName("Kurento server managing test")
@SpringBootTest
@ActiveProfiles("dev")
class Signalling2ApplicationTests {
    @Autowired
    private KurentoClient kurentoClient;

    @Test
    void contextLoads() throws IOException {
        ServerManager serverManager = kurentoClient.getServerManager();
//        // notice: pipeline 생성
//        Request<JsonObject> request = KurentoUtil.pipelineRequest();
//        Response<JsonElement> response = kurentoClient.sendJsonRpcRequest(request);
//        System.out.println("pipeline response: " + response);
//        JsonObject result = response.getResult().getAsJsonObject();
//        System.out.println("result :" + result);
//        String pipeline = result.get("value").getAsString();
//        String sessionId = response.getSessionId();
//        System.out.println("pipeline: " + pipeline);
//        System.out.println("sessionId: " + sessionId);
//
//        Request<JsonObject> epRequest = KurentoUtil.endpointRequest(pipeline, sessionId);
//        System.out.println("endpoint request: " + epRequest);
//        Response<JsonElement> epResponse = kurentoClient.sendJsonRpcRequest(epRequest);
//        System.out.println("endpoint response: " + epResponse);
//        JsonObject epResult = epResponse.getResult().getAsJsonObject();
//        String ep = epResult.get("value").getAsString();
//        System.out.println("endpoint: " + ep);
//        System.out.println("sessionId: " + epResponse.getSessionId());
//
//        Request<JsonObject> rq1 = KurentoUtil.endpointRequest(pipeline, sessionId);
//        System.out.println("endpoint request: " + rq1);
//        Response<JsonElement> rp1 = kurentoClient.sendJsonRpcRequest(rq1);
//        System.out.println("endpoint response: " + rp1);
//        JsonObject rr1 = rp1.getResult().getAsJsonObject();
//        String ep1 = rr1.get("value").getAsString();
//        System.out.println("endpoint: " + ep1);
//        System.out.println("sessionId: " + rp1.getSessionId());
//
//        Request<JsonObject> cn1 = KurentoUtil.connectRequest(ep, ep1, sessionId);
//        System.out.println("connect request: " + cn1);
//        Response<JsonElement> cr1 = kurentoClient.sendJsonRpcRequest(cn1);
//        System.out.println("connect response: " + cr1);
//        System.out.println("sessionId: " + cr1.getSessionId());
//
////        Request<JsonObject> releaseRequest = KurentoUtil.releaseRequest(5, ep1, sessionId);
////        System.out.println("release request: " + releaseRequest);
////        Response<JsonElement> releaseResponse = kurentoClient.sendJsonRpcRequest(releaseRequest);
////        System.out.println("release response: " + releaseResponse);
//
//        Request<JsonObject> remove1 = KurentoUtil.releaseRequest(pipeline, sessionId);
//        System.out.println("release request: " + remove1);
//        Response<JsonElement> remove2 = kurentoClient.sendJsonRpcRequest(remove1);
//        System.out.println("release response: " + remove2);
//        System.out.println(serverManager.getUsedMemory());
//
//        // todo: addIceCandidateFoundListener, processOffer, gatherCandidates 어칼거임?
//        // todo: addCandidate




//        System.out.println(serverManager.getPipelines().toString());
//        List<MediaPipeline> pipelines = serverManager.getPipelines();
//        for(MediaPipeline p : pipelines) {
//        }
//        System.out.println(serverManager.getSessions());

//         pipeline 제거 테스트
//        List<MediaPipeline> mediaPipelineList = serverManager.getPipelines();
//        System.out.println(mediaPipelineList);
//        for (MediaPipeline pipeline : mediaPipelineList) {
//            System.out.println(pipeline);
//            System.out.println(pipeline.getId());
//            pipeline.release();
//        }

//        Properties properties = new Properties();
//        properties.add("name", "presenter");
//        MediaPipeline pipeline = kurentoClient.createMediaPipeline(properties);
//        WebRtcEndpoint preEp = new WebRtcEndpoint.Builder(pipeline).with("name", "presenter").build();
//        WebRtcEndpoint viewEp = new WebRtcEndpoint.Builder(pipeline).with("name", "viewer").build();
//        preEp.connect(viewEp);

//        Request<JsonObject> request = new Request();
//        JsonObject params = new JsonObject();
//        JsonObject properties = new JsonObject();
//        JsonObject constructorParams = new JsonObject();

        // ping test
//        params.addProperty("interval", 240000);
//        request.setMethod("ping");
//        request.setId(1);
//        request.setParams(params);

        // meia pipeline test
        // type
//        params.addProperty("type", "MediaPipeline");
//        // properties
//        properties.addProperty("name", "presenter");
//        params.add("properties", properties);
//        // constructorParams
//        params.add("constructorParams", constructorParams);
//
//        request.setMethod("create");
//        request.setId(1);
//        request.setParams(params);

//        Response response = kurentoClient.sendJsonRpcRequest(request);
//        System.out.println(response);
        System.out.println(serverManager.getPipelines());
        List<String> sessions = serverManager.getSessions();
        System.out.println(serverManager.getSessions());
        System.out.println(sessions.size());
    }

}
