package com.ose.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class KnowledgeDtos {
    private KnowledgeDtos() {
    }

    public record KnowledgeTreeItem(
            Long id,
            String code,
            String name,
            Integer level,
            Integer masteryLevel,
            Integer weight,
            String note,
            Long parentId,
            List<KnowledgeTreeItem> children
    ) {
    }

    public record KnowledgeRequest(
            @NotBlank(message = "请输入知识点编码") String code,
            @NotBlank(message = "请输入知识点名称") String name,
            @NotNull(message = "请输入层级") @Min(value = 1) @Max(value = 3) Integer level,
            @NotNull(message = "请输入掌握度") @Min(value = 0) @Max(value = 100) Integer masteryLevel,
            @NotNull(message = "请输入权重") @Min(value = 1) Integer weight,
            String note,
            Integer sortOrder,
            Long parentId
    ) {
    }
}
