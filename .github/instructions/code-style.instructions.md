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
> `speckit.implement` — **MUST** strictly follow every rule in this file. NO EXCEPTIONS.

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

### Import Ordering

Organise imports into **groups separated by a single blank line**, in this exact order:

1. `static` imports
2. `java.*`
3. `org.*`
4. `com.*` (project and third-party)
5. `lombok.*`
6. `jakarta.*`

Within each group, list imports in alphabetical order.

```java
// ✓ correct
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tih.app.dto.CategoryDto;
import com.tih.app.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Validator;

// ❌ incorrect — groups mixed, no blank lines between groups
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import static org.mockito.Mockito.when;
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

### Java Records for Simple DTOs

Use Java records for immutable DTOs that carry no business logic — request/response objects, error payloads, simple data containers:

```java
// ✓ correct
public record LanguageCreateRequest(
        @NotBlank String name,
        @NotBlank String code) {}

// ❌ incorrect — Lombok @Data class for a simple request DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageCreateRequest { ... }
```

Prefer records over Lombok `@Data` / `@Builder` / `@NoArgsConstructor` / `@AllArgsConstructor` when the class is immutable and has no builder-pattern callers that require mutation.

---

### No Inner Classes

Do **not** define inner classes (including inner enums and inner interfaces). Extract them to their own top-level file:

```java
// ❌ incorrect — inner enum adds coupling and complicates testing
public class BatchUploadService {
    private enum ItemStatus { SAVED, SKIPPED, FAILED }
}

// ✓ correct — extracted to its own file
// ItemStatus.java
enum ItemStatus { SAVED, SKIPPED, FAILED }
```

---

### Generic Type Arguments — Diamond Operator

Use the diamond operator `<>` instead of repeating explicit type arguments on the right-hand side:

```java
// ✓ correct
List<QuestionTransferItem> items = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {});

// ❌ incorrect — redundant explicit type argument
List<QuestionTransferItem> items = objectMapper.readValue(file.getInputStream(), new TypeReference<List<QuestionTransferItem>>() {});
```

---

### String Literal Constants

Define a `private static final` constant instead of duplicating a string literal three or more times in the same class:

```java
// ✓ correct
private static final String ITEM_PREFIX = "Item ";
...
errors.add(ITEM_PREFIX + index + ": " + msg);

// ❌ incorrect — "Item " duplicated across multiple lines
errors.add("Item " + (i + 1) + ": " + violations...);
...
errors.add("Item " + (i + 1) + ": " + e.getMessage());
```

---

### Method Ordering

Within a class, order members as follows:

1. Constants (`private static final`)
2. Fields
3. Constructors (or `@RequiredArgsConstructor` via Lombok)
4. Public methods
5. Package-private / protected methods
6. Private methods

Private helper methods must appear **after** all public methods:

```java
// ✓ correct
public CategoryDto findById(Long id) {
    return mapper.toDto(getOrThrow(id));
}

private Category getOrThrow(Long id) {  // ← at the end
    ...
}

// ❌ incorrect — private helper before the public method that calls it
private Category getOrThrow(Long id) { ... }

public CategoryDto findById(Long id) { ... }
```

---

### Error Responses

`GlobalExceptionHandler` must return a consistent `ErrorResponse` record — **not** Spring's `ProblemDetail`. All `@ExceptionHandler` methods must return `ResponseEntity<ErrorResponse>`:

```java
// ✓ correct — structured, app-branded error payload
ErrorResponse error = ErrorResponse.builder()
        .code(ErrorMessage.RESOURCE_NOT_FOUND)
        .message(ex.getMessage())
        .source("tih-app")
        .build();
return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

// ❌ incorrect — Spring ProblemDetail leaks implementation detail
ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
return detail;
```

The `ErrorResponse` structure:
```json
{
  "code": "CAPITALIZED_ERROR_CODE",
  "message": "string",
  "source": "app-name",
  "errors": [
    { "code": "string", "field": "string", "message": "string" }
  ]
}
```

---

### Blank Line After Class Declaration

Always leave a blank line between the class declaration line and the first field or constant:

```java
// ✓ correct
public class QuestionService {

    private static final long GENERAL_LANGUAGE_ID = 1L;

// ❌ incorrect — no blank line
public class QuestionService {
    private static final long GENERAL_LANGUAGE_ID = 1L;
```

---

### Single Return Statement

Methods should have **one return statement**, located at the end. Use early returns only for guard clauses at the very start of a method body — not scattered throughout the logic:

```java
// ✓ correct — single exit point at the end
public QuestionDto findById(Long id) {
    Question question = getQuestionOrThrow(id);
    QuestionDto dto = questionMapper.toDto(question);

    return dto;
}

// ✓ correct — early return for guard clause only (first lines)
public List<Tag> resolveTags(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
        return Collections.emptyList();
    }

