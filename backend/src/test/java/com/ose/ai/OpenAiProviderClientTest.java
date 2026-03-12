package com.ose.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ose.model.AppEnums;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiProviderClientTest {

    @Mock
    private AiProviderConfigurationResolver resolver;

    @Mock
    private AiProviderCatalogService catalogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OpenAiProviderClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        client = new OpenAiProviderClient(resolver, catalogService, objectMapper);
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
                respond(exchange, 500, "{\"error\":{\"message\":\"responses unavailable\"}}");
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

        when(resolver.resolve(AiProviderType.OPENAI)).thenReturn(resolvedConfig());

        AiQuestionDtos.ProviderGenerationPayload payload = client.generate(
                new AiQuestionDtos.AiQuestionGenerationRequest(
                        AiProviderType.OPENAI,
                        "gpt-4.1-mini",
                        AppEnums.QuestionType.MORNING_SINGLE,
                        AiQuestionDtos.AiQuestionTopicType.KNOWLEDGE_POINT,
                        List.of(1L),
                        AiQuestionDtos.AiQuestionDifficulty.MEDIUM,
                        1,
                        true,
                        true,
                        false,
                        "中文",
                        AiQuestionDtos.AiStyleType.EXAM,
                        null
                ),
                "system",
                "user",
                """
                {"type":"object"}
                """
        );

        assertEquals(AppEnums.QuestionType.MORNING_SINGLE, payload.questionType());
        assertEquals(1, payload.questions().size());
        assertEquals(1, responsesCalls.get());
        assertEquals(1, chatCalls.get());
    }

    @Test
    void shouldProbeGenerationCapabilityInsteadOfModelEndpoint() throws Exception {
        AtomicInteger responsesCalls = new AtomicInteger();
        AtomicInteger chatCalls = new AtomicInteger();
        startServer(exchange -> {
            if ("/v1/responses".equals(exchange.getRequestURI().getPath())) {
                responsesCalls.incrementAndGet();
                respond(exchange, 500, "{\"error\":{\"message\":\"responses unavailable\"}}");
                return;
            }
            if ("/v1/chat/completions".equals(exchange.getRequestURI().getPath())) {
                chatCalls.incrementAndGet();
                respond(exchange, 200, chatCompletionTextResponse("{\"ok\":true}"));
                return;
            }
            respond(exchange, 404, "{}");
        });

        AiProviderHealthResult result = client.testConnection(resolvedConfig());

        assertTrue(result.success());
        assertTrue(result.message().contains("chat.completions/json_schema"));
        assertEquals(1, responsesCalls.get());
        assertEquals(1, chatCalls.get());
    }

    private ResolvedAiProviderConfig resolvedConfig() {
        return new ResolvedAiProviderConfig(
                AiProviderType.OPENAI,
                true,
                true,
                "sk-test",
                "sk-***test",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "gpt-4.1-mini",
                3000,
                0,
                0.2d,
                0,
                AiProviderConfigSource.DB,
                "ok",
                List.of()
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

    private String chatCompletionTextResponse(String text) throws IOException {
        return objectMapper.writeValueAsString(
                java.util.Map.of(
                        "choices",
                        List.of(java.util.Map.of(
                                "message",
                                java.util.Map.of("content", text)
                        ))
                )
        );
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
