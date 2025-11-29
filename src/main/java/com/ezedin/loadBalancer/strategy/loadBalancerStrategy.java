package com.ezedin.loadBalancer.strategy;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class loadBalancerStrategy {
    private List<String> servers;
    private List<String> validServers;
    private List<String> invalidServers;

    protected loadBalancerStrategy() {
        this.servers = new ArrayList<>();
        this.validServers = new ArrayList<>();
        this.invalidServers = new ArrayList<>();
    }
    protected loadBalancerStrategy(List<String> list ) {
        this.servers = new ArrayList<>(list);
        this.validServers = new ArrayList<>();
        this.invalidServers = new ArrayList<>();
    }
    public List<String> getServers() {
        return new ArrayList<>(this.validServers);
    }

    public abstract Optional<String> getNext();
    public abstract Optional<String> getCurrent();
    public void startHealthCheck(){
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        System.out.println("in startHealthCheck");
        Runnable runnable = () -> {
            var list = new ArrayList<String>(this.servers);
            for (String server : list){
                CompletableFuture.runAsync(() -> this.tryHealthCheck(server));
            }
        };
        scheduler.scheduleAtFixedRate(runnable,0,2, TimeUnit.SECONDS);
    }

    public boolean tryHealthCheck(String url) {
        System.out.println("in tryHealthCheck");
        if (this.isHealthy(url)) {
            this.healthy(url);
            return true;
        } else {
            this.unhealthy(url);
            return false;
        }
    }

    public void unhealthy(String url) {
        if(!this.invalidServers.contains(url)) {
            this.invalidServers.add(url);
        }
        this.validServers.remove(url);

    }

    private void healthy(String url) {
        this.invalidServers.remove(url);
        if(!this.validServers.contains(url)){
            this.validServers.add(url);
        }
    }

    private boolean isHealthy(String url) {
        System.out.println("in isHealthy");
        if(!isTcpHealthy(url)){
            return false;
        }
            var client = new OkHttpClient.Builder()
                    .connectTimeout(1,TimeUnit.SECONDS)
                    .readTimeout(3,TimeUnit.SECONDS)
                    .build();
            var request = new Request.Builder()
                    .url(url + "/health")
                    .get()
                    .build();
        try (var response = client.newCall(request).execute()) {
            return response.code() == 200 && response.body().string().contains("healthy");
        }
    catch (Exception e) {
        return false;
    }

    }
    private boolean isTcpHealthy(String urlString) {
        int timeout = 1000;
        try(var socket = new Socket()){
            var url = new URL(urlString);
            socket.connect(new InetSocketAddress(url.getHost(),url.getPort()),timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
