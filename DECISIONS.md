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

## Etapa 4

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| `PUT /api/profile` faz upsert | Mesmo endpoint cria e atualiza | Spec lista só `PUT`; perfil é 1:1 com usuário, não há semântica de "criar várias vezes" |
| `GET /api/profile` sem perfil | 404 + mensagem instrutiva | Mais claro que devolver objeto vazio; o front sabe que precisa redirecionar para a tela de perfil |
| Identificação do dono | `@AuthenticationPrincipal AppUserPrincipal` | Não precisa de helper extra nem de buscar usuário a cada request |
| Construtor público `Profile(User)` | Necessário porque `ProfileService` está em outro pacote | Mantém o `protected Profile()` para Hibernate e centraliza a obrigatoriedade do `User` |
| `dietaryRestrictions` em branco vira `null` | Normalização no service | Evita strings vazias no banco e simplifica a lógica do `DietGenerator` (Etapa 6) |
| Validação de enums inválidos no JSON | Handler para `HttpMessageNotReadableException` extraindo `InvalidFormatException` | Bean Validation não captura enums fora do range — o erro nasce no Jackson antes |

## Etapa 5

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Estratégia das fórmulas | Cada implementação `@Component` agregada num `EnumMap<Formula, MetabolicFormula>` | Adicionar nova fórmula = criar a classe; o service indexa por enum em vez de `if/else` |
| `MetabolicFormula.type()` | Método na interface além de `calculateBmr` | Permite ao service indexar sem refletir; nome explícito > anotação custom |
| Arredondamento de TMB/TDEE/alvo | `Math.round` para `int` apenas no resultado final | Mantém precisão dos cálculos intermediários; o `DietPlan` armazena `Integer` (kcal sem casas) |
| Fator de atividade e multiplicador de objetivo | Campo dentro dos enums `ActivityLevel`/`Goal` | Os números são parte do domínio, não de configuração externa; evita switch espalhado |
| Construtor público `Profile()` | Necessário para fixtures de teste em outro pacote | Hibernate continua feliz; mantém a entidade testável sem reflection |
| Test fixture | Helper estático `ProfileFixtures` (apenas em src/test/java) | Centraliza criação de `Profile` nos testes; não vaza para o código de produção |
| Asserts numéricos | AssertJ `isCloseTo(..., within(0.x))` | Trata aproximação de doubles sem `assertEquals(delta)` repetitivo |

## Etapa 6

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Abstração da LLM | `LlmService.generateJson(prompt)` retornando texto bruto | Mantém `DietGenerator` agnóstico do provedor; trocar Gemini por Groq/OpenRouter exige só nova impl |
| Template de prompt | Arquivo `resources/prompts/diet-prompt.txt` carregado no constructor do `DietGenerator` | Spec proíbe strings concatenadas; arquivo é mais fácil de editar e revisar que constante Java |
| Formato da resposta | `responseMimeType: application/json` na chamada Gemini | A própria API força JSON estruturado; reduz necessidade de limpeza |
| Parsing defensivo | `stripCodeFences` remove ``` ```json``` / ``` ``` ``` antes do `ObjectMapper` | LLM eventualmente envolve o JSON em cerca markdown; falhar nisso seria desperdiçar a chamada |
| Retry | Loop manual, 3 tentativas (500ms/1500ms/3000ms), só p/ `UNAVAILABLE`/`RATE_LIMITED` | Suficiente p/ MVP; evita pull de Spring Retry; erros de configuração não são retentados |
| Tipos de erro da LLM | Enum `LlmException.Kind` (UNAVAILABLE, RATE_LIMITED, INVALID_RESPONSE, CONFIGURATION) | Mapping limpo para HTTP no handler global (502/503/500) |
| Cliente HTTP | `RestClient` (decisão Etapa 1) com timeouts 10s/60s | Geração pode ser lenta (LLM); leitura curta cortaria respostas legítimas |
| Validação de plano vazio | `meals` vazio → `INVALID_RESPONSE` | Plano sem refeições não tem utilidade; melhor falhar e permitir nova tentativa |
| Fixture compartilhada | `support/ProfileFixtures` (pacote `support` em test) | Reuso entre testes de `metabolism` e `llm` sem dependência cruzada |
| Records `DietContent.*` com `@JsonIgnoreProperties(ignoreUnknown = true)` | Tolerar campos extras da LLM | LLMs ocasionalmente acrescentam chaves; desserialização não deve quebrar por isso |

