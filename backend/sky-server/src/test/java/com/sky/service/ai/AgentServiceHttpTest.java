package com.sky.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.properties.AgentServiceProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentServiceHttpTest {
    private HttpServer server;
    private AgentServiceHttp client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        AgentServiceProperties properties = new AgentServiceProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setAuthToken("test-token");
        client = new AgentServiceHttp(properties, new ObjectMapper());
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsStatusAndConfirmationFromUserChat() {
        server.createContext("/v1/user/chat", exchange -> respond(exchange,
                "{\"session_id\":\"42\",\"answer\":\"please confirm\"," +
                        "\"status\":\"waiting_user\",\"trace_id\":\"trace-1\"," +
                        "\"confirmation\":{\"token\":\"token-123456\",\"action\":\"clear\"}}"));

        AgentServiceHttp.Result result = client.userChat("req-1", 1001L, 42L, "clear cart");

        assertEquals("waiting_user", result.getStatus());
        assertEquals("please confirm", result.getAnswer());
        assertEquals("clear", result.getConfirmation().get("action"));
    }

    @Test
    void resumesUserConfirmationOnBoundThread() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/v1/threads/42/resume", exchange -> {
            requestBody.set(readBody(exchange.getRequestBody()));
            respond(exchange,
                    "{\"session_id\":\"42\",\"answer\":\"cancelled\"," +
                            "\"status\":\"completed\",\"trace_id\":\"trace-2\"}");
        });

        AgentServiceHttp.Result result = client.resumeUser(
                "req-2", 1001L, 42L, "token-123456", "reject", null);

        assertEquals("completed", result.getStatus());
        assertEquals("cancelled", result.getAnswer());
        assertTrue(requestBody.get().contains("\"agent_name\":\"user_support_agent\""));
        assertTrue(requestBody.get().contains("\"decision\":\"reject\""));
    }

    private static void respond(HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String readBody(InputStream input) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
