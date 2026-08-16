package com.gerador.dietas.service;

import com.gerador.dietas.domain.LlmCallKind;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.exception.DietGenerationLimitException;
import com.gerador.dietas.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A cota é o que separa "app com usuários" de "conta do Gemini zerada": geração e
 * ajuste custam o mesmo e precisam sair do mesmo bolso.
 */
class LlmQuotaIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LlmQuotaService llmQuotaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User usuario;

    @BeforeEach
    void cenario() {
        usuario = criarUsuario("cota@teste.com");
        criarPerfil(usuario);
    }

    @Test
    @DisplayName("cada reserva vira uma linha em llm_usage")
    void reservaRegistraChamada() {
        llmQuotaService.reserve(usuario.getId(), LlmCallKind.GENERATE);

        assertThat(llmUsageRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("gerações param no sub-limite diário")
    void geracaoParaNoSubLimite() {
        for (int i = 0; i < LlmQuotaService.DAILY_GENERATION_LIMIT; i++) {
            llmQuotaService.reserve(usuario.getId(), LlmCallKind.GENERATE);
        }

        assertThatThrownBy(() -> llmQuotaService.reserve(usuario.getId(), LlmCallKind.GENERATE))
                .isInstanceOf(DietGenerationLimitException.class)
                .hasMessageContaining("gerações por dia");
    }

    @Test
    @DisplayName("ajuste continua liberado quando só o sub-limite de geração estourou")
    void ajusteNaoCaiNoSubLimiteDeGeracao() {
        for (int i = 0; i < LlmQuotaService.DAILY_GENERATION_LIMIT; i++) {
            llmQuotaService.reserve(usuario.getId(), LlmCallKind.GENERATE);
        }

        assertThatCode(() -> llmQuotaService.reserve(usuario.getId(), LlmCallKind.ADJUST))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ajustes entram na mesma cota diária e a esgotam")
    void ajusteEsgotaCotaUnificada() {
        for (int i = 0; i < LlmQuotaService.DAILY_CALL_LIMIT; i++) {
            llmQuotaService.reserve(usuario.getId(), LlmCallKind.ADJUST);
        }

        assertThatThrownBy(() -> llmQuotaService.reserve(usuario.getId(), LlmCallKind.ADJUST))
                .isInstanceOf(DietGenerationLimitException.class)
                .hasMessageContaining("chamadas à IA por dia");
    }

    @Test
    @DisplayName("chamadas fora da janela de 24h não contam")
    void chamadasAntigasNaoContam() {
        Instant ontem = Instant.now().minus(Duration.ofHours(25));
        for (int i = 0; i < LlmQuotaService.DAILY_CALL_LIMIT; i++) {
            jdbcTemplate.update(
                    "INSERT INTO llm_usage (user_id, kind, created_at) VALUES (?, ?, ?)",
                    usuario.getId(), LlmCallKind.GENERATE.name(), Timestamp.from(ontem));
        }

        assertThatCode(() -> llmQuotaService.reserve(usuario.getId(), LlmCallKind.GENERATE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a cota barra a geração antes de chamar a LLM")
    void geracaoComCotaEsgotadaRetorna429SemChamarLlm() throws Exception {
        for (int i = 0; i < LlmQuotaService.DAILY_CALL_LIMIT; i++) {
            llmQuotaService.reserve(usuario.getId(), LlmCallKind.ADJUST);
        }

        mockMvc.perform(post("/api/diet/generate")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(usuario)))
                .andExpect(status().isTooManyRequests());

        verifyNoInteractions(llmService);
    }

    @Test
    @DisplayName("a cota de um usuário não afeta a do outro")
    void cotaEhPorUsuario() {
        User outro = criarUsuario("outro@teste.com");
        for (int i = 0; i < LlmQuotaService.DAILY_CALL_LIMIT; i++) {
            llmQuotaService.reserve(usuario.getId(), LlmCallKind.ADJUST);
        }

        assertThatCode(() -> llmQuotaService.reserve(outro.getId(), LlmCallKind.GENERATE))
                .doesNotThrowAnyException();
    }
}
