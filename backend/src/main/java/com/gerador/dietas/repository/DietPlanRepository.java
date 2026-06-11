package com.gerador.dietas.repository;

import com.gerador.dietas.domain.DietPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {

    List<DietPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<DietPlan> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);
}
