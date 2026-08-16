package com.gerador.dietas.support;

import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.ProfileSnapshot;
import com.gerador.dietas.domain.Sex;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.llm.LlmService;
import com.gerador.dietas.repository.DietPlanRepository;
import com.gerador.dietas.repository.LlmUsageRepository;
import com.gerador.dietas.repository.ProfileRepository;
import com.gerador.dietas.repository.UserRepository;
import com.gerador.dietas.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

/**
 * Base dos testes de integração: PostgreSQL real (JSONB, Flyway e as constraints
 * de verdade) num container reaproveitado por todas as classes, e {@link LlmService}
 * sempre mockado — nenhum teste pode gastar quota do Gemini.
 *
 * <p>O container sobe uma única vez por JVM (bloco estático em vez de
 * {@code @Container}), então o contexto do Spring é reusado entre as classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    protected LlmService llmService;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ProfileRepository profileRepository;

    @Autowired
    protected DietPlanRepository dietPlanRepository;

    @Autowired
    protected LlmUsageRepository llmUsageRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparBase() {
        // llm_usage, profiles e diet_plans caem por cascade (FK ON DELETE CASCADE).
        dietPlanRepository.deleteAll();
        llmUsageRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User criarUsuario(String email) {
        return userRepository.save(new User(email, passwordEncoder.encode("senha-de-teste"), "Teste"));
    }

    protected Profile criarPerfil(User user) {
        Profile profile = new Profile(user);
        profile.setSex(Sex.MALE);
        profile.setAge(30);
        profile.setWeightKg(80.0);
        profile.setHeightCm(180.0);
        profile.setActivityLevel(ActivityLevel.MODERATE);
        profile.setGoal(Goal.MAINTAIN);
        profile.setMealsPerDay(4);
        return profileRepository.save(profile);
    }

    protected DietPlan criarPlano(User user) {
        DietPlan plan = new DietPlan(user);
        plan.setTmb(1800);
        plan.setTdee(2500);
        plan.setTargetCalories(2200);
        plan.setFormulaUsed(Formula.MIFFLIN_ST_JEOR);
        plan.setContent(conteudoDeExemplo());
        plan.setPromptUsed("prompt de teste");
        profileRepository.findByUserId(user.getId())
                .ifPresent(p -> plan.setProfileSnapshot(ProfileSnapshot.from(p)));
        return dietPlanRepository.save(plan);
    }

    protected String tokenDe(User user) {
        return "Bearer " + jwtService.generate(user.getId(), user.getEmail());
    }

    protected static Map<String, Object> conteudoDeExemplo() {
        return Map.of(
                "summary", "Plano de teste",
                "totalCalories", 2200,
                "meals", List.of(Map.of(
                        "name", "Café da manhã",
                        "calories", 2200,
                        "items", List.of(Map.of(
                                "food", "Ovos mexidos",
                                "portion", "3 unidades",
                                "calories", 2200)))),
                "macros", Map.of("proteinG", 150, "carbsG", 220, "fatG", 70));
    }
}
