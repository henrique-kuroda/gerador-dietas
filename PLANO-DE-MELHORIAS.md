# Plano de Melhorias — Gerador de Dietas

Análise do estado atual do projeto com correções, melhorias técnicas e um roadmap de
produto focado em **personalização profunda** — o diferencial que nenhum gerador de
dieta genérico do mercado entrega.

> Estado atual: MVP sólido e bem documentado (Java 21 + Spring Boot 3.4, React 19,
> Gemini, JWT, PDF, testes de metabolismo). A base está boa — este plano é sobre
> transformá-la em produto.

---

## 1. Correções (prioridade alta)

### 1.1 Modelo Gemini desatualizado
`application.yml` usa `gemini-1.5-flash` como default. A série Gemini 1.5 foi
descontinuada pelo Google — chamadas com chaves novas tendem a retornar 404.

- **Ação:** trocar o default para `gemini-2.5-flash` (ou `gemini-2.0-flash`) e
  validar que o `.env.example` documenta o `GEMINI_MODEL`.
- **Arquivo:** `backend/src/main/resources/application.yml:29`

### 1.2 API key na query string
`GeminiLlmService.callGemini()` monta a URL com `?key=...`. URLs vazam em logs de
proxy, stack traces e ferramentas de APM.

- **Ação:** enviar a chave no header `x-goog-api-key` em vez da query string.
- **Arquivo:** `backend/.../llm/GeminiLlmService.java:63-64`

### 1.3 Transação aberta durante a chamada à LLM
`DietService.generate()` é `@Transactional` e a chamada ao Gemini (até 60s + retries)
acontece dentro dela. Cada geração segura uma conexão do pool durante todo esse tempo —
com poucos usuários simultâneos o pool esgota. (Já registrado como dívida no
`DECISIONS.md`, hora de pagar.)

- **Ação:** quebrar em duas fases — ler perfil + calcular metabolismo (transação 1),
  chamar a LLM **fora** de transação, persistir o plano (transação 2).
- **Arquivo:** `backend/.../service/DietService.java:55-74`

### 1.4 Snapshot do perfil no DietPlan
O PDF (e o histórico) mostram o perfil **atual**, não o do momento da geração. Se o
usuário emagrece 10 kg e atualiza o perfil, todas as dietas antigas passam a exibir
dados incoerentes com as calorias calculadas na época.

- **Ação:** adicionar colunas de snapshot em `diet_plans` (peso, altura, idade, sexo,
  atividade, objetivo, restrições) via migration V3, preenchidas na geração. O PDF e o
  detalhe da dieta passam a usar o snapshot. Isso também habilita o gráfico de evolução
  (item 4.5) sem tabela extra.

### 1.5 Falha rápida com JWT_SECRET default
O `application.yml` tem um fallback de segredo JWT hard-coded. Em dev é cômodo, mas se
o app subir em produção sem a env var, todos os tokens são forjáveis.

- **Ação:** criar profile `prod` sem fallback (ou um `ApplicationRunner` que aborta o
  boot se o segredo for o default e o profile não for `dev`).

### 1.6 Rate limit na geração de dieta
`POST /api/diet/generate` não tem nenhum limite. Cada chamada custa dinheiro/quota da
LLM — um usuário (ou um script com um JWT válido) pode esgotar a quota em minutos.

- **Ação:** limite por usuário (ex.: 5 gerações/dia ou 1/minuto). Implementação simples:
  contar `diet_plans` do usuário nas últimas 24h antes de chamar a LLM e responder
  `429` com mensagem clara. Bucket4j é overkill nesse estágio.

---

## 2. Melhorias técnicas (backend)

### 2.1 Saída estruturada do Gemini (`responseSchema`)
Hoje o JSON é garantido só por prompt + `responseMimeType` + `stripCodeFences` +
parsing defensivo. A API do Gemini aceita `responseSchema` no `generationConfig`,
que **força** o formato na decodificação.

- **Ganho:** elimina quase todos os `INVALID_RESPONSE`, e o `stripCodeFences` vira
  redundância barata (manter como cinto de segurança).

### 2.2 Validação semântica do plano gerado
Validar além de "meals não vazio":
- soma das calorias dos itens ≈ caloria da refeição (tolerância ~10%);
- soma das refeições dentro da faixa min/max já enviada no prompt;
- nenhuma refeição > 40% do total (regra que o prompt já pede — verificar).