## Etapa 7

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| `POST /api/diet/generate` sem corpo | Lê só o JWT; gera com base no perfil persistido | Spec só pede para usar dados do perfil; passar o perfil no corpo duplicaria a fonte de verdade |
| Perfil ausente ao gerar dieta | `ProfileIncompleteException` → HTTP 409 | A8 sugere 409/400; 409 (Conflict) descreve melhor "estado incompleto, complete antes" |
| Conversão `DietContent` → `Map<String, Object>` | `ObjectMapper.convertValue(content, MAP_TYPE)` | Sem serializar/desserializar para string; alimenta o mapping `@JdbcTypeCode(JSON)` direto |
| `DietPlan(User)` construtor público | Mesmo padrão de `Profile(User)` | `DietService` está em outro pacote; mantém o `protected DietPlan()` para Hibernate |
| Acesso a dieta de outro usuário | `findByIdAndUserId` → 404 `DietPlanNotFoundException` | A8 permite 403/404; 404 não vaza existência de recursos alheios |
| Listagem em `GET /api/diet` | Mesmo `DietPlanResponse` da geração (inclui `content`) | MVP com poucos planos por usuário; manter um único DTO é mais simples que summary separado |
| Persistência do `promptUsed` | Sempre salvar o prompt que foi enviado | A2 lista `promptUsed` para depuração; barato e útil para diagnosticar respostas ruins |
| Transação que envolve a LLM | `@Transactional` no `generate` (cobre leitura do perfil + persistência final) | A transação fica aberta durante a chamada à LLM — aceitável no MVP, único usuário por vez; revisitar com pool real |

## Etapa 8

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Versão do React | 19 (scaffold padrão do Vite atual) | Spec pede "React 18+"; o scaffold já entrega 19 e a API que usamos é igual; evitar downgrade manual |
| Tailwind | v4 via `@tailwindcss/vite` + `@import "tailwindcss"` no CSS | Setup oficial atual; dispensa `tailwind.config.js` para o conjunto padrão |
| Estado do servidor | TanStack Query (`useQuery`/`useMutation`) | Cache, dedupe, refetch e estados de loading sem reinventar |
| Storage do JWT | `localStorage` (`diet.jwt`) lido por interceptor Axios | MVP single-page; suficiente. Aceita o risco XSS conhecido — apropriado para escopo educacional |
| Logout em 401 | Interceptor de resposta limpa o token e redireciona para `/login` | Evita ficar tentando requests com token expirado em todas as queries |
| Auth context | Deriva user do payload JWT (`email`) sem call extra | Token já carrega o `email`; evita um `GET /me` que não existe |
| Decoder JWT | `atob` + JSON.parse com try/catch | Não precisamos validar assinatura no front (servidor já valida); só queremos o claim |
| Rotas privadas | `<ProtectedRoute>` + rota pai com `<AppLayout/>` e `<Outlet/>` | Layout compartilhado por todas as telas autenticadas, sem repetição |
| Validação de formulários | React Hook Form + Zod (`zodResolver`) | Spec exige a stack; `z.coerce.number` cuida do `<input type="number">` que devolve string |
| Tipagem do form com `z.coerce` | `useForm<z.input, unknown, z.output>` | RHF separa tipo de entrada (string) e saída (number após coerção); usar `z.infer` quebra a inferência do resolver |
| `bodyFatPercent` opcional | aceita `""` no schema e converte para `null` antes de enviar | Input vazio vira string; back-end espera `null` para significar "ausente" → fórmula Mifflin |
| Disclaimer | Componente `<Disclaimer/>` exibido em Dashboard, Histórico e Detalhe da dieta | Spec exige visível; entra em todas as telas que mostram/geram dieta |
| Página inicial de novo usuário | Após cadastro o login é automático e redireciona para `/profile` | Evita usuário cair no dashboard sem perfil; o botão "Gerar" ficaria desabilitado de qualquer jeito |
