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

The GitHub Actions workflow currently tests with Temurin JDK 24 and 26.

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
- semantic-boundary AST preservation for syntactically valid but semantically invalid programs;
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
- overload selection and no-match diagnostics;
- subtype assignment and inherited member access;
- invalid array access;
- semantic type categories such as value/math, class/object, array, generic-parameter, and unknown;
- invalid return statements;
- invalid `break` and `continue` placement.

### Integration tests

Integration tests should cover complete front-end flows:

```text
source file(s)
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

Phase 6 integration tests should use the project-level compiler entry point once
it exists. The first target is a two-file cross-reference test where one source
file refers to a class declared in another file, proving that declaration
collection happens across all parsed units before name/type resolution.

## Testing rule of thumb

When fixing a compiler bug, add the smallest source snippet that reproduces it.

Good tests usually answer one question:

- Did this source produce the expected AST?
- Did this source produce the expected diagnostic?
- Did parser recovery preserve the valid code after the error?
- Did the semantic pass reject the correct construct?

## CI behavior

The repository has a GitHub Actions workflow that runs the test suite on pushed branches and pull requests targeting `main`.

The workflow also uploads Surefire test reports as artifacts, publishes a JUnit test report in GitHub Actions, and checks local Markdown documentation links.

## Reports

Maven Surefire writes test reports under:

```text
target/surefire-reports/
```

When CI fails, download the matching `surefire-reports-jdk-24` or `surefire-reports-jdk-26` artifact from the failed workflow run to inspect the raw test output.

## Before opening a pull request

Run:

```bash
./mvnw test
```

A documentation-only change does not usually need new compiler tests, but it should still avoid broken links and misleading implementation claims.
