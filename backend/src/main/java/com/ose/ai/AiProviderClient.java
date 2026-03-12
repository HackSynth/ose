package com.ose.ai;

import java.util.List;

public interface AiProviderClient {

    AiProviderType provider();

    boolean isConfigured();

    List<AiQuestionDtos.AiModelConfig> models();

    AiQuestionDtos.ProviderGenerationPayload generate(AiQuestionDtos.AiQuestionGenerationRequest request,
                                                      String systemPrompt,
                                                      String userPrompt,
                                                      String jsonSchema);

    AiProviderHealthResult testConnection(ResolvedAiProviderConfig config);
}
