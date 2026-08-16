package com.gerador.dietas.integration;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O teto de ajustes por plano é do back-end; o front só o exibe. Estes testes
 * seguram as duas pontas: a resposta carrega o valor e o servidor o aplica.
 */
class DietAdjustLimitIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("a resposta carrega o teto e o saldo de ajustes")
    void respostaCarregaTetoESaldo() throws Exception {
        User dono = criarUsuario("teto@teste.com");
        DietPlan plano = criarPlano(dono);

        mockMvc.perform(get("/api/diet/{id}", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(dono)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjustmentCount").value(0))
                .andExpect(jsonPath("$.adjustmentsRemaining").value(DietPlan.MAX_ADJUSTMENTS))
                .andExpect(jsonPath("$.maxAdjustments").value(DietPlan.MAX_ADJUSTMENTS));
    }

    @Test
    @DisplayName("o saldo cai a cada ajuste aplicado")
    void saldoCaiACadaAjuste() throws Exception {
        User dono = criarUsuario("saldo@teste.com");
        DietPlan plano = criarPlano(dono);
        plano.recordAdjustment("troque o frango por peixe");
        dietPlanRepository.save(plano);

        mockMvc.perform(get("/api/diet/{id}", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(dono)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjustmentCount").value(1))
                .andExpect(jsonPath("$.adjustmentsRemaining").value(DietPlan.MAX_ADJUSTMENTS - 1));
    }

    @Test
    @DisplayName("plano no teto recusa novo ajuste sem gastar chamada de LLM")
    void planoNoTetoRecusaNovoAjuste() throws Exception {
        User dono = criarUsuario("estourado@teste.com");
        DietPlan plano = criarPlano(dono);
        for (int i = 0; i < DietPlan.MAX_ADJUSTMENTS; i++) {
            plano.recordAdjustment("ajuste " + i);
            plano = dietPlanRepository.save(plano);
        }

        mockMvc.perform(post("/api/diet/{id}/adjust", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"mais um ajuste\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(
                        "Limite de " + DietPlan.MAX_ADJUSTMENTS + " ajustes para este plano atingido."));

        verifyNoInteractions(llmService);
        // O teto por plano é checado antes da reserva: cota diária não é consumida.
        assertThat(llmUsageRepository.count()).isZero();
    }
}
