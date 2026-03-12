package com.ose.ai;

import com.ose.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class AiProviderUrlBuilder {

    public String build(ResolvedAiProviderConfig config, Endpoint endpoint) {
        String baseUrl = trimTrailingSlash(config.baseUrl());
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException("Provider Base URL 未配置");
        }
        if (config.baseUrlMode() == AiBaseUrlMode.FULL_OVERRIDE) {
            return baseUrl;
        }
        return baseUrl + endpoint.path(config.providerType());
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public enum Endpoint {
        CHAT_COMPLETIONS {
            @Override
            String path(AiProviderType providerType) {
                return "/v1/chat/completions";
            }
        },
        RESPONSES {
            @Override
            String path(AiProviderType providerType) {
                return "/v1/responses";
            }
        },
        MODELS {
            @Override
            String path(AiProviderType providerType) {
                return "/v1/models";
            }
        },
        ANTHROPIC_MESSAGES {
            @Override
            String path(AiProviderType providerType) {
                return "/v1/messages";
            }
        };

        abstract String path(AiProviderType providerType);
    }
}
