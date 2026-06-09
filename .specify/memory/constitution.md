<!--
SYNC IMPACT REPORT
==================
Version change: (new) → 1.0.0
Added sections: Core Principles (I–V), Technology Stack Constraints, Development Workflow, Governance
Removed sections: N/A (initial fill from template)
Templates reviewed:
  ✅ .specify/templates/plan-template.md — Constitution Check section is generic; aligns with updated principles
  ✅ .specify/templates/spec-template.md — Scope/requirements sections align; no principle-driven changes required
  ✅ .specify/templates/tasks-template.md — Task phases and path conventions align; no changes required
  ✅ .specify/workflows/speckit/workflow.yml — CLAUDE references are valid integration-option listings, not agent-only guidance
Follow-up TODOs: none
-->

# Tech Interview Helper Constitution

## Core Principles

### I. REST-First API Design
Every application feature MUST be exposed as a RESTful HTTP endpoint. All endpoints MUST carry complete SpringDoc OpenAPI annotations (`@Operation`, `@ApiResponse`) before a PR is submitted. Breaking API contracts requires a version increment and a documented migration plan.

**Rationale**: The service is a backend API consumed by external clients; discoverability and contract stability are non-negotiable.

### II. Integration-Test-Driven Development
Integration tests using Testcontainers MUST be written alongside feature implementation. Tests MUST exercise real infrastructure (PostgreSQL, Elasticsearch) — mocking persistence or search dependencies is prohibited. New endpoints or service methods MUST have at least one integration test before merge.

**Rationale**: Caffeine cache, Liquibase migrations, and Elasticsearch queries interact in ways that unit tests cannot adequately cover.

### III. Schema-Managed Persistence (NON-NEGOTIABLE)
ALL database schema changes MUST be delivered as append-only Liquibase changesets under `src/main/resources/db/changelog/migrations/`. Direct DDL execution against any running environment is prohibited. Existing changesets MUST NOT be modified after they have been applied anywhere.

**Rationale**: Reproducible, auditable schema evolution is required for Docker Compose bootstrapping, CI pipelines, and production deployments.

### IV. Containerised by Default
The canonical development and deployment environment is Docker Compose. All services (application, PostgreSQL, Elasticsearch) MUST start correctly with a single `docker-compose up --build` command. Changes that break containerised execution MUST NOT be merged.

**Rationale**: Eliminates environment drift and guarantees consistent behaviour across developer machines and deployment targets.

### V. Simplicity & YAGNI
Abstractions, design patterns, and additional dependencies are only permitted when a concrete, current requirement demands them. Generic infrastructure or speculative "just in case" code MUST be removed or rejected in review. Every deviation from the approved technology stack MUST be justified in the `Complexity Tracking` section of the relevant plan.

**Rationale**: A focused backend service must remain easy to onboard, reason about, and maintain over time.

## Technology Stack Constraints

- **Language**: Java 21 (LTS) — downgrade is prohibited; major version upgrades require team review.
- **Framework**: Spring Boot 3.3.x — minor patch updates are allowed; major upgrades require team review.
- **Persistence**: PostgreSQL 16 via Spring Data JPA + Hibernate.
- **Search**: Elasticsearch 8.x via Spring Data Elasticsearch; index settings MUST live under `src/main/resources/elasticsearch/`.
- **Migrations**: Liquibase only — all changesets under `src/main/resources/db/changelog/`.
- **Cache**: Caffeine in-memory cache; distributed caching requires explicit approval.
- **Build**: Maven 3.9 via `pom.xml`; Gradle is not permitted.
- **Object Mapping**: MapStruct; manual DTO↔entity mapping in service or controller layers is prohibited.
- **Containerisation**: Multi-stage `Dockerfile` + Docker Compose for all environments.

Dependencies outside this approved stack MUST be proposed, justified, and approved before being added to `pom.xml`.

## Development Workflow

1. **Feature branching**: All work MUST happen on feature branches from `develop`; direct commits to `develop` or `main` are prohibited.
2. **Spec-first**: New features MUST have a completed `spec.md` and `plan.md` before coding begins.
3. **Migrations first**: Liquibase changesets MUST be delivered before any JPA entity or service change that depends on them.
4. **OpenAPI first**: Full OpenAPI annotations MUST be complete before a PR is submitted.
5. **PR quality gate**: PRs MUST pass all integration tests and have zero unresolved review comments before merge.
6. **No destructive Git operations**: Force-pushing to `develop` or `main` and amending published commits are prohibited.

## Governance

This constitution supersedes all informal coding conventions. Amendments MUST:

1. Be proposed as a PR modifying `.specify/memory/constitution.md`.
2. Increment the version according to semantic versioning (MAJOR: principle removal or redefinition; MINOR: new principle or section; PATCH: clarification or wording fix).
3. Include a rationale explaining why the change is needed and what impact it has.
4. Be reviewed and approved before merge.

All PRs MUST include a Constitution Check confirming no principles are violated. Violations MUST be explicitly justified in the `Complexity Tracking` section of the relevant plan.

**Version**: 1.0.0 | **Ratified**: 2026-06-09 | **Last Amended**: 2026-06-09
