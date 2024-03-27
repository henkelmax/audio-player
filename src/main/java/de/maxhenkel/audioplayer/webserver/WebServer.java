package de.maxhenkel.audioplayer.webserver;

import de.maxhenkel.audioplayer.AudioPlayer;
import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WebServer implements AutoCloseable {

    @Nullable
    protected HttpServer server;
    @Nullable
    protected StaticFileCache staticFileCache;

    protected WebServer() {
    }

    public static WebServer create() {
        return new WebServer();
    }

    public WebServer start() throws Exception {
        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();

        UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
        mapper.register("/upload", new UploadHandler());
        mapper.register("*", new StaticContentHandler());

        staticFileCache = StaticFileCache.of("web");

        server = ServerBootstrap.bootstrap()
                .setListenerPort(AudioPlayer.WEB_SERVER_CONFIG.port.get())
                .setSocketConfig(socketConfig)
                .setHandlerMapper(mapper)
                .create();

        server.start();

        return this;
    }

    /**
     * @return the port the webserver is running on or <code>-1</code> if not running
     */
    public int getPort() {
        return server != null ? server.getLocalPort() : -1;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }

    private class StaticContentHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            if (!request.getRequestLine().getMethod().equalsIgnoreCase("GET")) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            String requestedResource = request.getRequestLine().getUri();

            if (requestedResource.equals("/")) {
                requestedResource = "/index.html";
            }

            byte[] data = staticFileCache.get(requestedResource);

            if (data == null) {
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                return;
            }
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new ByteArrayEntity(data));
        }
    }

    private class UploadHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            if (!request.getRequestLine().getMethod().equalsIgnoreCase("POST")) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            if (!(request instanceof HttpEntityEnclosingRequest enclosingRequest)) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            HttpEntity entity = enclosingRequest.getEntity();

            if (entity.getContentLength() > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
                response.setStatusCode(HttpStatus.SC_REQUEST_TOO_LONG);
                return;
            }

            InputStream inputStream = entity.getContent();

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byte[] bytes = bis.readAllBytes();

            //TODO Handle uploads

            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

}
