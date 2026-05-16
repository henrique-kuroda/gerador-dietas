package com.gerador.dietas.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.domain.ActivityLevel;
import com.gerador.dietas.domain.Formula;
import com.gerador.dietas.domain.Goal;
import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.Sex;
import com.gerador.dietas.metabolism.MetabolismResult;
import com.gerador.dietas.support.ProfileFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DietGeneratorTest {

    private static final String VALID_JSON = """
            {
              "summary": "Plano de 2200 kcal para ganho de massa, 4 refeições.",
              "totalCalories": 2200,
              "meals": [
                {
                  "name": "Café da manhã",
                  "calories": 550,
                  "items": [
                    { "food": "Ovos mexidos", "portion": "3 unidades", "calories": 230 },
                    { "food": "Pão integral", "portion": "2 fatias", "calories": 160 }
                  ]
                }
              ],
              "macros": { "proteinG": 150, "carbsG": 220, "fatG": 70 }
            }
            """;

    private StubLlmService llm;
    private DietGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        llm = new StubLlmService();
        Resource template = new ByteArrayResource("""
                Sexo: {sex} | Idade: {age} | Peso: {weightKg} kg | Altura: {heightCm} cm
                Atividade: {activityLevel} | Objetivo: {goal}
                Restrições: {dietaryRestrictions}
                Refeições: {mealsPerDay}
                Calorias-alvo: {targetCalories} kcal
                """.getBytes(StandardCharsets.UTF_8));
        generator = new DietGenerator(llm, new ObjectMapper(), template);
    }

    @Test
    void monta_prompt_substituindo_todos_os_placeholders() {
        Profile profile = ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.GAIN_MUSCLE, 4, "sem lactose", null);
        MetabolismResult metabolism = new MetabolismResult(1780, 2759, 3090, Formula.MIFFLIN_ST_JEOR);

        String prompt = generator.buildPrompt(profile, metabolism);

        assertThat(prompt).contains("Sexo: MALE");
        assertThat(prompt).contains("Idade: 30");
        assertThat(prompt).contains("Peso: 80 kg");
        assertThat(prompt).contains("Altura: 180 cm");
        assertThat(prompt).contains("Atividade: MODERATE");
        assertThat(prompt).contains("Objetivo: GAIN_MUSCLE");
        assertThat(prompt).contains("Restrições: sem lactose");
        assertThat(prompt).contains("Refeições: 4");
        assertThat(prompt).contains("Calorias-alvo: 3090 kcal");
        assertThat(prompt).doesNotContain("{");
    }

    @Test
    void substitui_restricoes_em_branco_por_nenhuma() {
        Profile profile = ProfileFixtures.of(Sex.FEMALE, 28, 60, 165,
                ActivityLevel.LIGHT, Goal.MAINTAIN, 3, null, null);
        MetabolismResult metabolism = new MetabolismResult(1330, 1828, 1828, Formula.MIFFLIN_ST_JEOR);

        String prompt = generator.buildPrompt(profile, metabolism);

        assertThat(prompt).contains("Restrições: nenhuma");
    }

    @Test
    void gera_dieta_parseando_json_limpo() {
        llm.responseBody = VALID_JSON;
        Profile profile = padraoMale();
        MetabolismResult metabolism = new MetabolismResult(1780, 2759, 2207, Formula.MIFFLIN_ST_JEOR);

        DietGeneratorResult result = generator.generate(profile, metabolism);

        assertThat(result.content().totalCalories()).isEqualTo(2200);
        assertThat(result.content().meals()).hasSize(1);
        assertThat(result.content().meals().get(0).name()).isEqualTo("Café da manhã");
        assertThat(result.content().macros().proteinG()).isEqualTo(150);
        assertThat(result.prompt()).contains("Calorias-alvo: 2207");
    }

    @Test
    void remove_cerca_de_codigo_json_ao_parsear() {
        llm.responseBody = "```json\n" + VALID_JSON + "\n```";

        DietGeneratorResult result = generator.generate(padraoMale(),
                new MetabolismResult(1780, 2759, 2207, Formula.MIFFLIN_ST_JEOR));

        assertThat(result.content().totalCalories()).isEqualTo(2200);
    }

    @Test
    void remove_cerca_de_codigo_generica_ao_parsear() {
        llm.responseBody = "```\n" + VALID_JSON + "\n```";

        DietGeneratorResult result = generator.generate(padraoMale(),
                new MetabolismResult(1780, 2759, 2207, Formula.MIFFLIN_ST_JEOR));

        assertThat(result.content().totalCalories()).isEqualTo(2200);
    }

    @Test
    void falha_quando_resposta_nao_e_json_valido() {
        llm.responseBody = "isso não é JSON nenhum";

        assertThatThrownBy(() -> generator.generate(padraoMale(),
                new MetabolismResult(1780, 2759, 2207, Formula.MIFFLIN_ST_JEOR)))
                .isInstanceOf(LlmException.class)
                .extracting(ex -> ((LlmException) ex).getKind())
                .isEqualTo(LlmException.Kind.INVALID_RESPONSE);
    }

    @Test
    void falha_quando_plano_vem_sem_refeicoes() {
        llm.responseBody = """
                { "summary": "vazio", "totalCalories": 0, "meals": [], "macros": { "proteinG": 0, "carbsG": 0, "fatG": 0 } }
                """;

        assertThatThrownBy(() -> generator.generate(padraoMale(),
                new MetabolismResult(1780, 2759, 2207, Formula.MIFFLIN_ST_JEOR)))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("refeições");
    }

    @Test
    void strip_code_fences_quando_nao_ha_cerca_retorna_original_trimmed() {
        assertThat(DietGenerator.stripCodeFences("  {\"a\":1}  ")).isEqualTo("{\"a\":1}");
    }

    private Profile padraoMale() {
        return ProfileFixtures.of(Sex.MALE, 30, 80, 180,
                ActivityLevel.MODERATE, Goal.LOSE_WEIGHT, 4, "sem lactose", null);
    }

    private static final class StubLlmService implements LlmService {
        String responseBody = "";

        @Override
        public String generateJson(String prompt) {
            return responseBody;
        }
    }
}
