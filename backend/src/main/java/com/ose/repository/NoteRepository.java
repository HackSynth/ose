package com.ose.repository;

import com.ose.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByUpdatedAtDesc(String title, String content);

    List<Note> findTop10ByOrderByUpdatedAtDesc();

    Optional<Note> findFirstByTitleIgnoreCase(String title);
}
