package com.ose.ai;

import java.util.List;

public interface AiProviderClient {

    AiProviderType providerType();

    AiQuestionDtos.ProviderGenerationPayload generate(ResolvedAiProviderConfig config,
                                                      String model,
                                                      String systemPrompt,
                                                      String userPrompt,
                                                      String jsonSchema);

    AiProviderHealthResult testConnection(ResolvedAiProviderConfig config);

    default List<AiProviderAdminDtos.CreateModelRequest> discoverModels(ResolvedAiProviderConfig config) {
        return List.of();
    }
}