Se falhar, **uma** re-tentativa com o erro anexado ao prompt ("o plano anterior somou
X kcal, fora da faixa — corrija"). Isso é o que separa um wrapper de LLM de um produto
confiável.

### 2.3 Paginação no histórico
`GET /api/diet` devolve todos os planos com `content` completo. Com uso real isso
cresce rápido.

- **Ação:** `Pageable` + um `DietPlanSummaryResponse` (id, data, kcal, resumo) para a
  listagem; `content` completo só no `GET /api/diet/{id}`.

### 2.4 Refresh token
JWT expira em 1h e o usuário é deslogado no meio do uso. Adicionar refresh token
(httpOnly cookie, rotativo) + endpoint `/api/auth/refresh`. De quebra, reduz a
exposição do access token no `localStorage` (pode ficar só em memória).

### 2.5 Observabilidade e custo da LLM
- Spring Actuator (`/actuator/health`) — necessário para qualquer deploy.
- Logar `usageMetadata` da resposta do Gemini (tokens de entrada/saída) e persistir no
  `DietPlan` — visibilidade de custo por geração desde o dia 1.
- Métricas: contagem de gerações, taxa de `INVALID_RESPONSE`, latência da LLM.

### 2.6 Endpoints faltantes
- `GET /api/auth/me` — o front hoje decodifica o JWT na unha para obter o e-mail.
- `DELETE /api/diet/{id}` — usuário não consegue limpar o histórico.
- `DELETE /api/auth/me` — exclusão de conta (LGPD; o cascade no banco já está pronto).

---

## 3. Melhorias técnicas (frontend)

- **Error boundary global** + tela de erro amigável (hoje um throw em render quebra a SPA).
- **Skeleton loaders** nas páginas de histórico/detalhe em vez de spinner genérico.
- **`AbortController`/timeout visível na geração** — a geração pode levar 30s+; mostrar
  progresso por etapas ("Calculando metas… Montando cardápio…") reduz abandono.
- **Acessibilidade:** revisar contraste dos cinzas (`--color-ink-3` em 13px provavelmente
  falha WCAG AA), `aria-live` no estado de geração, foco visível.
- **PWA básico** (manifest + ícone): "instalar" no celular é onde uma dieta é consultada
  na prática — na cozinha e no mercado.
- **Testes:** Vitest + Testing Library para os fluxos críticos (login, guard de perfil,
  geração) — hoje o front tem zero testes.

---

## 4. Diferenciais de produto — personalização real

É aqui que o projeto deixa de ser "mais um wrapper de LLM". A arquitetura já ajuda:
`LlmService` é plugável, o prompt é template externo, o conteúdo é JSONB consultável.
Ordenado por relação valor/esforço:

### 4.1 Preferências alimentares estruturadas ⭐ (curto prazo)
Hoje só existe um campo livre `dietaryRestrictions`. Expandir o perfil com:

- **Não gosto / não como** (lista de alimentos) — diferente de restrição médica;
- **Gosto muito** (alimentos que devem aparecer com frequência);
- **Orçamento:** econômico / moderado / livre — muda drasticamente o cardápio
  (sardinha e ovo vs. salmão e castanhas);
- **Rotina:** almoça fora? leva marmita? cozinha à noite? tempo máximo de preparo;
- **Região do Brasil** — culinária regional (cuscuz nordestino vs. chimarrão e
  churrasco no sul) torna o plano *reconhecível* para o usuário.

Tudo vira seções novas no prompt. Esforço baixo, impacto enorme na percepção de
"feito para mim".

### 4.2 Trocar item/refeição ⭐ (curto prazo)
Botão "trocar" em cada item ou refeição do plano: chama a LLM com o contexto do plano
("substitua X por algo equivalente em kcal/macros, respeitando as preferências").
O plano atualizado é salvo por cima (ou versão nova).

- É a feature que transforma o app de "gerador" em "editor de dieta" — ninguém aceita
  um plano 100% no primeiro tiro.
- Endpoint: `POST /api/diet/{id}/swap` com `{ mealIndex, itemIndex? }`.

### 4.3 Feedback que alimenta a próxima geração ⭐ (médio prazo)
Após gerar, o usuário marca 👍/👎 por refeição ou item. Os feedbacks viram uma seção
do prompt nas próximas gerações: "o usuário rejeitou tapioca no café; aprovou omelete".

- Tabela `meal_feedback (user_id, food, signal, created_at)`.
- Esse loop de aprendizado por usuário é o diferencial competitivo real — o app
  **melhora com o uso**, coisa que nutricionista de PDF estático não faz.

### 4.4 Plano semanal + lista de compras (médio prazo)
- Gerar 7 dias com regra de variedade entre dias (proteína do almoço não repete 2 dias
  seguidos, etc.). Uma chamada por dia ou uma chamada com schema semanal — medir custo.
- **Lista de compras agregada** derivada do plano (agrupar itens, somar porções) —
  feature de altíssimo valor percebido e custo quase zero (é só agregação do JSONB).
- Exportar lista por texto/WhatsApp (share API no mobile).

### 4.5 Acompanhamento e TDEE adaptativo (médio prazo)
- Registro de peso periódico (`weight_logs`) + gráfico de evolução no dashboard.
- Com 3–4 semanas de dados, ajustar o TDEE pelo resultado real ("perdeu 0,2 kg/semana
  com 2.100 kcal → TDEE real ≈ X") em vez de só fórmula. Isso é o que apps caros
  (MacroFactor) cobram para fazer — e o projeto já tem TMB/TDEE/histórico para isso.

### 4.6 Validação nutricional com a tabela TACO (longo prazo, diferencial técnico)
Importar a **TACO** (Tabela Brasileira de Composição de Alimentos — pública) para o
banco e cruzar os itens gerados pela LLM com valores reais:

- corrigir/ajustar calorias e macros dos itens em vez de confiar na estimativa da LLM;
- exibir selo "valores conferidos" — credibilidade que nenhum gerador genérico tem;
- base para busca de substitutos por equivalência nutricional sem chamar a LLM.

### 4.7 Chat de ajuste fino (longo prazo)
"Deixa o jantar mais leve", "troca o almoço de domingo por feijoada" — refinamento
conversacional sobre o plano existente. A abstração `LlmService` aguenta; o que muda é
o prompt levar o plano atual + a instrução, devolvendo o JSON revisado.

### 4.8 Modo despensa (ideia para explorar)
Usuário lista o que tem em casa → o plano da semana prioriza esses ingredientes.
Combina com a lista de compras (4.4): compra só o que falta.

---

## 5. Qualidade e testes

| Lacuna | Ação |
|--------|------|
| Sem testes de integração da API | Testcontainers (PostgreSQL) + `@SpringBootTest` cobrindo auth, profile e geração (LLM mockada) |
| Sem teste do fluxo de segurança | Testes do filtro JWT: token expirado, assinatura inválida, acesso a recurso alheio |
| Sem testes de contrato do prompt | Snapshot test do `buildPrompt` — mudanças no template ficam visíveis no diff |
| Front sem testes | Vitest + Testing Library (item 3) |
| Sem CI | GitHub Actions: `mvn verify` + `npm run build` + lint em todo push |

---

## 6. DevOps / Deploy

- **`docker-compose.yml`:** remover `version: '3.8'` (obsoleto, gera warning).
- **Dockerfile multi-stage** para backend (build Maven → JRE 21 slim) e frontend
  (build Vite → nginx), com serviços no compose atrás de profile `full`.
- **CORS por variável de ambiente** (`ALLOWED_ORIGINS`) — hoje só localhost hard-coded
  em `SecurityConfig.java:70`.
- **Deploy alvo sugerido:** backend + Postgres no Railway/Render/Fly.io, front no
  Vercel/Cloudflare Pages. Custo ~zero para validar com usuários reais.

---

## 7. Roadmap sugerido

| Fase | Conteúdo | Resultado |
|------|----------|-----------|
| **1 — Fundação** (1–2 semanas) | Correções 1.1–1.6, responseSchema (2.1), validação semântica (2.2), `GET /me`, CI | App confiável e barato de operar |
| **2 — Personalização** (2–3 semanas) | Preferências estruturadas (4.1), trocar item (4.2), paginação, refresh token, DELETE dieta/conta | "Feito para mim" — primeiro diferencial visível |
| **3 — Hábito** (3–4 semanas) | Feedback loop (4.3), plano semanal + lista de compras (4.4), PWA, registro de peso (4.5) | Motivo para voltar toda semana |
| **4 — Autoridade** (contínuo) | TACO (4.6), TDEE adaptativo, chat de ajustes (4.7), modo despensa (4.8) | Diferencial técnico defensável |

---

## 8. O que **não** fazer agora

- **Microsserviços / filas / cache distribuído** — o monólito atual aguenta milhares
  de usuários; complexidade prematura.
- **App nativo** — PWA cobre o caso de uso (consultar dieta no celular).
- **Multi-idioma** — o contexto brasileiro no prompt é parte do diferencial; i18n
  diluiria isso.
- **Trocar de LLM por moda** — a abstração `LlmService` já permite trocar quando houver
  motivo (custo/qualidade), não antes.