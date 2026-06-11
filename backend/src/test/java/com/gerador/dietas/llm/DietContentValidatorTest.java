package com.gerador.dietas.llm;

import com.gerador.dietas.llm.DietContent.Item;
import com.gerador.dietas.llm.DietContent.Macros;
import com.gerador.dietas.llm.DietContent.Meal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DietContentValidatorTest {

    private static final Macros MACROS = new Macros(150, 220, 70);

    @Test
    void plano_coerente_passa_sem_violacoes() {
        DietContent content = new DietContent("ok", 2000, List.of(
                meal("Café", 500, 250, 250),
                meal("Almoço", 700, 400, 300),
                meal("Jantar", 800, 500, 300)
        ), MACROS);

        assertThat(DietContentValidator.validate(content, 2000)).isEmpty();
    }

    @Test
    void acusa_soma_das_refeicoes_fora_da_faixa() {
        DietContent content = new DietContent("baixo", 1500, List.of(
                meal("Café", 400, 200, 200),
                meal("Almoço", 550, 300, 250),
                meal("Jantar", 550, 300, 250)
        ), MACROS);

        // soma 1500 vs target 2000 (faixa 1900-2100)
        assertThat(DietContentValidator.validate(content, 2000))
                .singleElement().asString().contains("fora da faixa");
    }

    @Test
    void acusa_itens_que_nao_batem_com_a_refeicao() {
        DietContent content = new DietContent("desvio", 2000, List.of(
                meal("Café", 500, 100, 100), // itens somam 200, declara 500
                meal("Almoço", 700, 400, 300),
                meal("Jantar", 800, 500, 300)
        ), MACROS);

        assertThat(DietContentValidator.validate(content, 2000))
                .singleElement().asString().contains("Café").contains("desvio");
    }

    @Test
    void acusa_refeicao_acima_de_40_por_cento_do_dia() {
        DietContent content = new DietContent("concentrado", 2000, List.of(
                meal("Café", 200, 100, 100),
                meal("Almoço", 900, 500, 400), // 45% de 2000
                meal("Jantar", 900, 500, 400)  // 45% de 2000
        ), MACROS);

        assertThat(DietContentValidator.validate(content, 2000))
                .hasSize(2)
                .allSatisfy(v -> assertThat(v).contains("máximo 40%"));
    }

    @Test
    void regra_dos_40_por_cento_nao_se_aplica_a_menos_de_3_refeicoes() {
        DietContent content = new DietContent("duas refeições", 2000, List.of(
                meal("Almoço", 1000, 600, 400),
                meal("Jantar", 1000, 600, 400)
        ), MACROS);

        assertThat(DietContentValidator.validate(content, 2000)).isEmpty();
    }

    @Test
    void acusa_refeicao_sem_itens() {
        DietContent content = new DietContent("vazia", 2000, List.of(
                meal("Café", 500, 250, 250),
                new Meal("Almoço", 700, List.of()),
                meal("Jantar", 800, 500, 300)
        ), MACROS);

        assertThat(DietContentValidator.validate(content, 2000))
                .singleElement().asString().contains("sem itens");
    }

    private static Meal meal(String name, int calories, int... itemCalories) {
        List<Item> items = java.util.Arrays.stream(itemCalories)
                .mapToObj(c -> new Item("alimento", "porção", c))
                .toList();
        return new Meal(name, calories, items);
    }
}
