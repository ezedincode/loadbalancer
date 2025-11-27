package com.ezedin.loadBalancer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class main {
    public static void main(String[] args) throws IOException {
        String applicationName ="";
        var client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(3,TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url("http://eureka-server:8761/eureka/apps/" + applicationName)
                .get()
                .build();
        var response = client.newCall(request).execute();
    }
    public List<String> urlExtractor(Response response) throws JsonProcessingException {
        List <String> list= new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(String.valueOf(response));

        JsonNode instances = root.path("application").path("instance");
        for (JsonNode instance : instances) {
            String host = instance.path("hostName").asText();
            int port = instance.path("port").asInt();
            list.add("http://"+host + ":" + port);
            System.out.println("Host: " + host + ", Port: " + port);
        }
        return list;

    }

}
