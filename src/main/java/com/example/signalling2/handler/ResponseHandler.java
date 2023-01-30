package com.example.signalling2.handler;

import com.google.gson.JsonObject;

public class ResponseHandler {
    public static JsonObject sdpResponse(String type, String SDP) {
        JsonObject response = new JsonObject();
        switch (type) {
            case "presenter":
                response.addProperty("id", "presenterResponse");
                response.addProperty("response", "accepted");
                response.addProperty("sdpAnswer", SDP);
                break;
            case "viewer":
                response.addProperty("id", "viewerResponse");
                response.addProperty("response", "accepted");
                response.addProperty("sdpAnswer", SDP);
                break;
        }
        return response;
    }
    public static JsonObject messageResponse(String type, String status) {
        JsonObject response = new JsonObject();
        if (type.equals("presenter")) {
            response.addProperty("id", "presenterResponse");
            response.addProperty("response", "rejected");
            switch (status) {
                case "already_in":
                    response.addProperty("message",
                            "You are already acting as sender.");
                    break;
            }
        } else if (type.equals("viewer")) {
            response.addProperty("id", "viewerResponse");
            response.addProperty("response", "rejected");
            switch (status) {
                case "no_room":
                    response.addProperty("message",
                            "Can't find room you requested. Room doesn't exist!");
                    break;
                case "already_in":
                    response.addProperty("message", "You are already viewing in this session. "
                            + "Use a different browser to add additional viewers.");
            }
        } else {
            response.addProperty("id", "stopCommunication");
            response.addProperty("message", "Presenter stopped streaming.");
        }
        return response;
    }
}
