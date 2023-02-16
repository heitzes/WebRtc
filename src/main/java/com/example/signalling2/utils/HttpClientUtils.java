package com.example.signalling2.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HttpClientUtils {

    public static String sendGetRequest(String uri) {
        var httpClient = HttpClients.createDefault();
        var httpGet = new HttpGet(uri);

        try (var res = httpClient.execute(httpGet)) {
            var resEntity = res.getEntity();
            httpClient.close();
            return EntityUtils.toString(resEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sendPostRequest(String uri, HttpEntity entity) {
        var httpClient = HttpClients.createDefault();
        var postReq = new HttpPost(uri);
        postReq.setEntity(entity);

        try (var res = httpClient.execute(postReq)) {
            var resEntity = res.getEntity();
            httpClient.close();
            return EntityUtils.toString(resEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
