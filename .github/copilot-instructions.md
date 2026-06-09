<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
specs/001-improve-search-relevance/plan.md
<!-- SPECKIT END -->

# Tech Interview Helper — Agent Instructions

Spring Boot 3.3.5 / Java 21 REST API for managing interview questions with PostgreSQL, Elasticsearch, and Caffeine cache. Full project docs: [README.md](../README.md).

## Build & Run

```bash
# Recommended: all services via Docker Compose
docker-compose up -d --build tih-app

# Health check
curl http://localhost:8080/actuator/health
# Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Architecture & Layer Conventions

```
controller/   →  REST endpoints, @Valid on request bodies, no business logic
service/      →  Business logic, @Transactional(readOnly = true) by default, write methods override with @Transactional
mapper/       →  MapStruct interfaces only — never manual mapping code
dto/          →  Request/Response POJOs; use builder pattern where applicable
model/        →  JPA entities extend BaseAuditEntity; ES document: QuestionDocument
repository/   →  Spring Data interfaces only
exception/    →  ResourceNotFoundException, DuplicateResourceException; GlobalExceptionHandler returns RFC 7807 ProblemDetail
config/       →  App-wide beans (cache, OpenAPI, WebConfig, ElasticsearchIndexInitializer)
```

## Key Conventions

**IDs**: JPA entities use `Long id` (internal, DB-generated). Public API exposes `UUID externalId` (set in `@PrePersist`). Never expose the internal `Long` in external-facing responses unless already present in existing DTOs.

**MapStruct + Lombok order**: The `maven-compiler-plugin` annotation processor path puts Lombok **before** MapStruct — do not reorder. Always annotate mapper interfaces with `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`.

**Caching**: Named caches — `languages`, `categories`, `categoriesByLanguage`, `questions` — expire after 30 min via Caffeine. Use `@Cacheable` / `@CacheEvict` on service methods. Evict relevant cache keys on any write operation.

**Elasticsearch dual-path**: `QuestionSearchService` handles ES full-text search; `QuestionService.search()` falls back to PostgreSQL FTS when ES is unreachable. Both code paths must be maintained. `ElasticsearchIndexInitializer` bulk-upserts all questions on startup.

**Exception handling**: Throw `ResourceNotFoundException` for missing entities, `DuplicateResourceException` for constraint violations. `GlobalExceptionHandler` converts these to `ProblemDetail` — do not return raw error strings from controllers.

**Database migrations**: All schema changes go in `src/main/resources/db/changelog/migrations/` as numbered XML files (`NNN-description.xml`) and referenced in `db.changelog-master.xml`. Never use `spring.jpa.hibernate.ddl-auto=update`.

**Validation**: Use `jakarta.validation` annotations on DTOs (e.g., `@NotBlank`, `@NotNull`). Controllers annotate request bodies with `@Valid`.

**API versioning**: All endpoints are under `/api/v1/`. New resource controllers follow `@RequestMapping("/api/v1/<resource>")`.

**Logging**: `logging.level.com.tih=DEBUG` in dev. Use `@Slf4j` or `@Log4j2` (Lombok) — no `System.out.println`.

## Question Content Generation

A dedicated agent generates batch question answers for import into the UI.

| File | Purpose |
|------|---------|
| `.github/agents/question-bot.agent.md` | Agent: takes a list of questions, generates answers, assigns metadata, outputs a JSON file |
| `.github/instructions/question-format.instructions.md` | Reference: JSON structure, answer Markdown syntax, valid languages/categories/tags |
| `questions-export.json` | Example export: real questions from the live DB (source of truth for format) |

**Usage:** Invoke via `/question-bot <list of questions>` in GitHub Copilot Chat.
The agent outputs a `questions-ai-generated-<YYYYMMDD>.json` file ready for upload via `POST /api/v1/batch/upload`.

**Answer format at a glance:**
```
#L<1|2|3>

> ⚠️ AI-Generated, needs to be verified

<Markdown content>
```

## Pitfalls

- `GENERAL_LANGUAGE_ID = 1L` in `QuestionService` is always included alongside any language filter — preserve this logic when modifying search/filter methods.
- Tests directory (`src/test/java`) is empty — new tests should use Testcontainers (already on the classpath via `spring-boot-testcontainers` BOM).
- `spring.jpa.hibernate.ddl-auto=validate` — any new entity fields need a corresponding Liquibase migration or startup will fail.
- ES index settings/analyzers live in `src/main/resources/elasticsearch/settings.json` — changes there require re-indexing.
