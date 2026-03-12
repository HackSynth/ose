package com.ose.common.config;

import com.ose.ai.AiConfigMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private DefaultAdmin defaultAdmin = new DefaultAdmin();
    private Ai ai = new Ai();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expireMinutes;
    }

    @Getter
    @Setter
    public static class DefaultAdmin {
        private String username;
        private String password;
        private String displayName;
    }

    @Getter
    @Setter
    public static class Ai {
        private int requestTimeoutMs = 30000;
        private int maxRetries = 1;
        private boolean enableSaveReview = true;
        private AiConfigMode configMode = AiConfigMode.HYBRID;
        private String secretEncryptionKey;
        private Openai openai = new Openai();
        private Anthropic anthropic = new Anthropic();
    }

    @Getter
    @Setter
    public static class Openai {
        private String apiKey;
        private String baseUrl = "https://api.openai.com";
        private String defaultModel = "gpt-4.1-mini";
        private String models = "gpt-4.1-mini,gpt-4.1";
        private double temperature = 0.2d;
    }

    @Getter
    @Setter
    public static class Anthropic {
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com";
        private String defaultModel = "claude-3-5-sonnet-latest";
        private String models = "claude-3-5-sonnet-latest,claude-3-5-haiku-latest";
        private double temperature = 0.2d;
        private int maxTokens = 4000;
    }
}
