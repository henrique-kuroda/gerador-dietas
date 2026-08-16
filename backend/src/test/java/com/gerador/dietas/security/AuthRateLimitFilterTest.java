package com.gerador.dietas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private MutableClock clock;
    private AuthRateLimitFilter filter;
    private AtomicInteger chamadasAoChain;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
        filter = new AuthRateLimitFilter(objectMapperComoNoApp(), clock);
        chamadasAoChain = new AtomicInteger();
    }

    @Test
    @DisplayName("login: falhas seguidas do mesmo IP acabam bloqueadas com 429")
    void loginBloqueiaAposLimiteDeFalhas() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LOGIN_MAX_FAILURES; i++) {
            assertThat(executar(loginRequest("10.0.0.1"), HttpStatus.UNAUTHORIZED).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        MockHttpServletResponse bloqueada = executar(loginRequest("10.0.0.1"), HttpStatus.UNAUTHORIZED);

        assertThat(bloqueada.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(bloqueada.getHeader(HttpHeaders.RETRY_AFTER))
                .isEqualTo(String.valueOf(AuthRateLimitFilter.LOGIN_WINDOW.toSeconds()));
        assertThat(chamadasAoChain).hasValue(AuthRateLimitFilter.LOGIN_MAX_FAILURES);
    }

    @Test
    @DisplayName("login: quem acerta a senha nunca e barrado")
    void loginBemSucedidoNaoConta() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LOGIN_MAX_FAILURES * 3; i++) {
            assertThat(executar(loginRequest("10.0.0.1"), HttpStatus.OK).getStatus())
                    .isEqualTo(HttpStatus.OK.value());
        }
    }

    @Test
    @DisplayName("o bloqueio e por IP, nao global")
    void bloqueioEhPorIp() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LOGIN_MAX_FAILURES; i++) {
            executar(loginRequest("10.0.0.1"), HttpStatus.UNAUTHORIZED);
        }

        assertThat(executar(loginRequest("10.0.0.2"), HttpStatus.UNAUTHORIZED).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("passada a janela, o contador zera")
    void janelaExpiraELiberaNovamente() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LOGIN_MAX_FAILURES; i++) {
            executar(loginRequest("10.0.0.1"), HttpStatus.UNAUTHORIZED);
        }
        assertThat(executar(loginRequest("10.0.0.1"), HttpStatus.UNAUTHORIZED).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        clock.avancar(AuthRateLimitFilter.LOGIN_WINDOW.plusMinutes(1));

        assertThat(executar(loginRequest("10.0.0.1"), HttpStatus.UNAUTHORIZED).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("cadastro: toda tentativa conta, inclusive as bem-sucedidas")
    void cadastroContaTodasAsTentativas() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.REGISTER_MAX_ATTEMPTS; i++) {
            assertThat(executar(registerRequest("10.0.0.1"), HttpStatus.CREATED).getStatus())
                    .isEqualTo(HttpStatus.CREATED.value());
        }

        assertThat(executar(registerRequest("10.0.0.1"), HttpStatus.CREATED).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("endpoints fora do auth passam direto")
    void outrosEndpointsNaoSaoFiltrados() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/diet/generate");
        request.setRemoteAddr("10.0.0.1");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("GET no caminho de login nao e filtrado (so POST autentica)")
    void apenasPostEhFiltrado() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", AuthRateLimitFilter.LOGIN_PATH);
        request.setRemoteAddr("10.0.0.1");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    private MockHttpServletResponse executar(MockHttpServletRequest request, HttpStatus statusDoChain)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            chamadasAoChain.incrementAndGet();
            ((MockHttpServletResponse) res).setStatus(statusDoChain.value());
        };
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletRequest loginRequest(String ip) {
        return requestPara(AuthRateLimitFilter.LOGIN_PATH, ip);
    }

    private MockHttpServletRequest registerRequest(String ip) {
        return requestPara(AuthRateLimitFilter.REGISTER_PATH, ip);
    }

    /** O ObjectMapper do app tem o modulo de datas; o corpo do 429 carrega um Instant. */
    private static ObjectMapper objectMapperComoNoApp() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private MockHttpServletRequest requestPara(String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return request;
    }

    /** Relogio controlado: o teste de janela nao pode depender de tempo real. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void avancar(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
