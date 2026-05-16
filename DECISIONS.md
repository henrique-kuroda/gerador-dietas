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

## Etapa 2

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Tabela `users` | nome plural `users` | `user` é palavra reservada do PostgreSQL |
| Tipo do PK | `BIGSERIAL` / `Long` + `IDENTITY` | Suficiente para o escopo; evita sequence manual |
| Coluna `DietPlan.content` | `JSONB` mapeado para `Map<String, Object>` via `@JdbcTypeCode(SqlTypes.JSON)` | Hibernate 6.2+ tem suporte nativo; evita serializar JSON manualmente; permite consultas JSON futuras |
| Relação `User`–`Profile` | 1:1 com `profiles.user_id` UNIQUE + FK | Simples; cascade `ALL` + `orphanRemoval` para excluir perfil junto |
| Relação `User`–`DietPlan` | 1:N com `diet_plans.user_id` FK + `ON DELETE CASCADE` | Histórico do usuário é descartado junto |
| Validações no banco | `CHECK` constraints em `profiles` e `diet_plans` | Defesa em profundidade — back-end também valida (Bean Validation) |
| Timestamps | `TIMESTAMPTZ` (PostgreSQL) ↔ `Instant` (Java) | UTC sempre; evita confusão de fuso |
| Índice em `diet_plans` | `(user_id, created_at DESC)` | Consulta principal é "histórico do usuário ordenado por data" |
| `ddl-auto` | `validate` (já configurado) | Garante que migration e mapping JPA fiquem em sincronia |

## Etapa 3

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Estratégia de sessão | Stateless (sem `HttpSession`) | API REST + JWT; servidor não guarda estado |
| Algoritmo de hash de senha | BCrypt (padrão Spring) | Adaptativo, work-factor configurável; padrão da indústria |
| Algoritmo JWT | HS256 (HMAC-SHA256) via segredo simétrico | Suficiente p/ MVP de servidor único; sem necessidade de chave assimétrica |
| Claim do JWT | `sub` = userId, claim extra `email` | `sub` numérico evita lookup por string e dispensa atualização quando o email muda |
| Filtro JWT | `OncePerRequestFilter` antes do `UsernamePasswordAuthenticationFilter` | Padrão; popula o `SecurityContext` e deixa o restante das regras agirem normalmente |
| `AuthenticationEntryPoint` | `HttpStatusEntryPoint(401)` | 401 puro; corpo de erro padronizado é responsabilidade do `GlobalExceptionHandler` quando o erro nasce no controller |
| Normalização de e-mail | `trim().toLowerCase()` antes de salvar/consultar | Evita duplicatas por diferença de caixa |
| Princípal do Spring | `AppUserPrincipal` (UserDetails customizado) | Expõe `id` do `User` direto no principal; facilita pegar o dono nos controllers das próximas etapas |
| CORS | Apenas `http://localhost:5173` (Vite default) | Origem do front-end em dev; ajustar em produção |
| Liberados sem auth | `/api/auth/**`, Swagger UI e OpenAPI | Necessário para registro/login e exploração da API |
