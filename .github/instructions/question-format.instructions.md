---
description: >
  Describes the JSON structure and Markdown answer syntax for the Tech Interview Helper question bank.
  Apply this when generating, editing, validating, or importing interview questions and answers.
applyTo: "**"
---

# Question Format Reference

This document describes the exact format used by the Tech Interview Helper (TIH) question bank.
All AI agents that create, edit, or validate questions **must** follow these rules.

---

## JSON Structure

The export file is a **JSON array** of question objects. Each object has the following fields:

```json
{
  "extId":    "<UUID v4>",
  "question": "<plain text — no Markdown>",
  "answer":   "<Markdown string — see Answer Format below>",
  "language": "<language value — see valid values>",
  "category": "<category value — see valid values>",
  "tags":     ["<tag>", "..."]
}
```

| Field      | Type            | Required | Notes |
|------------|-----------------|----------|-------|
| `extId`    | string (UUID v4) | Yes     | Generate a new random UUID v4 for every new question |
| `question` | string           | Yes     | Plain text. No Markdown. Should be a clear, self-contained interview question |
| `answer`   | string           | Yes     | Markdown. Must follow the Answer Format described below |
| `language` | string           | Yes     | One of the valid language values listed below |
| `category` | string           | Yes     | One of the valid category values listed below |
| `tags`     | string array     | Yes     | Can be an empty array `[]`. Use for sub-topic classification |

> **Important:** The JSON array must be valid JSON — escape newlines as `\n` and double-quotes as `\"` inside answer strings.

---

## Answer Format

Every answer is a **Markdown string**. The structure is:

```
> ⚠️ AI-Generated, needs to be verified

<answer body in Markdown>
```

### Line-by-line breakdown

Always the very first line; no space before it.

**Line 1 — AI-generated disclaimer (required for all AI-generated answers)**

```markdown
> ⚠️ AI-Generated, needs to be verified
```

This blockquote marks the answer as unverified AI output. Do **not** omit it.

**Line 2 — blank line**

**Lines 3+ — Answer body**

Use standard Markdown:

- `**Bold**` for key terms and concepts introduced for the first time
- ` ```sql`, ` ```java`, ` ```text`, ` ```bash` — fenced code blocks with language hints
- Markdown tables (`| col | col |`) for comparisons (index types, isolation levels, etc.)
- Bullet lists (`*` or `-`) for enumerations without inherent order
- Numbered lists (`1.`, `2.`) for steps or ordered items
- `##` / `###` subheadings for multi-section answers
- `> Note:` blockquotes for supplementary notes or caveats
- Inline code `` `code` `` for SQL keywords, class names, method names, config values

### Complete answer example (L1)

```markdown
> ⚠️ AI-Generated, needs to be verified

A **database index** is a separate data structure maintained by the database engine
that allows it to find rows matching a condition **without scanning the entire table**.

**Why we use them:**

* **Faster queries:** A `SELECT` with a `WHERE` clause on an indexed column can jump
  directly to relevant rows instead of a full table scan.
* **Faster sorting:** `ORDER BY` on an indexed column avoids expensive sort steps.
* **Unique constraint enforcement:** `UNIQUE` indexes ensure data integrity.

**Trade-off:** Indexes speed up reads but slow down writes (`INSERT`, `UPDATE`, `DELETE`)
because the index must be updated alongside the table.
```

### Complete answer example (L3)

```markdown
> ⚠️ AI-Generated, needs to be verified

A **partial index** is an index built on a **subset of rows** defined by a `WHERE` predicate.

```sql
CREATE INDEX idx_orders_pending
    ON orders (created_at)
    WHERE status = 'pending';
```

**When it is beneficial:**

1. **High-cardinality status columns with a small active subset** — index only `pending`
   rows instead of 10 million rows.
2. **Soft-delete patterns** — index only non-deleted rows.
3. **Partial unique constraints** — enforce uniqueness within a subset.

**Requirement:** The query's `WHERE` clause must match the partial index predicate exactly.
```

---

## Valid Languages

| Value     | Description |
|-----------|-------------|
| `general` | Language-agnostic topics: SQL, databases, architecture, networking, security, cloud, soft skills |
| `java`    | Java-specific topics: JVM, Spring, Kafka, concurrency, core Java |

---

## Valid Categories

| Category            | Language  | Covers |
|---------------------|-----------|--------|
| `Database`          | `general` | SQL, indexes, transactions, PostgreSQL, MVCC |
| `Cloud Fundamentals`| `general` | Cloud concepts, AWS/GCP/Azure, serverless, containers |
| `Networking`        | `general` | HTTP/HTTPS, TCP/IP, REST, gRPC, DNS, TLS |
| `Security`          | `general` | Auth/AuthZ, OAuth2, JWT, encryption, OWASP Top 10 |
| `Microservices`     | `general` | Service design, API gateway, service mesh, resiliency |
| `Patterns`          | `general` | Design patterns (GoF), architectural patterns, DDD |
| `Soft Skills`       | `general` | Communication, teamwork, conflict resolution, leadership |
| `EngEx`             | `general` | Code quality, testing, CI/CD, observability, code review |
| `Core`              | `java`    | Core Java: types, OOP, generics, exceptions, I/O, streams |
| `Advanced Java`     | `java`    | JVM internals, class loading, GC algorithms, performance |
| `Multi-Threading`   | `java`    | Concurrency, threads, locks, `java.util.concurrent` |
| `Spring Framework`  | `java`    | Spring Boot, DI, AOP, MVC, Data, Security, testing |
| `Kafka`             | `java`    | Apache Kafka: producers, consumers, topics, offsets, streams |

**Selection rules:**
- If the question is about a Java-specific framework or library → `java` language + matching category
- If the question applies regardless of language → `general` language + matching category
- When unsure between two categories, choose the one that most precisely describes the question's topic

---

## Tags

Tags are **optional** sub-topic labels within a category. Use them when a question belongs to a
well-defined sub-topic that benefits from additional filtering.

**Common tag patterns:**

| Example tag          | When to use |
|----------------------|-------------|
| `Garbage Collection` | GC-specific Java questions |
| `Java Collections`   | `List`, `Map`, `Set` questions |
| `Spring Security`    | Spring Security-specific questions |
| `ACID`               | Transaction property questions |
| `Concurrency`        | Thread-safety, locking, race conditions |

**Rules:**
- Tags are free-form strings; use title case (e.g., `Garbage Collection`, not `garbage collection`)
- Do **not** duplicate the category name as a tag
- **can** add level tags (L1, L2, L3) in the tags array if questionlevel identified
- An empty `[]` is valid when no meaningful sub-topic tag applies

---

## Duplicate Warning Marker

When an answer covers a topic also addressed by another question, add a warning blockquote after the AI-Generated marker:

```markdown
> ⚠️ Duplicate found! Check for duplicates! (see: "<other question text>" — <brief context note>)
```

---

## Full Valid Example

```json
{
  "extId": "550e8400-e29b-41d4-a716-446655440001",
  "question": "What is a database transaction?",
  "answer": "> ⚠️ AI-Generated, needs to be verified\n\nA **database transaction** is a sequence of one or more SQL operations executed as a **single logical unit of work**. Either all operations succeed and are permanently saved, or none of them take effect.\n\n**ACID properties:**\n\n| Property | Meaning |\n|---|---|\n| **Atomicity** | All-or-nothing |\n| **Consistency** | Database moves between valid states |\n| **Isolation** | Concurrent transactions don't interfere |\n| **Durability** | Committed changes survive crashes |",
  "language": "general",
  "category": "Database",
  "tags": ["L1"]
}
```
