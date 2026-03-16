# Tech Interview Helper — Backend Service

A Spring Boot REST API that manages interview questions organised by programming language and category. It supports
full-text search powered by Elasticsearch, bulk data import via JSON batch upload, and exposes an OpenAPI/Swagger UI for
easy exploration.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Running with Docker (recommended)](#running-with-docker-recommended)
- [Rebuilding & Redeploying with Docker](#rebuilding--redeploying-with-docker)
- [Running Locally (without Docker)](#running-locally-without-docker)
- [Environment Variables](#environment-variables)
- [Elasticsearch](#elasticsearch)
- [API Documentation](#api-documentation)
- [Useful Endpoints](#useful-endpoints)

---

## Overview

| Feature             | Details                                                   |
|---------------------|-----------------------------------------------------------|
| Question management | CRUD for questions grouped by language & category         |
| Full-text search    | Elasticsearch — fuzzy, partial, phrase & weighted ranking |
| Batch import        | Upload a JSON file to seed multiple questions at once     |
| Caching             | Caffeine in-memory cache for frequently read data         |
| Schema migrations   | Liquibase manages all DDL changes                         |

---

## Tech Stack

| Layer            | Technology                                     |
|------------------|------------------------------------------------|
| Language         | Java 21                                        |
| Framework        | Spring Boot 3.3.5                              |
| Persistence      | Spring Data JPA + Hibernate + PostgreSQL 16    |
| Search           | Elasticsearch 8.15 + Spring Data Elasticsearch |
| Migrations       | Liquibase                                      |
| Cache            | Caffeine                                       |
| API Docs         | SpringDoc OpenAPI (Swagger UI)                 |
| Build            | Maven 3.9                                      |
| Containerisation | Docker + Docker Compose                        |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/tih/app/
│   │   ├── TihApp.java               # Entry point
│   │   ├── batch/                    # Bulk import logic
│   │   ├── config/                   # App configuration (cache, OpenAPI, web, ES initializer)
│   │   ├── controller/               # REST controllers (incl. AdminController)
│   │   ├── dto/                      # Request / response DTOs
│   │   ├── exception/                # Global exception handling
│   │   ├── mapper/                   # MapStruct mappers
│   │   ├── model/                    # JPA entities + QuestionDocument (ES)
│   │   ├── repository/               # Spring Data repositories
│   │   └── service/                  # Business logic (incl. QuestionIndexService, QuestionSearchService)
│   └── resources/
│       ├── application.properties
│       ├── elasticsearch/
│       │   └── settings.json         # ES index settings (analyzers, ngram filter)
│       └── db/changelog/             # Liquibase migrations
└── test/
    └── java/com/tih/app/
        └── TihAppTests.java          # Integration tests (Testcontainers)
```

---

## Requirements

### Docker (recommended)

| Tool           | Minimum version                  |
|----------------|----------------------------------|
| Docker Desktop | 4.x                              |
| Docker Compose | v2 (bundled with Docker Desktop) |

> No local JDK or Maven needed — the multi-stage `Dockerfile` builds the JAR inside the container.

### Local development

| Tool          | Minimum version                                   |
|---------------|---------------------------------------------------|
| JDK           | 21                                                |
| Maven         | 3.9                                               |
| PostgreSQL    | 16                                                |
| Elasticsearch | 8.x                                               |
| Docker        | 4.x *(only for running tests via Testcontainers)* |

---

## Running with Docker (recommended)

```bash
# Clone the repository
git clone <repo-url>
cd tih-app

# Build the image and start all services (PostgreSQL + Elasticsearch + app)
docker-compose up -d

# Follow application logs
docker-compose logs -f tih-app
```

To stop and **wipe all data**:

```bash
# ⚠️ Destroys postgres_data AND elasticsearch_data volumes
docker-compose down -v
```

To stop while **keeping data**:

```bash
docker-compose down
```

---

## Rebuilding & Redeploying with Docker

Use these workflows whenever you change source code, dependencies, or configuration and need to push the update into the
running container.

### Rebuild and restart the app only (most common)

Stops the `tih-app` container, rebuilds its image from the current source, and starts it again — PostgreSQL is left
untouched.

```bash
docker-compose up -d --build tih-app
```

### Force a clean rebuild (no Docker layer cache)

Useful when you change the `pom.xml`, `Dockerfile`, or suspect a stale cache layer.

```bash
docker-compose build --no-cache tih-app
docker-compose up -d tih-app
```

### Rebuild everything (app + all services)

```bash
docker-compose up -d --build
```

### Restart the app container without rebuilding

Picks up changes to environment variables in `docker-compose.yml` but does **not** recompile the code.

```bash
docker-compose restart tih-app
```

### Full reset (wipe DB and start fresh)

> ⚠️ This destroys all data in the `postgres_data` and `elasticsearch_data` volumes.

```bash
docker-compose down -v
docker-compose up -d --build
```

### Check the app is healthy after redeployment

```bash
# Watch container status
docker-compose ps

# Tail logs until the app is ready
docker-compose logs -f tih-app

# Quick health check
curl http://localhost:8080/actuator/health
```

> Liquibase runs automatically on every startup and applies any pending migrations before the app begins accepting
> traffic.

---

## Running Locally (without Docker)

1. **Start a PostgreSQL instance** (or use Docker just for the DB):

```bash
docker-compose up -d postgres
```

2. **Build and run the application**:

```bash
./mvnw spring-boot:run
```

Or build the JAR first and run it:

```bash
./mvnw package -DskipTests
java -jar target/tih-app-0.0.1-SNAPSHOT.jar
```

3. **Run tests** (requires Docker for Testcontainers):

```bash
./mvnw test
```

---

## Environment Variables

All variables have sensible defaults for local development.

| Variable      | Default        | Description                  |
|---------------|----------------|------------------------------|
| `DB_HOST`     | `localhost`    | PostgreSQL host              |
| `DB_PORT`     | `5432`         | PostgreSQL port              |
| `DB_NAME`     | `tih_db`       | Database name                |
| `DB_USER`     | `tih_user`     | Database user                |
| `DB_PASSWORD` | `tih_password` | Database password            |
| `SERVER_PORT` | `8080`         | HTTP port the app listens on |
| `ES_HOST`     | `localhost`    | Elasticsearch host           |
| `ES_PORT`     | `9200`         | Elasticsearch HTTP port      |

When running via Docker Compose these are set automatically via the `environment` block in `docker-compose.yml`.

---

## Elasticsearch

### How indexing works

| Event                           | What happens                                                                        |
|---------------------------------|-------------------------------------------------------------------------------------|
| App startup                     | `ElasticsearchIndexInitializer` bulk-upserts all existing questions from PostgreSQL |
| `POST /api/v1/questions`        | Question is saved to PostgreSQL and immediately indexed in ES                       |
| `PUT  /api/v1/questions/{id}`   | Updated document is re-indexed in ES                                                |
| `DELETE /api/v1/questions/{id}` | Document is removed from the ES index                                               |
| `POST /api/v1/batch/upload`     | Each saved question is indexed in ES after the PostgreSQL save                      |
| Search fallback                 | If ES is unreachable, the app falls back to PostgreSQL FTS automatically            |

### Verify the index

**Check document count:**

```bash
curl http://localhost:9200/questions/_count
```

Expected response when 40 questions are loaded:

```json
{
  "count": 40
}
```

**Inspect the index mapping (all indexed fields):**

```bash
curl http://localhost:9200/questions/_mapping | python3 -m json.tool
```

**Check cluster health:**

```bash
curl http://localhost:9200/_cluster/health?pretty
```

**Preview a sample indexed document:**

```bash
curl "http://localhost:9200/questions/_search?size=1&_source=id,questionText,languageCode,categoryName&pretty"
```

**Search directly against Elasticsearch (bypass the app):**

```bash
curl "http://localhost:9200/questions/_search?q=thread&size=3&pretty"
```

### Manual re-index

If questions exist in PostgreSQL but are missing from Elasticsearch (e.g., after a fresh start where ES was temporarily
unavailable), trigger a full re-sync:

```bash
curl -X POST http://localhost:8080/api/v1/admin/reindex
```

Expected response:

```json
{
  "status": "ok",
  "indexed": 40
}
```

### Test search quality

```bash
# Exact phrase match
curl "http://localhost:8080/api/v1/questions/search?q=thread+pool"

# Partial word (ngram)
curl "http://localhost:8080/api/v1/questions/search?q=concurr"

# Fuzzy / typo tolerance
curl "http://localhost:8080/api/v1/questions/search?q=completabl+futur"

# Filter by language + free-text
curl "http://localhost:8080/api/v1/questions/search?q=deadlock&languageId=1"
```

### Search architecture

The `/api/v1/questions/search` endpoint runs a compound Elasticsearch query with six ranked layers:

| Priority | Query type                        | Boost                          | Purpose                               |
|----------|-----------------------------------|--------------------------------|---------------------------------------|
| 1        | `match_phrase` on `questionText`  | ×6                             | Exact phrase in the question          |
| 2        | `match_phrase` on `answerContent` | ×3                             | Exact phrase in the answer            |
| 3        | `multi_match best_fields`         | `questionText^4, answer^1`     | All tokens present, question weighted |
| 4        | `multi_match cross_fields AND`    | `questionText^2, answer^1`     | Words spread across fields            |
| 5        | `multi_match fuzziness=AUTO`      | `questionText^2, answer^0.5`   | Typo tolerance                        |
| 6        | `multi_match on *.ngram`          | `questionText^0.5, answer^0.2` | Partial word / substring hits         |

Term frequency is handled natively by Elasticsearch's BM25 scorer.

---

## API Documentation

Once the application is running, the interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI spec (JSON):

```
http://localhost:8080/api-docs
```

---

## Useful Endpoints

| Endpoint                           | Description                                            |
|------------------------------------|--------------------------------------------------------|
| `GET  /actuator/health`            | Health check (includes Elasticsearch status)           |
| `GET  /actuator/metrics`           | Application metrics                                    |
| `GET  /actuator/caches`            | Cache statistics                                       |
| `GET  /api/v1/languages`           | List all languages                                     |
| `GET  /api/v1/categories`          | List all categories                                    |
| `GET  /api/v1/questions`           | List questions with optional filters                   |
| `GET  /api/v1/questions/search?q=` | Full-text search (Elasticsearch, falls back to PG FTS) |
| `POST /api/v1/batch/upload`        | Bulk import questions from JSON                        |
| `POST /api/v1/admin/reindex`       | Re-sync all questions from PostgreSQL → Elasticsearch  |
