---
description: >
  Defines testing standards for the Tech Interview Helper project: unit test structure,
  naming conventions, coverage requirements, folder layout, mocking rules, and best practices.
  Apply this whenever writing, reviewing, or generating any test code.
applyTo: "**/src/test/**/*.java"
---

# Testing Standards

> **Code style rules** (builder format, import style, mock grouping, given-section layout,
> `// when - then` collapsed sections, ArgumentCaptor blank line, section separator comments)
> are defined in **[`.github/instructions/code-style.instructions.md`](.github/instructions/code-style.instructions.md)**.
> This file covers testing *methodology and structure* only.

---

## Test Folder Layout

| Test type | Source root | Naming suffix |
|---|---|---|
| Unit tests | `src/test/java/com/tih/app/` | `*Test.java` |
| Integration / functional tests | `src/test/java/com/tih/app/integration/` | `*IT.java` |

Unit tests and integration tests **must not** share a package. Integration tests that require
Testcontainers, a real database, Elasticsearch, or a full Spring context belong under
`integration/`. Pure unit tests (with mocked dependencies) stay in the mirrored package of the
class under test.

---

## Coverage Requirement

- **Business logic (services, domain helpers)** → line + branch coverage **> 80 %** (mandatory).
- **Controllers** → every endpoint must have at least one `@WebMvcTest` test covering the happy path and each distinct error response (404, 400, 409, …).
- **Mappers** → every MapStruct mapper must have a dedicated unit test covering `toDto`, `toDtoList`, `toEntity` / `updateEntity` (where applicable), and null-input guards.
- Every pull request / change set must include tests for all new and modified code paths.
- After implementing tests, run the full test suite and confirm **all tests pass** before
  considering the task done.

---

## Naming Conventions

- Test class name = **`<ClassUnderTest>Test`** (e.g. `QuestionService` → `QuestionServiceTest`).
- Test method name describes the scenario:
  `<methodName>_<scenario>_<expectedOutcome>` or plain English — either is acceptable as long as
  the name is self-explanatory without reading the body.
- Integration test class name = **`<Feature>IT`** (e.g. `SearchFallbackIT`).

---

## Assertions — AssertJ (primary)

Use **AssertJ** for all assertions. It is already on the classpath via `spring-boot-starter-test`.

```java
// Preferred — AssertJ
assertThat(result).isNotNull();
assertThat(result.getItems()).hasSize(3).extracting(QuestionDto::getQuestion)
    .containsExactlyInAnyOrder("Q1", "Q2", "Q3");

// Acceptable — Hamcrest assertThat (for cases where a matcher is cleaner)
assertThat(result.getScore(), greaterThan(0.8));

// Forbidden — bare JUnit assertions
assertEquals("expected", actual);   // ❌
assertTrue(condition);               // ❌
assertNull(value);                   // ❌
```

> Reason: AssertJ produces descriptive failure messages and chains naturally; bare JUnit assertions
> require the developer to read the code to understand the failure.

---

## Mocking Framework — Mockito

**Mockito** (bundled with `spring-boot-starter-test`) is the default mocking framework for unit
tests. Use constructor injection in the class under test so dependencies can be injected as mocks.

```java
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionService questionService;
```

**Rule:** Field name for the subject under test should be the service name (e.g., `service`, `categoryService`). For mock grouping and blank-line rules, see [code-style.instructions.md](.github/instructions/code-style.instructions.md).

### Mocking static methods

For static method calls, use **`Mockito.mockStatic()`** (available in Mockito 5, included with
Spring Boot 3). Do **not** add PowerMock — it is incompatible with JUnit 5.

```java
try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
    uuidMock.when(UUID::randomUUID).thenReturn(FIXED_UUID);

    // when / then ...
}
```

### Rules

- **Prefer mocks over real instances.** Integration tests are valuable, but they introduce
  infrastructure dependencies and slow down the build. Use them only when the test must verify
  interaction with a real system (DB, ES, HTTP).
- Never instantiate the class under test's transitive dependencies (repositories, REST clients,
  etc.) in a unit test — always mock them.
- Use `@Spy` only when partial real behavior is genuinely required and document why.

---

## Keeping Tests Short and Focused

### One assertion cluster per test

Each test should verify **one behaviour**. Split large tests into smaller focused ones:

```java
// ❌ One test doing too much
@Test
void testSearch() {
    // ... 40 lines testing three different scenarios
}

// ✓ Small, focused tests
@Test
void search_blankKeyword_returnsAllQuestions() { ... }

@Test
void search_matchingKeyword_returnsFilteredResults() { ... }

@Test
void search_noResults_returnsEmptyPage() { ... }
```

### Branch coverage rule

If the method under test contains an **`if-else`** (or any conditional), write **at least two
tests** — one for each branch:

```java
@Test
void findByLanguage_withLanguageFilter_includesGeneralLanguage() { ... }

@Test
void findByLanguage_withoutLanguageFilter_returnsAllLanguages() { ... }
```

### Overcomplicated tests are a design signal

