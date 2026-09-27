package com.nyx.bridge;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class LocalBridgeServer {

    public static final int PORT = 8765;
    private static final String TAG = "NYXBridge";

    private volatile boolean running;
    private volatile String status = "starting";
    private ServerSocket serverSocket;
    private Thread serverThread;

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        status = "starting";
        serverThread = new Thread(() -> {
            try {
                InetAddress loopback = InetAddress.getByName("127.0.0.1");
                serverSocket = new ServerSocket(PORT, 20, loopback);
                status = "listening on 127.0.0.1:" + PORT;
                Log.i(TAG, status);

                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        handle(socket);
                    } catch (IOException e) {
                        if (!running) {
                            break;
                        }
                        status = "accept error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                        Log.e(TAG, status, e);
                    }
                }
            } catch (IOException e) {
                running = false;
                status = "startup error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                Log.e(TAG, status, e);
            }
        }, "nyx-bridge-http");

        serverThread.start();
    }

    public String getStatus() {
        return status;
    }

    public synchronized void stop() {
        running = false;
        status = "stopped";

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }

        serverThread = null;
    }

    private void handle(Socket socket) {
        try (Socket client = socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             OutputStream output = client.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }

            String[] parts = requestLine.split(" ");
            String path = parts.length >= 2 ? parts[1] : "/";

            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                // Headers are intentionally ignored for this minimal bridge.
            }

            String body;
            int statusCode;
            String statusText;

            if ("/ping".equals(path)) {
                body = "{\"status\":\"ok\",\"response\":\"pong\",\"bridge_version\":\"0.2\",\"transport\":\"localhost_http\"}";
                statusCode = 200;
                statusText = "OK";
            } else if ("/device_info".equals(path)) {
                body = "{"
                        + "\"status\":\"ok\","
                        + "\"manufacturer\":\"" + escape(Build.MANUFACTURER) + "\","
                        + "\"model\":\"" + escape(Build.MODEL) + "\","
                        + "\"sdk_int\":" + Build.VERSION.SDK_INT + ","
                        + "\"release\":\"" + escape(Build.VERSION.RELEASE) + "\","
                        + "\"package\":\"com.nyx.bridge\","
                        + "\"bridge_version\":\"0.2\","
                        + "\"transport\":\"localhost_http\""
                        + "}";
                statusCode = 200;
                statusText = "OK";
            } else {
                body = "{\"status\":\"error\",\"error\":\"unknown_path\"}";
                statusCode = 404;
                statusText = "Not Found";
            }

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                    + "Content-Type: application/json; charset=utf-8\r\n"
                    + "Content-Length: " + bytes.length + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";

            output.write(response.getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.flush();
        } catch (IOException e) {
            Log.e(TAG, "request error: " + e.getMessage(), e);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
