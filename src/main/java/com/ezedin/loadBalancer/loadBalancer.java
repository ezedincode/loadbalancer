package com.ezedin.loadBalancer;
import jakarta.servlet.http.HttpServlet;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;



public class loadBalancer {
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
    }
}
