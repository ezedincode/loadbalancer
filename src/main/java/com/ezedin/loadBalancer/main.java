package com.ezedin.loadBalancer;

import com.ezedin.loadBalancer.strategy.RoundRobin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class main {
    public static void main(String[] args) throws Exception {
        String applicationName ="API-GATEWAY";
        var client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(3,TimeUnit.SECONDS)
                .build();

        String username = "eureka";  // Default Eureka username
        String password = "password"; // Default Eureka password
        String credentials = username + ":" + password;
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url("http://localhost:8761/eureka/apps/" + applicationName)
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + base64Credentials) // Add this line
                .build();



        try{ var response = client.newCall(request).execute();

           var lbStrategy = new RoundRobin(urlExtractor(response));
           lbStrategy.startHealthCheck();
            new loadBalancer(8040,lbStrategy);
       } catch (IOException e) {
           System.out.println("no eurika");
       }



       while(true) {

       }


    }
    public static List<String> urlExtractor(Response response) throws IOException {
        List<String> list = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = response.body().string();
        JsonNode root = mapper.readTree(jsonBody);

        // Fixed path - use "application" not "applications"
        JsonNode applicationNode = root.path("application");
        JsonNode instances = applicationNode.path("instance");


        for (JsonNode instance : instances) {
            String ip = instance.get("ipAddr").asText();
            // Port is inside port object with "$" field, not in metadata
            String port = instance.path("port").get("$").asText();
            String url = "http://" + ip + ":" + port;
            //to handle docker internal ip address
            if(!url.contains("127.0.0.1")){
                String url2= "http://localhost"+ ":" + port;
                list.add(url2);
            }
            list.add(url);
        }
        System.out.println(list);
        return list;
    }


}
