package com.gerador.dietas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "diet_plans")
public class DietPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Integer tmb;

    @Column(nullable = false)
    private Integer tdee;

    @Column(name = "target_calories", nullable = false)
    private Integer targetCalories;

    @Enumerated(EnumType.STRING)
    @Column(name = "formula_used", nullable = false, length = 30)
    private Formula formulaUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> content;

    @Column(name = "prompt_used", columnDefinition = "text")
    private String promptUsed;

    @Embedded
    private ProfileSnapshot profileSnapshot;

    @Column(name = "adjusted_at")
    private Instant adjustedAt;

    @Column(name = "adjustment_count", nullable = false)
    private int adjustmentCount;

    @Column(name = "last_adjustment", length = 500)
    private String lastAdjustment;

    protected DietPlan() {
    }

    public DietPlan(User user) {
        this.user = user;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getTmb() {
        return tmb;
    }

    public void setTmb(Integer tmb) {
        this.tmb = tmb;
    }

    public Integer getTdee() {
        return tdee;
    }

    public void setTdee(Integer tdee) {
        this.tdee = tdee;
    }

    public Integer getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(Integer targetCalories) {
        this.targetCalories = targetCalories;
    }

    public Formula getFormulaUsed() {
        return formulaUsed;
    }

    public void setFormulaUsed(Formula formulaUsed) {
        this.formulaUsed = formulaUsed;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public String getPromptUsed() {
        return promptUsed;
    }

    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }

    /** Nulo (ou vazio) em planos gerados antes da migration V3. */
    public ProfileSnapshot getProfileSnapshot() {
        if (profileSnapshot == null || profileSnapshot.isEmpty()) {
            return null;
        }
        return profileSnapshot;
    }

    public void setProfileSnapshot(ProfileSnapshot profileSnapshot) {
        this.profileSnapshot = profileSnapshot;
    }

    public Instant getAdjustedAt() {
        return adjustedAt;
    }

    public int getAdjustmentCount() {
        return adjustmentCount;
    }

    public String getLastAdjustment() {
        return lastAdjustment;
    }

    /** Registra um ajuste aplicado (chamado após a LLM revisar o plano). */
    public void recordAdjustment(String instruction) {
        this.adjustedAt = Instant.now();
        this.adjustmentCount += 1;
        this.lastAdjustment = instruction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DietPlan plan)) return false;
        return id != null && Objects.equals(id, plan.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
