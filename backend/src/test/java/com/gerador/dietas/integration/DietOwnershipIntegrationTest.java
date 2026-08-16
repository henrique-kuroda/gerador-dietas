package com.gerador.dietas.integration;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A garantia mais importante do sistema: dieta é dado de saúde e não pode vazar
 * entre contas. Hoje isso depende de uma única linha ({@code findByIdAndUserId})
 * em cada método do serviço — estes testes são a rede de proteção dela.
 *
 * <p>O 404 (em vez de 403) é proposital: o dono de outra conta não deve nem
 * descobrir que o id existe.
 */
class DietOwnershipIntegrationTest extends IntegrationTestBase {

    private User dono;
    private User intruso;
    private DietPlan plano;

    @BeforeEach
    void cenario() {
        dono = criarUsuario("dono@teste.com");
        criarPerfil(dono);
        plano = criarPlano(dono);
        intruso = criarUsuario("intruso@teste.com");
    }

    @Test
    @DisplayName("o dono lê a própria dieta (prova que o 404 alheio não é falso positivo)")
    void donoLeSuaPropriaDieta() throws Exception {
        mockMvc.perform(get("/api/diet/{id}", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(dono)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plano.getId()));
    }

    @Test
    @DisplayName("outro usuário não lê a dieta alheia")
    void intrusoNaoLeDietaAlheia() throws Exception {
        mockMvc.perform(get("/api/diet/{id}", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(intruso)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a dieta alheia não aparece no histórico do intruso")
    void historicoNaoVazaDietaAlheia() throws Exception {
        mockMvc.perform(get("/api/diet")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(intruso)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("outro usuário não ajusta a dieta alheia — e não gasta chamada de LLM")
    void intrusoNaoAjustaDietaAlheia() throws Exception {
        mockMvc.perform(post("/api/diet/{id}/adjust", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(intruso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"troque o café da manhã\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(llmService);
        assertThat(llmUsageRepository.count()).isZero();
    }

    @Test
    @DisplayName("outro usuário não apaga a dieta alheia")
    void intrusoNaoApagaDietaAlheia() throws Exception {
        mockMvc.perform(delete("/api/diet/{id}", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(intruso)))
                .andExpect(status().isNotFound());

        assertThat(dietPlanRepository.existsById(plano.getId())).isTrue();
    }

    @Test
    @DisplayName("outro usuário não baixa o PDF da dieta alheia")
    void intrusoNaoBaixaPdfAlheio() throws Exception {
        mockMvc.perform(get("/api/diet/{id}/pdf", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(intruso)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem token não se lê dieta nenhuma")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/diet/{id}", plano.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token assinado com outro segredo é rejeitado")
    void tokenForjadoRetorna401() throws Exception {
        String forjado = "Bearer eyJhbGciOiJIUzI1NiJ9."
                + "eyJzdWIiOiIxIiwiZW1haWwiOiJkb25vQHRlc3RlLmNvbSJ9.assinatura-invalida";

        mockMvc.perform(get("/api/diet/{id}", plano.getId())
                        .header(HttpHeaders.AUTHORIZATION, forjado))
                .andExpect(status().isUnauthorized());
    }
}
