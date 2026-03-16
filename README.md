# Tech Interview Helper — Backend Service

A Spring Boot REST API that manages interview questions organised by programming language and category. It supports full-text search, audit history, bulk data import via JSON batch upload, and exposes an OpenAPI/Swagger UI for easy exploration.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Running with Docker (recommended)](#running-with-docker-recommended)
- [Running Locally (without Docker)](#running-locally-without-docker)
- [Environment Variables](#environment-variables)
- [API Documentation](#api-documentation)
- [Useful Endpoints](#useful-endpoints)

---

## Overview

| Feature | Details |
|---|---|
| Question management | CRUD for questions grouped by language & category |
| Full-text search | PostgreSQL FTS index on question content |
| Audit history | Hibernate Envers tracks every change with revision info |
| Batch import | Upload a JSON file to seed multiple questions at once |
| Caching | Caffeine in-memory cache for frequently read data |
| Schema migrations | Liquibase manages all DDL changes |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Persistence | Spring Data JPA + Hibernate + PostgreSQL 16 |
| Migrations | Liquibase |
| Audit | Hibernate Envers |
| Cache | Caffeine |
| Batch | Spring Batch |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9 |
| Containerisation | Docker + Docker Compose |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/tih/app/
│   │   ├── TihApp.java               # Entry point
│   │   ├── batch/                    # Spring Batch jobs (bulk import)
│   │   ├── config/                   # App configuration (cache, audit, OpenAPI, web)
│   │   ├── controller/               # REST controllers
│   │   ├── dto/                      # Request / response DTOs
│   │   ├── exception/                # Global exception handling
│   │   ├── mapper/                   # MapStruct mappers
│   │   ├── model/                    # JPA entities
│   │   ├── repository/               # Spring Data repositories
│   │   └── service/                  # Business logic
│   └── resources/
│       ├── application.properties
│       └── db/changelog/             # Liquibase migrations
└── test/
    └── java/com/tih/app/
        └── TihAppTests.java          # Integration tests (Testcontainers)
```

---

## Requirements

### Docker (recommended)

| Tool | Minimum version |
|---|---|
| Docker Desktop | 4.x |
| Docker Compose | v2 (bundled with Docker Desktop) |

> No local JDK or Maven needed — the multi-stage `Dockerfile` builds the JAR inside the container.

### Local development

| Tool | Minimum version |
|---|---|
| JDK | 21 |
| Maven | 3.9 |
| PostgreSQL | 16 |
| Docker | 4.x *(only for running tests via Testcontainers)* |

---

## Running with Docker (recommended)

```bash
# Clone the repository
git clone <repo-url>
cd tih-app

# Build the image and start all services
docker-compose up -d

# Follow application logs
docker-compose logs -f tih-app
```

To stop and **wipe all data**:

```bash
docker-compose down -v
```

To stop while **keeping data**:

```bash
docker-compose down
```

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

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `tih_db` | Database name |
| `DB_USER` | `tih_user` | Database user |
| `DB_PASSWORD` | `tih_password` | Database password |
| `SERVER_PORT` | `8080` | HTTP port the app listens on |

When running via Docker Compose these are set automatically via the `environment` block in `docker-compose.yml`.

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

| Endpoint | Description |
|---|---|
| `GET  /actuator/health` | Health check |
| `GET  /actuator/metrics` | Application metrics |
| `GET  /actuator/caches` | Cache statistics |
| `GET  /api/v1/languages` | List all languages |
| `GET  /api/v1/categories` | List all categories |
| `GET  /api/v1/questions` | List / search questions |
| `POST /api/v1/batch/upload` | Bulk import questions from JSON |
