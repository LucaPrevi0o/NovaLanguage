# Testing

NovaLanguage uses JUnit tests executed through Maven.

For the current implementation plan, see [`../PLAN.md`](../PLAN.md).

## Running tests locally

From the repository root:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

The Maven Wrapper is included in the repository, so contributors should prefer it over a globally installed Maven version.

## Java version

The project is configured for Java 24 or newer.

The GitHub Actions workflow currently uses Temurin JDK 26.

## Test organization

Tests are grouped by compiler layer.

Current or intended groups include:

- lexer tests;
- parser tests;
- semantic analysis tests;
- diagnostic tests;
- integration tests.

This organization helps keep ownership boundaries visible.

## What to test

### Lexer tests

Lexer tests should cover tokenization and lexical diagnostics.

Useful cases:

- identifiers;
- keywords;
- literals;
- comments;
- whitespace handling;
- unknown tokens;
- unterminated strings or characters;
- malformed escapes;
- source-position reporting.

### Parser tests

Parser tests should cover AST shape, syntax diagnostics, and recovery.

Useful cases:

- declaration parsing;
- class parsing;
- expression precedence;
- assignment associativity;
- invalid syntax recovery;
- unknown token recovery;
- block-level recovery;
- parser cursor movement regressions.

### Semantic tests

Semantic tests should cover meaning, not syntax.

Useful cases:

- undefined identifiers;
- undefined types/classes;
- duplicate declarations;
- invalid assignment targets;
- type mismatches;
- invalid function-call arguments;
- invalid array access;
- invalid return statements;
- invalid `break` and `continue` placement.

### Integration tests

Integration tests should cover complete front-end flows:

```text
source
  ↓
lexer
  ↓
parser
  ↓
semantic analysis
  ↓
diagnostics
```

These tests are useful when a behavior depends on multiple compiler layers.

## Testing rule of thumb

When fixing a compiler bug, add the smallest source snippet that reproduces it.

Good tests usually answer one question:

- Did this source produce the expected AST?
- Did this source produce the expected diagnostic?
- Did parser recovery preserve the valid code after the error?
- Did the semantic pass reject the correct construct?

## CI behavior

The repository has a GitHub Actions workflow that runs the test suite on pushes and pull requests targeting `main`.

The workflow also uploads Surefire test reports as artifacts and publishes a JUnit test report in GitHub Actions.

## Reports

Maven Surefire writes test reports under:

```text
target/surefire-reports/
```

When CI fails, download the `surefire-reports` artifact from the failed workflow run to inspect the raw test output.

## Before opening a pull request

Run:

```bash
./mvnw test
```

A documentation-only change does not usually need new compiler tests, but it should still avoid broken links and misleading implementation claims.
