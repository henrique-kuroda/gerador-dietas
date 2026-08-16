package com.gerador.dietas.security;

import com.gerador.dietas.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova que o throttle está mesmo na cadeia de filtros — e que conta uma vez por
 * requisição, mesmo com o filtro registrado no chain do Spring Security e no do
 * container. Um contador dobrado apareceria aqui como 429 antes da hora.
 */
class AuthRateLimitIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("login: bloqueia exatamente após o limite de falhas configurado")
    void loginBloqueiaAposOLimite() throws Exception {
        criarUsuario("throttle@teste.com");

        for (int i = 0; i < AuthRateLimitFilter.LOGIN_MAX_FAILURES; i++) {
            mockMvc.perform(loginComSenhaErrada())
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginComSenhaErrada())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Muitas tentativas de login")));
    }

    /**
     * IP próprio do teste: o filtro é um singleton do contexto compartilhado e o
     * bloqueio dura 15 minutos — não pode respingar em outras classes.
     */
    private MockHttpServletRequestBuilder loginComSenhaErrada() {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"throttle@teste.com\",\"password\":\"senha-errada\"}")
                .with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                });
    }
}
