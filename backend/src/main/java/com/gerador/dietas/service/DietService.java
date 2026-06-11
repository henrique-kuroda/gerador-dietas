package com.gerador.dietas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.ProfileSnapshot;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.exception.DietPlanNotFoundException;
import com.gerador.dietas.exception.ProfileIncompleteException;
import com.gerador.dietas.llm.DietGenerator;
import com.gerador.dietas.llm.DietGeneratorResult;
import com.gerador.dietas.metabolism.MetabolismResult;
import com.gerador.dietas.metabolism.MetabolismService;
import com.gerador.dietas.repository.DietPlanRepository;
import com.gerador.dietas.repository.ProfileRepository;
import com.gerador.dietas.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DietService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

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
    public List<DietPlan> listForUser(Long userId) {
        return dietPlanRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public DietPlan getOwned(Long userId, Long dietPlanId) {
        return dietPlanRepository.findByIdAndUserId(dietPlanId, userId)
                .orElseThrow(() -> new DietPlanNotFoundException(
                        "Dieta " + dietPlanId + " não encontrada."));
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
