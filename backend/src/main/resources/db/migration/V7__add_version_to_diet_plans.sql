-- V7: Optimistic locking em diet_plans. O ajuste lê o plano, chama a LLM (30s+)
-- e grava fora de transação; sem version, dois ajustes simultâneos se sobrescrevem
-- e o adjustment_count fica menor que o número de chamadas realmente feitas.

ALTER TABLE diet_plans
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
