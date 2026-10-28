# RepoMind — GitHub Repository AI Assistant

A Java and React application for indexing public GitHub repositories and asking grounded questions about their source code. Repository files are cloned with JGit, split into overlapping line-aware chunks, embedded with Gemini, stored in PostgreSQL and Pinecone, and retrieved with a semantic plus keyword search before Gemini generates an answer with clickable source references.

The current product is intentionally public and requires no login. Submitting the same public GitHub URL reuses its shared repository record, so its conversations, search history, and saved locations are shared too. User accounts and private workspaces are a later phase.

## Stack

- **Backend:** Java 21, Spring Boot 3, Spring MVC, Spring Data JPA, JGit
- **Frontend:** React, Vite, React Markdown
- **AI:** Gemini embeddings and Gemini answer generation
- **Vector search:** Pinecone
- **Persistence:** PostgreSQL

## Requirements

- Java 21+
- Maven 3.9+ (or use a Maven wrapper if you add one)
- Node.js 20+
- PostgreSQL 15+
- Gemini API key
- Pinecone index configured for 768-dimensional dense vectors

## Configure services

Copy `backend/.env.example` to `backend/.env` and set the API keys, PostgreSQL credentials, and Pinecone index host. The app uses the PostgreSQL database **GitHub Repo Analyzer Database** by default. If it does not already exist, create it once by running `psql -U postgres -d postgres -f backend/sql/create_database.sql`. The JDBC URL percent-encodes spaces in the database name. Spring reads environment variables and does not load `.env` files automatically. Copy `frontend/.env.example` to `frontend/.env.local` if the API is not at `http://localhost:8000`.

Create a Pinecone index with 768 dimensions and cosine metric. The backend uses one namespace per repository (`repo-{repositoryId}`). The index host is the full host URL shown for the Pinecone index.

## Run locally

Start PostgreSQL first. With Docker Desktop running, from the project root run `docker compose up -d postgres`, or start your local PostgreSQL service. Then start the backend in one terminal:

```powershell
cd backend
$env:DB_URL = "jdbc:postgresql://localhost:5432/GitHub%20Repo%20Analyzer%20Database"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-postgres-password"
$env:GEMINI_API_KEY = "your-key"
$env:PINECONE_API_KEY = "your-key"
$env:PINECONE_INDEX_HOST = "https://your-index-host.pinecone.io"
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL (normally `http://localhost:5173`), paste a public GitHub URL, and wait for the repository status to become `completed` before asking questions.

To start PostgreSQL, the API, and the production frontend together with Docker Compose, copy the root `.env.example` to `.env` and `backend/.env.example` to `backend/.env`. Fill in the PostgreSQL password, Gemini key, and Pinecone key and index host, then run `docker compose up --build` from the project root. The web app is at `http://localhost:5173`.

