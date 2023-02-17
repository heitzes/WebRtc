package com.example.signalling2.utils;

import com.google.gson.JsonObject;
import org.kurento.jsonrpc.message.Request;

public class JsonRpcUtil {
    public static Request<JsonObject> pipelineRequest() {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject constructorParams = new JsonObject();
        JsonObject properties = new JsonObject();

        params.addProperty("type", "MediaPipeline");
        params.add("properties", properties);
        params.add("constructorParams", constructorParams);

        request.setMethod("create");
        request.setParams(params);

        return request;
    }

    public static Request<JsonObject> endpointRequest(String pipeline, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject constructorParams = new JsonObject();
        JsonObject properties = new JsonObject();

        constructorParams.addProperty("mediaPipeline", pipeline);

        params.addProperty("type", "WebRtcEndpoint");
        params.addProperty("sessionId", sessionId);
        params.add("properties", properties);
        params.add("constructorParams", constructorParams);

        request.setMethod("create");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> connectRequest(String artistEndpoint, String fanEndpoint, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject operationParams = new JsonObject();

        operationParams.addProperty("sink", artistEndpoint);

        params.addProperty("object", fanEndpoint);
        params.addProperty("operation", "connect");
        params.add("operationParams", operationParams);
        params.addProperty("sessionId", sessionId);

        request.setMethod("invoke");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> releaseRequest(String mediaObject, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();

        params.addProperty("object", mediaObject);
        params.addProperty("sessionId", sessionId);

        request.setMethod("release");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> sdpRequest(String endpoint, String sdpOffer, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject operationParams = new JsonObject();

        operationParams.addProperty("offer", sdpOffer);

        params.addProperty("object", endpoint);
        params.addProperty("operation", "processOffer");
        params.add("operationParams", operationParams);
        params.addProperty("sessionId", sessionId);

        request.setMethod("invoke");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> gatherRequest(String endpoint, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject operationParams = new JsonObject();

        params.addProperty("object", endpoint);
        params.addProperty("operation", "gatherCandidates");
        params.add("operationParams", operationParams);
        params.addProperty("sessionId", sessionId);

        request.setMethod("invoke");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> addCandidateRequest(String endpoint, String candi, String sdpMid, Integer sdpIndex, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject operationParams = new JsonObject();
        JsonObject candidate = new JsonObject();

        candidate.addProperty("candidate", candi);
        candidate.addProperty("sdpMid", sdpMid);
        candidate.addProperty("sdpMLineIndex", sdpIndex);

        operationParams.add("candidate", candidate);

        params.addProperty("object", endpoint);
        params.addProperty("operation", "addIceCandidate");
        params.add("operationParams", operationParams);
        params.addProperty("sessionId", sessionId);

        request.setMethod("invoke");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> iceFoundRequest(String endpoint, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();

        params.addProperty("type", "IceCandidateFound");
        params.addProperty("object", endpoint);
        params.addProperty("sessionId", sessionId);


        request.setMethod("subscribe");
        request.setParams(params);
        return request;
    }

    public static Request<JsonObject> getCandidateRequest(String endpoint, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();
        JsonObject value = new JsonObject();
        JsonObject data = new JsonObject();

        data.addProperty("source", endpoint);
        data.addProperty("type", "IceCandidateFoundEvent");

        value.addProperty("object", endpoint);
        value.addProperty("type", "IceCandidateFoundEvent");
        value.add("data", data);

        params.add("value", value);
        params.addProperty("operation", "getCandidate");
        params.addProperty("object", endpoint);

        request.setMethod("invoke");
        request.setParams(params);
        request.setSessionId(sessionId);
        return request;
    }

    public static Request<JsonObject> describeRequest(String mediaObject, String sessionId) {
        Request<JsonObject> request = new Request();
        JsonObject params = new JsonObject();

        params.addProperty("object", mediaObject);
        params.addProperty("sessionId", sessionId);

        request.setMethod("describe");
        request.setParams(params);
        return request;
    }
}