    return new ArrayList<>(tagRepository.findAllById(tagIds));
}

// ❌ incorrect — return statements scattered throughout the method body
public ItemStatus processItem(...) {
    if (condition) {
        doSomething();
        return ItemStatus.SKIPPED;
    }
    doMoreWork();
    if (otherCondition) {
        return ItemStatus.FAILED;
    }
    doFinalWork();
    return ItemStatus.SAVED;
}
```

---

### Blank Lines Before `return`

Add a blank line immediately before a `return` statement when the line above it is any executable statement. Omit the blank line only when `return` is the sole statement in the method or the first (and only) statement in a short branch:

```java
// ✓ correct — blank line before return after preceding statements
public CategoryDto create(CategoryCreateRequest request) {
    Category category = categoryMapper.toEntity(request);
    category.setLanguage(language);

    return categoryMapper.toDto(categoryRepository.save(category));
}

// ✓ correct — single-statement method, no blank line needed
public CategoryDto findById(Long id) {
    return categoryMapper.toDto(getCategoryOrThrow(id));
}

// ❌ incorrect — return immediately follows a statement without a blank line
public CategoryDto create(CategoryCreateRequest request) {
    Category category = categoryMapper.toEntity(request);
    category.setLanguage(language);
    return categoryMapper.toDto(categoryRepository.save(category));
}
```

---

### Blank Lines Around Control Flow Statements

**Before** `if`, `for`, `try`: add a blank line when preceded by any other statement (not when it is the first statement in a block).

**After** the closing `}` of an `if`, `for`, `try` block: add a blank line when followed by another statement (not a closing `}` of the enclosing block).

**Between** `else`/`else if` and `catch`/`finally`: place the keyword on a **new line** after `}` — never on the same line. Add **no blank line** between `if/else` chains or `try/catch/finally` blocks:

```java
// ✓ correct
List<Long> languageIds = resolveLanguageIds(languageId);
Page<Question> result;

if (languageIds != null && categoryId != null) {
    result = questionRepository.findAllByLanguageIdInAndCategoryId(languageIds, categoryId, pageable);
}
else if (languageIds != null) {
    result = questionRepository.findAllByLanguageIdIn(languageIds, pageable);
}
else {
    result = questionRepository.findAll(pageable);
}

return toPageResponse(result);

// ✓ correct — try/catch
try {
    items = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {});
}
catch (Exception e) {
    log.error("Failed to parse upload file", e);
    return errorResponse();
}

// ❌ incorrect — no blank line before if, else on same line as }
List<Long> languageIds = resolveLanguageIds(languageId);
Page<Question> result;
if (languageIds != null) {
    result = questionRepository.findAllByLanguageIdIn(languageIds, pageable);
} else {
    result = questionRepository.findAll(pageable);
}
return toPageResponse(result);
```

---

### Single-Line Comments

Use `//` for any comment that fits on one line. Never use `/* ... */` or `/** ... */` block syntax for a comment that is a single sentence:

```java
// ✓ correct
// ID of the "General" language — always included alongside any selected language filter.
private static final long GENERAL_LANGUAGE_ID = 1L;

// ❌ incorrect — block comment for a single line
/**
 * ID of the "General" language.
 */
private static final long GENERAL_LANGUAGE_ID = 1L;

/* ID of the "General" language. */
private static final long GENERAL_LANGUAGE_ID = 1L;
```

---

### No Ternary Operators

Do **not** use the ternary operator `? :`. Use an explicit `if-else` block instead:

```java
// ✓ correct
if (languageIds != null) {
    result = questionRepository.searchByFullTextWithLanguageIds(query, languageIds, categoryId, pageable);
}
else {
    result = questionRepository.searchByFullText(query, null, categoryId, pageable);
}

// ❌ incorrect — ternary hides the two branches
result = languageIds != null
        ? questionRepository.searchByFullTextWithLanguageIds(query, languageIds, categoryId, pageable)
        : questionRepository.searchByFullText(query, null, categoryId, pageable);
```

---

### Error Code Constants

All error code string values **must** be defined as `public static final String` constants in `com.tih.app.util.ErrorCode`. Never write a raw error code string literal in any other class:

```java
// ✓ correct — constant defined in ErrorCode, referenced everywhere
ErrorResponse.builder()
        .code(ErrorCode.RESOURCE_NOT_FOUND)
        ...

// ❌ incorrect — raw string literal used in handler
ErrorResponse.builder()
        .code("RESOURCE_NOT_FOUND")
        ...
```

When adding a new error code, add its constant to `ErrorCode` first, then reference it.

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

### Test Structure — given / when / then

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
