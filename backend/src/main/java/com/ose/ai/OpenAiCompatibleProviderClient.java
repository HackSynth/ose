package com.ose.ai;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiCompatibleProviderClient implements AiProviderClient {

    private final OpenAiProviderClient delegate;

    public OpenAiCompatibleProviderClient(OpenAiProviderClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public AiProviderType providerType() {
        return AiProviderType.OPENAI_COMPATIBLE;
    }

    @Override
    public AiQuestionDtos.ProviderGenerationPayload generate(ResolvedAiProviderConfig config,
                                                             String model,
                                                             String systemPrompt,
                                                             String userPrompt,
                                                             String jsonSchema) {
        return delegate.generate(config, model, systemPrompt, userPrompt, jsonSchema);
    }

    @Override
    public AiProviderHealthResult testConnection(ResolvedAiProviderConfig config) {
        return delegate.testConnection(config);
    }

    @Override
    public List<AiProviderAdminDtos.CreateModelRequest> discoverModels(ResolvedAiProviderConfig config) {
        return delegate.discoverModels(config);
    }
}
