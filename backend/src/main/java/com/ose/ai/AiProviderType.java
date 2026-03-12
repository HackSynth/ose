package com.ose.ai;

public enum AiProviderType {
    OPENAI,
    ANTHROPIC,
    OPENAI_COMPATIBLE;

    public boolean supportsEnvFallback() {
        return this == OPENAI || this == ANTHROPIC;
    }

    public String defaultDisplayName() {
        return switch (this) {
            case OPENAI -> "OpenAI";
            case ANTHROPIC -> "Anthropic";
            case OPENAI_COMPATIBLE -> "OpenAI Compatible";
        };
    }
}
