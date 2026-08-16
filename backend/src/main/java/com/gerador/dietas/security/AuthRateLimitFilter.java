package com.gerador.dietas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerador.dietas.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttle por IP nos dois endpoints públicos que custam caro quando abusados:
 * {@code /api/auth/login} (força bruta de senha) e {@code /api/auth/register}
 * (criação de contas em massa — cada conta nova ganha uma cota diária de LLM).
 *
 * <p>Janela fixa em memória: o app roda em instância única e o próprio
 * {@code DECISIONS.md} já descartou Bucket4j/Redis neste estágio. Com mais de uma
 * instância, cada uma passa a ter seu próprio contador — trocar por um store
 * compartilhado é a evolução natural.
 *
 * <p>No login só a <b>falha</b> conta: quem acerta a senha nunca é barrado. No
 * cadastro toda tentativa conta, porque o custo é a conta criada.
 *
 * <p>A identificação usa {@code request.getRemoteAddr()}. Atrás de proxy/CDN,
 * defina {@code server.forward-headers-strategy} para o container reescrever o IP
 * de origem — confiar em {@code X-Forwarded-For} sem proxy confiável na frente
 * deixaria o limite trivial de burlar.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    static final String LOGIN_PATH = "/api/auth/login";
    static final String REGISTER_PATH = "/api/auth/register";

    static final int LOGIN_MAX_FAILURES = 10;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    static final int REGISTER_MAX_ATTEMPTS = 5;
    static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    /** Teto de chaves rastreadas; acima disso as janelas expiradas são varridas. */
    static final int MAX_TRACKED_CLIENTS = 20_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    AuthRateLimitFilter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return ruleFor(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Rule rule = ruleFor(request);
        String key = rule.name() + "|" + request.getRemoteAddr();

        if (isBlocked(key, rule)) {
            log.warn("Rate limit atingido em {} para {}", rule.name(), request.getRemoteAddr());
            writeTooManyRequests(response, rule);
            return;
        }

        if (rule.countsEveryAttempt()) {
            registerHit(key, rule);
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, response);
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            registerHit(key, rule);
        }
    }

    private Rule ruleFor(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (LOGIN_PATH.equals(path)) {
            return new Rule("login", LOGIN_MAX_FAILURES, LOGIN_WINDOW, false);
        }
        if (REGISTER_PATH.equals(path)) {
            return new Rule("register", REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW, true);
        }
        return null;
    }

    private boolean isBlocked(String key, Rule rule) {
        Window window = windows.get(key);
        return window != null
                && !isExpired(window, rule, clock.instant())
                && window.count() >= rule.maxAttempts();
    }

    private void registerHit(String key, Rule rule) {
        Instant now = clock.instant();
        if (windows.size() >= MAX_TRACKED_CLIENTS) {
            sweepExpired(now);
        }
        windows.compute(key, (ignored, current) ->
                (current == null || isExpired(current, rule, now))
                        ? new Window(now, 1)
                        : new Window(current.start(), current.count() + 1));
    }

    private boolean isExpired(Window window, Rule rule, Instant now) {
        return now.isAfter(window.start().plus(rule.window()));
    }

    /** Varredura preguiçosa: sem scheduler, o mapa se limpa quando cresce demais. */
    private void sweepExpired(Instant now) {
        Duration longest = LOGIN_WINDOW.compareTo(REGISTER_WINDOW) > 0 ? LOGIN_WINDOW : REGISTER_WINDOW;
        windows.values().removeIf(window -> now.isAfter(window.start().plus(longest)));
    }

    private void writeTooManyRequests(HttpServletResponse response, Rule rule) throws IOException {
        long retryAfter = rule.window().toSeconds();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                rule.blockedMessage()));
    }

    private record Window(Instant start, int count) {
    }

    private record Rule(String name, int maxAttempts, Duration window, boolean countsEveryAttempt) {

        String blockedMessage() {
            return "login".equals(name)
                    ? "Muitas tentativas de login. Aguarde " + window.toMinutes() + " minutos e tente novamente."
                    : "Muitos cadastros a partir deste endereço. Tente novamente mais tarde.";
        }
    }
}
