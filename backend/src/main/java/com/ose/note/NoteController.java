package com.ose.note;

import com.ose.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ApiResponse<List<NoteDtos.NoteView>> list(@RequestParam(required = false) String q) {
        return ApiResponse.success(noteService.list(q));
    }

    @PostMapping
    public ApiResponse<NoteDtos.NoteView> create(@Valid @RequestBody NoteDtos.NoteSaveRequest request) {
        return ApiResponse.success(noteService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteDtos.NoteView> update(@PathVariable Long id,
                                                 @Valid @RequestBody NoteDtos.NoteSaveRequest request) {
        return ApiResponse.success(noteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noteService.delete(id);
        return ApiResponse.successMessage("删除成功");
    }
}
