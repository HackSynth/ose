package com.ose.ai;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/providers")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;
    private final AiProviderHealthService healthService;
    private final AiModelRegistryService modelRegistryService;
    private final AiProviderConfigurationResolver resolver;
    private final List<AiProviderClient> providerClients;

    @GetMapping
    public ApiResponse<List<AiProviderAdminDtos.ProviderDetail>> listProviders() {
        return ApiResponse.success(providerService.listProviders());
    }

    @PostMapping
    public ApiResponse<AiProviderAdminDtos.ProviderDetail> create(@Valid @RequestBody AiProviderAdminDtos.CreateProviderRequest request) {
        return ApiResponse.success(providerService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiProviderAdminDtos.ProviderDetail> update(@PathVariable String id,
                                                                  @Valid @RequestBody AiProviderAdminDtos.UpdateProviderRequest request) {
        return ApiResponse.success(providerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        providerService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<AiProviderAdminDtos.ProviderDetail> enable(@PathVariable String id) {
        return ApiResponse.success(providerService.enable(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<AiProviderAdminDtos.ProviderDetail> disable(@PathVariable String id) {
        return ApiResponse.success(providerService.disable(id));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<AiProviderAdminDtos.ProviderTestResponse> test(@PathVariable String id) {
        AiProviderHealthResult result = healthService.test(id);
        ResolvedAiProviderConfig config = resolver.resolve(id);
        return ApiResponse.success(new AiProviderAdminDtos.ProviderTestResponse(
                result.success(),
                id,
                config.providerType(),
                result.model(),
                result.latencyMs(),
                result.message(),
                result.configSource()
        ));
    }

    @PostMapping("/{id}/keys")
    public ApiResponse<AiProviderAdminDtos.ApiKeySummary> addKey(@PathVariable String id,
                                                                 @Valid @RequestBody AiProviderAdminDtos.CreateApiKeyRequest request) {
        return ApiResponse.success(providerService.addKey(id, request));
    }

    @PutMapping("/{id}/keys/{keyId}")
    public ApiResponse<AiProviderAdminDtos.ApiKeySummary> updateKey(@PathVariable String id,
                                                                    @PathVariable String keyId,
                                                                    @Valid @RequestBody AiProviderAdminDtos.UpdateApiKeyRequest request) {
        return ApiResponse.success(providerService.updateKey(id, keyId, request));
    }

    @DeleteMapping("/{id}/keys/{keyId}")
    public ApiResponse<Void> deleteKey(@PathVariable String id, @PathVariable String keyId) {
        providerService.deleteKey(id, keyId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/models")
    public ApiResponse<List<AiProviderAdminDtos.ModelDetail>> listModels(@PathVariable String id) {
        return ApiResponse.success(modelRegistryService.list(id));
    }

    @PostMapping("/{id}/models")
    public ApiResponse<AiProviderAdminDtos.ModelDetail> createModel(@PathVariable String id,
                                                                    @Valid @RequestBody AiProviderAdminDtos.CreateModelRequest request) {
        return ApiResponse.success(modelRegistryService.create(id, request));
    }

    @PutMapping("/{id}/models/{modelId}")
    public ApiResponse<AiProviderAdminDtos.ModelDetail> updateModel(@PathVariable String id,
                                                                    @PathVariable String modelId,
                                                                    @Valid @RequestBody AiProviderAdminDtos.UpdateModelRequest request) {
        return ApiResponse.success(modelRegistryService.update(id, modelId, request));
    }

    @DeleteMapping("/{id}/models/{modelId}")
    public ApiResponse<Void> deleteModel(@PathVariable String id, @PathVariable String modelId) {
        modelRegistryService.delete(id, modelId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/models/discover")
    public ApiResponse<AiProviderAdminDtos.DiscoverModelsResponse> discoverModels(@PathVariable String id) {
        ResolvedAiProviderConfig config = resolver.resolve(id);
        AiProviderClient client = providerClients.stream()
                .filter(item -> item.providerType() == config.providerType())
                .findFirst()
                .orElseThrow(() -> new AiProviderException("未找到对应的 AI Provider Client"));
        List<AiProviderAdminDtos.ModelDetail> created = client.discoverModels(config).stream()
                .map(request -> modelRegistryService.create(id, request))
                .toList();
        return ApiResponse.success(new AiProviderAdminDtos.DiscoverModelsResponse(
                true,
                created.isEmpty() ? "未发现新模型" : "已同步 " + created.size() + " 个模型",
                created
        ));
    }
}
