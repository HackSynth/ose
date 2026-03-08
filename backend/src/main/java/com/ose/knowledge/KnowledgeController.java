package com.ose.knowledge;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-points")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/tree")
    public ApiResponse<List<KnowledgeDtos.KnowledgeTreeItem>> tree() {
        return ApiResponse.success(knowledgeService.tree());
    }

    @PostMapping
    public ApiResponse<KnowledgeDtos.KnowledgeTreeItem> create(@Valid @RequestBody KnowledgeDtos.KnowledgeRequest request) {
        return ApiResponse.success(knowledgeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeDtos.KnowledgeTreeItem> update(@PathVariable Long id,
                                                               @Valid @RequestBody KnowledgeDtos.KnowledgeRequest request) {
        return ApiResponse.success(knowledgeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
