package com.ose.ai;

import com.ose.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiProviderCatalogService {

    private final AppProperties appProperties;

    public List<AiQuestionDtos.AiModelConfig> models(AiProviderType provider, String selectedDefaultModel) {
        String configuredDefault = selectedDefaultModel == null || selectedDefaultModel.isBlank()
                ? defaultModel(provider)
                : selectedDefaultModel;
        List<AiQuestionDtos.AiModelConfig> models = new ArrayList<>();
        for (String item : modelList(provider).split(",")) {
            String model = item.trim();
            if (!model.isBlank()) {
                models.add(new AiQuestionDtos.AiModelConfig(model, model, model.equals(configuredDefault)));
            }
        }
        if (models.stream().noneMatch(item -> item.model().equals(configuredDefault))) {
            models.add(0, new AiQuestionDtos.AiModelConfig(configuredDefault, configuredDefault, true));
        }
        return models;
    }

    public String defaultModel(AiProviderType provider) {
        return switch (provider) {
            case OPENAI -> appProperties.getAi().getOpenai().getDefaultModel();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getDefaultModel();
        };
    }

    public String defaultBaseUrl(AiProviderType provider) {
        return switch (provider) {
            case OPENAI -> appProperties.getAi().getOpenai().getBaseUrl();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getBaseUrl();
        };
    }

    public int defaultTimeoutMs() {
        return appProperties.getAi().getRequestTimeoutMs();
    }

    public int defaultMaxRetries() {
        return appProperties.getAi().getMaxRetries();
    }

    public double defaultTemperature(AiProviderType provider) {
        return switch (provider) {
            case OPENAI -> appProperties.getAi().getOpenai().getTemperature();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getTemperature();
        };
    }

    public int maxTokens(AiProviderType provider) {
        return switch (provider) {
            case OPENAI -> 0;
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getMaxTokens();
        };
    }

    public String envApiKey(AiProviderType provider) {
        return switch (provider) {
            case OPENAI -> appProperties.getAi().getOpenai().getApiKey();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getApiKey();
        };
    }

    private String modelList(AiProviderType provider) {
        return switch (provider) {
            case OPENAI -> appProperties.getAi().getOpenai().getModels();
            case ANTHROPIC -> appProperties.getAi().getAnthropic().getModels();
        };
    }
}
