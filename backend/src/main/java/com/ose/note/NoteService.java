package com.ose.note;

import com.ose.common.exception.NotFoundException;
import com.ose.model.Note;
import com.ose.model.NoteLink;
import com.ose.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    public List<NoteDtos.NoteView> list(String q) {
        List<Note> notes = (q == null || q.isBlank())
                ? noteRepository.findAll().stream().sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt())).toList()
                : noteRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(q, q);
        return notes.stream().map(this::toDto).toList();
    }

    @Transactional
    public NoteDtos.NoteView create(NoteDtos.NoteSaveRequest request) {
        Note note = new Note();
        apply(note, request);
        return toDto(noteRepository.save(note));
    }

    @Transactional
    public NoteDtos.NoteView update(Long id, NoteDtos.NoteSaveRequest request) {
        Note note = noteRepository.findById(id).orElseThrow(() -> new NotFoundException("笔记不存在"));
        apply(note, request);
        return toDto(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long id) {
        noteRepository.deleteById(id);
    }

    public List<Note> recentNotes() {
        return noteRepository.findTop10ByOrderByUpdatedAtDesc();
    }

    private void apply(Note note, NoteDtos.NoteSaveRequest request) {
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setSummary(request.summary() == null || request.summary().isBlank() ? summarize(request.content()) : request.summary());
        note.setFavorite(request.favorite() != null && request.favorite());
        note.getLinks().clear();
        if (request.links() != null) {
            request.links().forEach(link -> note.getLinks().add(NoteLink.builder()
                    .note(note)
                    .linkType(link.linkType())
                    .targetId(link.targetId())
                    .build()));
        }
    }

    private NoteDtos.NoteView toDto(Note note) {
        return new NoteDtos.NoteView(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getSummary(),
                note.getFavorite(),
                note.getLinks().stream().map(link -> new NoteDtos.NoteLinkView(link.getId(), link.getLinkType(), link.getTargetId())).toList(),
                note.getUpdatedAt()
        );
    }

    private String summarize(String content) {
        String plain = content.replace("#", " ").replace("*", " ").replace("`", " ").replaceAll("\\s+", " ").trim();
        return plain.length() > 100 ? plain.substring(0, 100) + "..." : plain;
    }
}
