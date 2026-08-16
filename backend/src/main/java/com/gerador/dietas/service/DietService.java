package com.gerador.dietas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.ProfileSnapshot;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.exception.DietGenerationLimitException;
import com.gerador.dietas.exception.DietPlanNotFoundException;
import com.gerador.dietas.exception.ProfileIncompleteException;
import com.gerador.dietas.llm.DietContent;
import com.gerador.dietas.llm.DietGenerator;
import com.gerador.dietas.llm.DietGeneratorResult;
import com.gerador.dietas.metabolism.MetabolismResult;
import com.gerador.dietas.metabolism.MetabolismService;
import com.gerador.dietas.repository.DietPlanRepository;
import com.gerador.dietas.repository.ProfileRepository;
import com.gerador.dietas.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class DietService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    // Cada geração custa quota/dinheiro na LLM; limite simples por usuário
    // contando planos das últimas 24h (Bucket4j seria overkill neste estágio).
    static final int DAILY_GENERATION_LIMIT = 5;

    // Cada ajuste também é uma chamada paga à LLM; teto por plano barra abuso sem
    // precisar de contagem por tempo.
    static final int MAX_ADJUSTMENTS_PER_PLAN = 10;

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final DietPlanRepository dietPlanRepository;
    private final MetabolismService metabolismService;
    private final DietGenerator dietGenerator;
    private final DietPdfService dietPdfService;
    private final ObjectMapper objectMapper;

    public DietService(ProfileRepository profileRepository,
                       UserRepository userRepository,
                       DietPlanRepository dietPlanRepository,
                       MetabolismService metabolismService,
                       DietGenerator dietGenerator,
                       DietPdfService dietPdfService,
                       ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.dietPlanRepository = dietPlanRepository;
        this.metabolismService = metabolismService;
        this.dietGenerator = dietGenerator;
        this.dietPdfService = dietPdfService;
        this.objectMapper = objectMapper;
    }

    // Sem @Transactional de método: a chamada à LLM leva até 60s + retries e não pode
    // segurar uma conexão do pool. Leitura e persistência usam as transações curtas
    // dos próprios repositórios.
    public DietPlan generate(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileIncompleteException(
                        "Complete seu perfil em PUT /api/profile antes de gerar uma dieta."));

        long generatedLast24h = dietPlanRepository.countByUserIdAndCreatedAtAfter(
                userId, Instant.now().minus(Duration.ofHours(24)));
        if (generatedLast24h >= DAILY_GENERATION_LIMIT) {
            throw new DietGenerationLimitException(
                    "Limite de " + DAILY_GENERATION_LIMIT + " gerações por dia atingido. "
                            + "Tente novamente em algumas horas.");
        }

        MetabolismResult metabolism = metabolismService.calculate(profile);

        // Fora de qualquer transação: pode demorar.
        DietGeneratorResult generated = dietGenerator.generate(profile, metabolism);

        User user = userRepository.getReferenceById(userId);
        DietPlan plan = new DietPlan(user);
        plan.setProfileSnapshot(ProfileSnapshot.from(profile));
        plan.setTmb(metabolism.tmb());
        plan.setTdee(metabolism.tdee());
        plan.setTargetCalories(metabolism.targetCalories());
        plan.setFormulaUsed(metabolism.formulaUsed());
        plan.setContent(objectMapper.convertValue(generated.content(), MAP_TYPE));
        plan.setPromptUsed(generated.prompt());

        return dietPlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public Page<DietPlan> listForUser(Long userId, Pageable pageable) {
        return dietPlanRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public DietPlan getOwned(Long userId, Long dietPlanId) {
        return dietPlanRepository.findByIdAndUserId(dietPlanId, userId)
                .orElseThrow(() -> new DietPlanNotFoundException(
                        "Dieta " + dietPlanId + " não encontrada."));
    }

    // Sem @Transactional de método: igual a generate(), a chamada à LLM fica fora de transação.
    public DietPlan adjust(Long userId, Long dietPlanId, String instruction) {
        DietPlan plan = dietPlanRepository.findByIdAndUserId(dietPlanId, userId)
                .orElseThrow(() -> new DietPlanNotFoundException(
                        "Dieta " + dietPlanId + " não encontrada."));

        if (plan.getAdjustmentCount() >= MAX_ADJUSTMENTS_PER_PLAN) {
            throw new DietGenerationLimitException(
                    "Limite de " + MAX_ADJUSTMENTS_PER_PLAN + " ajustes para este plano atingido.");
        }

        // Preferências/restrições atuais do usuário (fallback: sem perfil → null tratado no prompt).
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        DietContent current = objectMapper.convertValue(plan.getContent(), DietContent.class);

        // Fora de transação: pode demorar.
        DietGeneratorResult adjusted = dietGenerator.adjust(
                current, plan.getTargetCalories(), profile, instruction);

        plan.setContent(objectMapper.convertValue(adjusted.content(), MAP_TYPE));
        plan.setPromptUsed(adjusted.prompt());
        plan.recordAdjustment(instruction);
        return dietPlanRepository.save(plan);
    }

    @Transactional
    public void delete(Long userId, Long dietPlanId) {
        DietPlan plan = dietPlanRepository.findByIdAndUserId(dietPlanId, userId)
                .orElseThrow(() -> new DietPlanNotFoundException(
                        "Dieta " + dietPlanId + " não encontrada."));
        dietPlanRepository.delete(plan);
    }

    @Transactional(readOnly = true)
    public byte[] renderPdf(Long userId, Long dietPlanId) {
        DietPlan plan = getOwned(userId, dietPlanId);
        // Planos anteriores à V3 não têm snapshot — caímos no perfil atual; se o
        // usuário também apagou o perfil, o PDF sai só com os dados calculados.
        ProfileSnapshot snapshot = plan.getProfileSnapshot();
        if (snapshot == null) {
            snapshot = profileRepository.findByUserId(userId)
                    .map(ProfileSnapshot::from)
                    .orElse(null);
        }
        return dietPdfService.render(plan, snapshot);
    }
}
