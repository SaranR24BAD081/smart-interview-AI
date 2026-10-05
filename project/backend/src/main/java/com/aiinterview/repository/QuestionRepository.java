package com.aiinterview.repository;

import com.aiinterview.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySessionIdOrderByIdAsc(Long sessionId);
}
