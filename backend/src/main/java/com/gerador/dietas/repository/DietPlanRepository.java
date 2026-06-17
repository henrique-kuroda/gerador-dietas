package com.gerador.dietas.repository;

import com.gerador.dietas.domain.DietPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {

    Page<DietPlan> findByUserId(Long userId, Pageable pageable);

    Optional<DietPlan> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);
}
