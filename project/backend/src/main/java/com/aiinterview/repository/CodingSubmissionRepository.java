package com.aiinterview.repository;

import com.aiinterview.model.CodingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {
    List<CodingSubmission> findBySessionIdOrderByIdAsc(Long sessionId);
}