> If a unit test becomes hard to write or read, the code under test is likely too complex.
> Refactor the production code first; do not paper over complexity with a large test.

---

## Extracting Repetitive Setup

Move repetitive object construction and multi-field assertion groups into **private helper
methods** with descriptive names. This improves readability more than inline duplication:

```java
// ❌ Repeated inline construction
Question q1 = new Question();
q1.setExternalId(UUID.randomUUID());
q1.setQuestion("What is JVM?");
q1.setAnswer("...");
// ... repeated in every test

// ✓ Builder helper
private Question buildQuestion() {
    return Question.builder()
        .externalId(QUESTION_UUID)
        .question("What is JVM?")
        .answer("The Java Virtual Machine...")
        .build();
}

// ✓ Named assertion helper
private void assertMatchesQuestion(QuestionDto dto, Question entity) {
    assertThat(dto.getExternalId()).isEqualTo(entity.getExternalId());
    assertThat(dto.getQuestion()).isEqualTo(entity.getQuestion());
    assertThat(dto.getCategory()).isEqualTo(entity.getCategory().getName());
}
```

Keep shared constants (UUIDs, IDs, strings) as `private static final` fields at the top of the
test class. Extract a constant whenever the same literal appears **more than 2–3 times**.

```java
private static final long LANGUAGE_ID    = 1L;
private static final long NON_EXISTENT_ID = 99L;
private static final String JAVA_CODE    = "java";
```

---

## Integration & Functional Tests

- Place all tests that require Spring context, a database, Elasticsearch, or external services
  under `src/test/java/com/tih/app/integration/`.
- Use **Testcontainers** (already on the classpath) to provide real PostgreSQL / Elasticsearch
  containers. Never rely on an externally running service in CI.
- Annotate with `@Testcontainers` + `@ServiceConnection`; share a single static container per
  class with `@Container static ...`.
- Integration tests are slow — keep their count lower than unit tests. If a behaviour can be
  verified with a mock, write a unit test instead.

---

## Controller Tests

Use `@WebMvcTest` for controller-layer tests. These are lightweight slice tests — no full Spring context, no real database.

```java
@WebMvcTest(LanguageController.class)
class LanguageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LanguageService languageService;
```

**Rules:**
- One `@WebMvcTest` class per controller. Name it `<Controller>Test.java` (not `IT`).
- Use `@MockBean` for every service / repository the controller depends on.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) is auto-loaded — no extra setup needed.
- Assert the HTTP status code, the key JSON fields, and the `code` field in error bodies.
- Do **not** use `@SpringBootTest` for controller-only tests — it is too heavy.

```java
// ✓ correct — lightweight @WebMvcTest
@Test
void findById_notFound_returns404() throws Exception {
    when(languageService.findById(99L))
            .thenThrow(new ResourceNotFoundException("Language", 99L));

    mockMvc.perform(get("/api/v1/languages/{id}", 99L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(ErrorMessage.RESOURCE_NOT_FOUND));
}
```

---

## Mapper Tests

MapStruct mapper implementations have no Spring dependencies and can be instantiated directly — no Spring context required.

```java
class LanguageMapperTest {

    private final LanguageMapper mapper = new LanguageMapperImpl();

    @Test
    void toDto_mapsAllFields() { ... }

    @Test
    void toDto_nullInput_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }
}
```

**Rules:**
- Every mapper must have a `<Mapper>Test.java` in `src/test/java/com/tih/app/mapper/`.
- Cover: `toDto` happy path, `toDto` null input, `toDtoList`, `toEntity` / `updateEntity` where present.
- For mappers with custom `@Named` methods (e.g. `tagsToNames`), cover null input, empty input, and sorting/transformation behaviour.
- Do **not** use `@SpringBootTest` — instantiate the generated `*MapperImpl` directly.

---

## Quick Checklist

Before marking a task as done, confirm:

- [ ] Every new/changed service method has at least one unit test.
- [ ] All `if-else` branches are covered by separate tests.
- [ ] Test class is named `<ClassUnderTest>Test`.
- [ ] Each test uses the `// given / // when / // then` structure (or `// when - then` when collapsed).
- [ ] Blank line separates variable declarations from `when(...)`/`doReturn(...)` stubs in `// given`.
- [ ] Each builder setter is on its own line.
- [ ] Constants are extracted for any literal used more than 2–3 times.
- [ ] `@Mock` fields have no blank lines between them; one blank line before `@InjectMocks`.
- [ ] No decorative `// ----` section separator comments.
- [ ] No wildcard static imports (`import static org.mockito.Mockito.*` is forbidden).
- [ ] `ArgumentCaptor` has a blank line between capture and `.getValue()`.
- [ ] AssertJ `assertThat(...)` is used — no bare JUnit assertions.
- [ ] No test instantiates a real repository or external client.
- [ ] Repetitive construction/assertion code is extracted into helper methods.
- [ ] Integration tests live in `integration/` and are named `*IT`.
- [ ] `mvn test` passes with no failures.
