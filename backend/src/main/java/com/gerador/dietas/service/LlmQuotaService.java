package com.gerador.dietas.service;

import com.gerador.dietas.domain.LlmCallKind;
import com.gerador.dietas.domain.LlmUsage;
import com.gerador.dietas.exception.DietGenerationLimitException;
import com.gerador.dietas.repository.LlmUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Cota diária de chamadas à LLM por usuário. Geração e ajuste custam o mesmo e
 * saem da mesma cota — contar só {@code diet_plans} deixaria o ajuste ilimitado.
 *
 * <p>A chamada é <b>reservada antes</b> de ir à LLM: uma tentativa que falha já
 * consumiu quota do provedor, então também consome a do usuário. Erro de
 * configuração nossa não chega a reservar (estoura antes, no {@code LlmService}).
 */
@Service
public class LlmQuotaService {

    /** Teto de chamadas pagas (geração + ajuste) por usuário em 24h. */
    static final int DAILY_CALL_LIMIT = 15;

    /** Sub-teto de gerações em 24h: plano novo é o uso mais caro em contexto. */
    static final int DAILY_GENERATION_LIMIT = 5;

    static final Duration WINDOW = Duration.ofHours(24);

    private final LlmUsageRepository llmUsageRepository;

    public LlmQuotaService(LlmUsageRepository llmUsageRepository) {
        this.llmUsageRepository = llmUsageRepository;
    }

    /**
     * Verifica a cota e registra a chamada. Transação curta e própria: roda antes
     * da ida à LLM, que acontece fora de qualquer transação.
     *
     * @throws DietGenerationLimitException quando o usuário estourou a cota (HTTP 429)
     */
    @Transactional
    public void reserve(Long userId, LlmCallKind kind) {
        Instant since = Instant.now().minus(WINDOW);

        if (kind == LlmCallKind.GENERATE) {
            long generations = llmUsageRepository.countByUserIdAndKindAndCreatedAtAfter(
                    userId, LlmCallKind.GENERATE, since);
            if (generations >= DAILY_GENERATION_LIMIT) {
                throw new DietGenerationLimitException(
                        "Limite de " + DAILY_GENERATION_LIMIT + " gerações por dia atingido. "
                                + "Tente novamente em algumas horas.");
            }
        }

        long calls = llmUsageRepository.countByUserIdAndCreatedAtAfter(userId, since);
        if (calls >= DAILY_CALL_LIMIT) {
            throw new DietGenerationLimitException(
                    "Limite de " + DAILY_CALL_LIMIT + " chamadas à IA por dia (gerações e "
                            + "ajustes somados) atingido. Tente novamente em algumas horas.");
        }

        llmUsageRepository.save(new LlmUsage(userId, kind));
    }
}
