package com.gerador.dietas.repository;

import com.gerador.dietas.domain.LlmCallKind;
import com.gerador.dietas.domain.LlmUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface LlmUsageRepository extends JpaRepository<LlmUsage, Long> {

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);

    long countByUserIdAndKindAndCreatedAtAfter(Long userId, LlmCallKind kind, Instant after);
}