## API

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/` | Service information |
| `GET` | `/health` | Health check |
| `POST` | `/repositories` | Start repository indexing; body `{ "url": "https://github.com/owner/repo" }` |
| `GET` | `/repositories/{id}` | Status and indexing statistics |
| `GET` | `/repositories/{id}/search?query=...` | Search indexed code chunks |
| `GET` | `/repositories/{id}/explore` | Repository language map, code areas, and likely entry points |
| `POST` | `/repositories/{id}/explore` | Search or trace a feature; body `{ "query": "authentication", "mode": "trace" }` |
| `GET` | `/repositories/{id}/search-history` | Recent feature searches stored in PostgreSQL |
| `GET` | `/repositories/{id}/saved-locations` | Bookmarked source ranges for this repository |
| `POST` | `/repositories/{id}/saved-locations` | Bookmark a source range; body `{ "file": "src/Auth.java", "startLine": 12, "endLine": 42, "note": "login flow" }` |
| `DELETE` | `/repositories/{id}/saved-locations/{locationId}` | Remove a bookmark |
| `POST` | `/repositories/{id}/ask` | Ask a question; body `{ "query": "..." }` |
| `GET` | `/repositories/{id}/ask?query=...` | Ask a question (compatibility endpoint) |
| `GET` | `/repositories/{id}/messages` | Recent conversation history |

The answer response has an `answer` field and `sources` array. Each source includes a file path, start/end lines, snippet, and GitHub URL.

Feature Finder searches are recorded per repository in the `search_history` table with their mode (`search` or `trace`), result count, and timestamp. The UI shows the latest 12 so developers can resume previous investigations. Bookmarked code snippets live in `saved_locations`. Hibernate creates or updates the `repositories`, `code_chunks`, `messages`, `search_history`, and `saved_locations` tables in **GitHub Repo Analyzer Database**. The browser also remembers the last active repository and restores the saved chat transcript after a refresh.

## Processing details

- Supports common source and configuration files (Java, Kotlin, JavaScript/TypeScript, Python, Go, Rust, C/C++, HTML/CSS, JSON/YAML, Markdown, SQL, shell, and more).
- Skips generated, dependency, build, and lock files, plus files larger than `MAX_FILE_BYTES`.
- Limits indexing by default to 5,000 source files, 50 MiB of accepted source, and 5,000 chunks per repository. Configure `MAX_REPOSITORY_FILES`, `MAX_REPOSITORY_SOURCE_BYTES`, and `MAX_REPOSITORY_CHUNKS` to adjust these limits.
- Applies in-memory per-client limits to costly endpoints: 3 repository indexing requests per hour and 20 ask/search/feature queries per minute. Configure `INDEX_REQUESTS_PER_HOUR` and `QUERIES_PER_MINUTE` to adjust them. Limits are per backend process and reset when it restarts; use a shared rate-limit store before running multiple backend instances.
- Rejects oversized question, search, and repository URL inputs with a `400` response.
- Defaults to 40-line chunks with 10-line overlap.
- Stores repository metadata, code chunks, conversations, search history, and saved locations in the **GitHub Repo Analyzer Database** PostgreSQL database.
- Uses a 70% semantic / 30% keyword retrieval score when Pinecone is available; keyword retrieval remains available if Pinecone is not configured.
- Includes a Feature Finder with a language breakdown, likely entry points, code-area overview, and explainable feature search results.
- Sources link to the cloned default branch and the cited line range.

## Deployment

The repository includes a Render Blueprint in `render.yaml` for the Java API and PostgreSQL database. Connect the GitHub repository to Render and create the Blueprint; review the selected service/database plans before confirming because Render may charge for the plans selected. Enter `GEMINI_API_KEY`, `PINECONE_API_KEY`, `PINECONE_INDEX_HOST`, and the Vercel frontend origin when prompted. The API container converts Render's PostgreSQL connection URL into the JDBC URL Spring expects.

Deploy the `frontend` directory as a Vercel Vite project: set its project root to `frontend`, build command to `npm run build`, output directory to `dist`, and `VITE_API_URL` to the deployed Render API URL. After Vercel assigns the production URL, set Render's `FRONTEND_ORIGIN` to that exact origin and redeploy the API. Keep credentials in the hosting dashboards; do not put them in the repository. For Docker Compose deployment, configure `VITE_API_URL` and `FRONTEND_ORIGIN` in the root `.env` before building so browsers use public service URLs instead of `localhost`.

## Current limitations

- Public GitHub repositories only; private repository authentication is not implemented.
- Indexing runs asynchronously inside the API process. A process restart interrupts active indexing; a durable task queue is a future improvement.
- Source links point at the cloned default branch and do not pin a commit SHA.
- Pinecone vectors require the configured index dimension to match Gemini's requested 768 dimensions.
- Set appropriate request limits, authentication, and rate limiting before exposing a deployment publicly.

## Project layout

```text
backend/   Spring Boot API, ingestion, Gemini, Pinecone, PostgreSQL
frontend/  React + Vite chat application
docs/      Product roadmap and feature sequence
```

See [the product roadmap](docs/PRODUCT_ROADMAP.md) for the staged plan for impact analysis, guided tours, test mapping, request tracing, and cross-repository search.
