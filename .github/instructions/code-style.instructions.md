---
description: >
  Java and unit-test code style rules for the Tech Interview Helper project.
  Split into two sections: general Java code style (all .java files) and
  unit-testing code style (test classes only).
  All agents producing Java code — java-developer, qa-engineer, speckit.implement — MUST follow
  every rule in this file. No exceptions.
applyTo: "**/*.java"
---

# Code Style

> All agents producing Java code — including `java-developer`, `qa-engineer`, and
> `speckit.implement` — **must** follow every rule in this file. No exceptions.

---

## Java Code Style

Rules that apply to **all** Java source files (production and test).

---

### Builder Pattern

Put **each builder setter on its own line**. Never chain multiple setters on a single line:

```java
// ✓ correct
Language language = Language.builder()
        .id(LANGUAGE_ID)
        .name("Java")
        .code(JAVA_CODE)
        .build();

// ❌ incorrect
Language language = Language.builder().id(LANGUAGE_ID).name("Java").code(JAVA_CODE).build();
```

---

### Import Style

Do **not** use wildcard imports — regular or static. Always import individual symbols:

```java
// ✓ correct
import org.springframework.stereotype.Service;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ❌ incorrect
import org.springframework.stereotype.*;
import static org.mockito.Mockito.*;
```

---

### Section Separator Comments

Do **not** use decorative separator comments:

```java
// ❌ incorrect — adds noise without value
// ------------------------------------------------------------------ findAll
// ================================================================== helpers
```

Let class structure, method names, and blank lines communicate organisation.

---

### Logging

Use `@Slf4j` or `@Log4J2` (Lombok) for all logging. Never use `System.out.println` or `System.err.println`:

```java
// ✓ correct
@Slf4j
@Service
public class QuestionService {
    public void someMethod() {
        log.debug("Processing question: {}", id);
    }
}

// ❌ incorrect
System.out.println("Processing question: " + id);
```

---

## Unit Testing Code Style

Rules that apply specifically to **test classes** (`*Test.java`, `*IT.java`).

---

### Mock Field Grouping

`@Mock` fields must be grouped with **no blank lines** between them.
A single **blank line** separates the last `@Mock` from `@InjectMocks`:

```java
// ✓ correct
@Mock
private QuestionRepository questionRepository;
@Mock
private QuestionMapper questionMapper;

@InjectMocks
private QuestionService service;

// ❌ incorrect — blank lines between @Mock fields
@Mock
private QuestionRepository questionRepository;

@Mock
private QuestionMapper questionMapper;
@InjectMocks
private QuestionService service;
```

---

### Given Section Layout

Within `// given`, place all **variable declarations first**, then a **blank line**, then all
Mockito stubs (`when(...)`, `doReturn(...)`, `doThrow(...)`):

```java
// given
Question question = buildQuestion();
LanguageDto dto = buildLanguageDto();

when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
when(questionMapper.toDto(question)).thenReturn(dto);
```

---

## Test Structure — given / when / then

Every test method must follow the **given → when → then** structure with explicit section comments:

```java
@Test
void findById_existingQuestion_returnsDto() {
    // given
    Question question = buildQuestion();

    when(questionRepository.findByExternalId(QUESTION_UUID)).thenReturn(Optional.of(question));

    // when
    QuestionDto result = questionService.findByExternalId(QUESTION_UUID);

    // then
    assertThat(result.getExternalId()).isEqualTo(QUESTION_UUID);
    assertThat(result.getQuestion()).isEqualTo(question.getQuestion());
}
```

---

### Collapsed `// when - then`

When the method call and the assertion are inseparable (e.g., `assertThatThrownBy`), collapse the
two sections using `// when - then` (hyphen, **not** slash `/`):

```java
// when - then
assertThatThrownBy(() -> service.findById(NON_EXISTENT_ID))
        .isInstanceOf(ResourceNotFoundException.class);
verify(repository, never()).save(any());
```

---

### ArgumentCaptor — Blank Line After Capture

Always leave a **blank line** between the `ArgumentCaptor` + `verify` call and the `.getValue()`
usage:

```java
// ✓ correct
ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
verify(elasticsearchOperations).save(captor.capture());

QuestionDocument doc = captor.getValue();
assertThat(doc.getId()).isEqualTo("1");

// ❌ incorrect — no blank line
ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
verify(elasticsearchOperations).save(captor.capture());
QuestionDocument doc = captor.getValue();
```
