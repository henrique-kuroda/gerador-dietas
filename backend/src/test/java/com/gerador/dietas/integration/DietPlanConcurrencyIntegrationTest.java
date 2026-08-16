package com.gerador.dietas.integration;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O ajuste lê o plano, chama a LLM (30s+) e grava fora de transação. Sem a coluna
 * {@code version}, dois ajustes simultâneos gravavam por cima um do outro: um sumia
 * e o {@code adjustment_count} ficava menor que o número de chamadas pagas feitas.
 */
class DietPlanConcurrencyIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("gravação com versão obsoleta falha em vez de sobrescrever em silêncio")
    void gravacaoConcorrenteFalhaComVersaoObsoleta() {
        User dono = criarUsuario("concorrencia@teste.com");
        Long planoId = criarPlano(dono).getId();

        // Duas leituras independentes, como duas requisições de ajuste em paralelo.
        DietPlan primeiro = dietPlanRepository.findById(planoId).orElseThrow();
        DietPlan segundo = dietPlanRepository.findById(planoId).orElseThrow();

        primeiro.recordAdjustment("deixe o jantar mais leve");
        dietPlanRepository.save(primeiro);

        segundo.recordAdjustment("troque o frango por peixe");
        assertThatThrownBy(() -> dietPlanRepository.save(segundo))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("o ajuste que ganha a corrida é preservado por inteiro")
    void ajusteVencedorPermanece() {
        User dono = criarUsuario("vencedor@teste.com");
        Long planoId = criarPlano(dono).getId();

        DietPlan primeiro = dietPlanRepository.findById(planoId).orElseThrow();
        DietPlan segundo = dietPlanRepository.findById(planoId).orElseThrow();

        primeiro.recordAdjustment("deixe o jantar mais leve");
        dietPlanRepository.save(primeiro);

        segundo.recordAdjustment("troque o frango por peixe");
        assertThatThrownBy(() -> dietPlanRepository.save(segundo))
                .isInstanceOf(OptimisticLockingFailureException.class);

        DietPlan persistido = dietPlanRepository.findById(planoId).orElseThrow();
        assertThat(persistido.getAdjustmentCount()).isEqualTo(1);
        assertThat(persistido.getLastAdjustment()).isEqualTo("deixe o jantar mais leve");
    }
}
