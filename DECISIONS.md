# Decisões de Implementação

Registro de decisões não especificadas explicitamente no documento de requisitos.

---

## Etapa 1

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Versão do Spring Boot | 3.4.1 | Versão estável mais recente da série 3.x |
| Biblioteca JWT | JJWT 0.12.6 | Biblioteca madura, amplamente usada no ecossistema Spring |
| Integração LLM | RestClient (Spring 3.2+) | Mais simples que WebClient; não exige WebFlux; suficiente para chamadas síncronas |
| Pacote base | `com.gerador.dietas` | Simples e descritivo |
| Testes na Etapa 1 | Sanity check sem contexto Spring | `@SpringBootTest` exige banco rodando; testes de unidade reais começam na Etapa 5; testes de integração precisam do banco via docker-compose |
| Versão do PostgreSQL (Docker) | 16-alpine | LTS mais recente, imagem leve |
