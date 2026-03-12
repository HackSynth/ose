package com.ose.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.model.AppEnums;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiProviderClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OpenAiProviderClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        client = new OpenAiProviderClient(objectMapper, new AiProviderUrlBuilder());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldFallbackToChatCompletionsWhenResponsesApiUnavailable() throws Exception {
        AtomicInteger responsesCalls = new AtomicInteger();
        AtomicInteger chatCalls = new AtomicInteger();
        startServer(exchange -> {
            if ("/v1/responses".equals(exchange.getRequestURI().getPath())) {
                responsesCalls.incrementAndGet();
                respond(exchange, 404, "{\"error\":{\"message\":\"responses unavailable\"}}");
                return;
            }
            if ("/v1/chat/completions".equals(exchange.getRequestURI().getPath())) {
                chatCalls.incrementAndGet();
                respond(exchange, 200, chatCompletionTextResponse("""
                        {
                          "questionType":"MORNING_SINGLE",
                          "questions":[
                            {
                              "title":"事务题",
                              "content":"以下关于事务隔离级别的说法，正确的是？",
                              "options":[
                                {"key":"A","content":"读未提交可避免脏读"},
                                {"key":"B","content":"可重复读总能避免幻读"},
                                {"key":"C","content":"串行化隔离级别最高"},
                                {"key":"D","content":"读已提交会出现脏写"}
                              ],
                              "correctAnswer":"C",
                              "explanation":"串行化提供最高隔离级别。",
                              "knowledgePointNames":["事务管理"],
                              "difficulty":"MEDIUM"
                            }
                          ]
                        }
                        """));
                return;
            }
            respond(exchange, 404, "{}");
        });

        AiQuestionDtos.ProviderGenerationPayload payload = client.generate(
                resolvedConfig(serverBaseUrl(), AiBaseUrlMode.ROOT),
                "gpt-4.1-mini",
                "system",
                "user",
                "{\"type\":\"object\"}"
        );

        assertEquals(AppEnums.QuestionType.MORNING_SINGLE, payload.questionType());
        assertEquals(2, responsesCalls.get());
        assertEquals(1, chatCalls.get());
    }

    @Test
    void shouldUseFullOverrideAddressWithoutAutoAppendingPath() throws Exception {
        AtomicInteger fullOverrideCalls = new AtomicInteger();
        startServer(exchange -> {
            if ("/gateway/chat".equals(exchange.getRequestURI().getPath())) {
                fullOverrideCalls.incrementAndGet();
                respond(exchange, 200, chatCompletionTextResponse("{\"ok\":true}"));
                return;
            }
            respond(exchange, 404, "{}");
        });

        AiProviderHealthResult result = client.testConnection(resolvedConfig(serverBaseUrl() + "/gateway/chat", AiBaseUrlMode.FULL_OVERRIDE));

        assertTrue(result.success());
        assertEquals(1, fullOverrideCalls.get());
    }

    @Test
    void shouldDiscoverModelsFromModelsEndpoint() throws Exception {
        startServer(exchange -> {
            if ("/v1/models".equals(exchange.getRequestURI().getPath())) {
                respond(exchange, 200, """
                        {
                          "data":[
                            {"id":"gpt-4.1-mini"},
                            {"id":"gpt-4.1"}
                          ]
                        }
                        """);
                return;
            }
            respond(exchange, 404, "{}");
        });

        List<AiProviderAdminDtos.CreateModelRequest> models = client.discoverModels(resolvedConfig(serverBaseUrl(), AiBaseUrlMode.ROOT));

        assertEquals(2, models.size());
        assertEquals("gpt-4.1-mini", models.get(0).modelId());
    }

    private ResolvedAiProviderConfig resolvedConfig(String baseUrl, AiBaseUrlMode mode) {
        return new ResolvedAiProviderConfig(
                "provider-openai",
                AiProviderType.OPENAI,
                "OpenAI",
                true,
                true,
                new ResolvedAiApiKey("key-1", "sk-secret-1234", "sk-***1234"),
                baseUrl,
                mode,
                "gpt-4.1-mini",
                1000,
                0,
                0.2d,
                0,
                AiProviderConfigSource.DB,
                "ok"
        );
    }

    private void startServer(ThrowingHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private String serverBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String chatCompletionTextResponse(String content) {
        return """
                {
                  "choices":[
                    {
                      "message":{
                        "content":%s
                      }
                    }
                  ]
                }
                """.formatted(objectMapper.valueToTree(content));
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
