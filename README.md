# Gerador de Dietas Personalizadas com IA

Aplicação web que coleta dados antropométricos do usuário, calcula seu gasto energético
(TMB/TDEE) e usa a API do Google Gemini para gerar um plano alimentar personalizado.

> **Aviso:** Este sistema gera sugestões educativas e **não substitui** a orientação de um
> nutricionista profissional. Consulte sempre um profissional de saúde antes de iniciar
> qualquer dieta.

---

## Stack

- **Back-end:** Java 21 + Spring Boot 3.4, Maven, PostgreSQL, Flyway, JWT
- **Front-end:** React 18 + TypeScript, Vite, Tailwind CSS, TanStack Query
- **LLM:** Google Gemini API

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose
- Node.js 20+ e npm
- Chave de API do Google Gemini (grátis em [ai.google.dev](https://ai.google.dev))

---

## Como rodar do zero

### 1. Clone o repositório e configure as variáveis de ambiente

```bash
git clone <url-do-repositorio>
cd gerador-de-dietas
cp .env.example .env
# Edite .env com suas credenciais (especialmente GEMINI_API_KEY e JWT_SECRET)
```

### 2. Suba o banco de dados

```bash
docker-compose up -d
```

O PostgreSQL ficará disponível em `localhost:5432`, banco `dietas`.

### 3. Inicie o back-end

```bash
cd backend

# Exporte as variáveis de ambiente (Linux/Mac)
export $(cat ../.env | xargs)

# Ou no Windows PowerShell:
# Get-Content ..\.env | ForEach-Object { if ($_ -match '^([^#][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2]) } }

mvn spring-boot:run
```

O back-end ficará disponível em `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Inicie o front-end

```bash
cd frontend
npm install
npm run dev
```

O front-end ficará disponível em `http://localhost:5173`.

---

## Executar testes

```bash
cd backend
mvn test
```

---

## Endpoints principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Cadastrar usuário |
| POST | `/api/auth/login` | Login (retorna JWT) |
| GET | `/api/profile` | Ver perfil |
| PUT | `/api/profile` | Criar/atualizar perfil |
| POST | `/api/diet/generate` | Gerar nova dieta |
| GET | `/api/diet` | Histórico de dietas |
| GET | `/api/diet/{id}` | Detalhe de uma dieta |
| GET | `/api/diet/{id}/pdf` | Exportar dieta em PDF |
