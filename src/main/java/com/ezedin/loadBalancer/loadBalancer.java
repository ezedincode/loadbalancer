package com.ezedin.loadBalancer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import com.ezedin.loadBalancer.strategy.loadBalancerStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class loadBalancer {
    private loadBalancerStrategy  lbStrategy;
    protected loadBalancer(int port){
        var server = new Server(new QueuedThreadPool(20));
        var connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);


        var context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new loadBalancerServlet(this)), "/");

    }
    public static class loadBalancerServlet extends HttpServlet {
        private transient loadBalancer loadbalancer;

        public loadBalancerServlet(loadBalancer loadBalancer) {
            this.loadbalancer=loadBalancer;
        }
        private Response executeRequest(String beUrl, HttpServletRequest request) throws IOException {
            var httpClient = new OkHttpClient();
            var reqBuilder = new Request.Builder()
                    .url(beUrl + request.getRequestURI());
            var reqHeaders = request.getHeaderNames();
            while (reqHeaders.hasMoreElements()) {
                var reqHeaderKey = reqHeaders.nextElement();
                var reqHeaderValue = request.getHeader(reqHeaderKey);
                reqBuilder.header(reqHeaderKey, reqHeaderValue);
            }
            var beRequest = reqBuilder.build();
            return httpClient.newCall(beRequest).execute();
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

            var retryCount = 2;
            while (retryCount > 0) {
                var be = loadbalancer.lbStrategy.getNext();
                if (be.isEmpty()) {
                    throw new ServletException("No backend found");
                }
                var beUrl = be.get();
                try {
                    var beResponse = this.executeRequest(beUrl, request);
                    fetchResponseAndSendBack(request, response, beUrl, beResponse);
                    return;
                } catch (IOException e) {
                    loadbalancer.lbStrategy.unhealthy(beUrl);
                    retryCount--;
                }
            }
        }

        private void fetchResponseAndSendBack(HttpServletRequest request, HttpServletResponse response, String beUrl,
                                              Response beResponse) throws IOException {
            response.setStatus(beResponse.code());

            for (String name : beResponse.headers().names()) {
                response.setHeader(name, beResponse.header(name));
            }

            ResponseBody body = beResponse.body();
            if (body != null) {
                try (InputStream in = body.byteStream();
                     OutputStream out = response.getOutputStream()) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
            }
            beResponse.close();
        }

        }
    }
