package com.ose.note;

import com.ose.model.AppEnums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class NoteDtos {
    private NoteDtos() {
    }

    public record NoteLinkInput(@NotNull AppEnums.NoteLinkType linkType, @NotNull Long targetId) {
    }

    public record NoteLinkView(Long id, AppEnums.NoteLinkType linkType, Long targetId) {
    }

    public record NoteSaveRequest(
            @NotBlank(message = "请输入笔记标题") String title,
            @NotBlank(message = "请输入笔记内容") String content,
            String summary,
            Boolean favorite,
            List<NoteLinkInput> links
    ) {
    }

    public record NoteView(
            Long id,
            String title,
            String content,
            String summary,
            Boolean favorite,
            List<NoteLinkView> links,
            LocalDateTime updatedAt
    ) {
    }
}
