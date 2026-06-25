---
name: Bug report
about: Report a compiler regression, incorrect diagnostic, or wrong Nova front-end behavior.
title: "bug: "
labels: [bug]
assignees: []
---

## Goal

<!-- Describe the bug or regression in one or two sentences. -->

## Project metadata

- Milestone: Phase 5 - Type model
- Area: diagnostics
- Kind: bug
- Priority: P1
- Size: M
- Suggested status: Backlog

## Reproduction

```nova
// Add the smallest Nova snippet that reproduces the problem.
```

Steps to reproduce:

1. 
2. 
3. 

## Expected behavior

<!-- What should the compiler, parser, semantic analyzer, or diagnostic output do? -->

## Actual behavior

<!-- What happens instead? Include diagnostics, stack traces, or command output if useful. -->

## Affected compiler layer

<!-- Choose one or more: lexer, parser, ast, diagnostics, semantic-analysis, type-system, multi-file-pipeline, stdlib, ir, backend, tests, docs, ci, architecture, project-management. -->

## Scope

- [ ] Add or update the smallest regression test that reproduces the bug.
- [ ] Fix the bug without expanding unrelated language behavior.
- [ ] Keep parser changes syntactic unless syntax truly changes.

## Out of scope

<!-- List related behavior that should not be changed by this issue. -->

## Acceptance criteria

- [ ] The reproduction is covered by a focused test or documented reason why no test is needed.
- [ ] The reported behavior is fixed or the diagnostic is corrected.
- [ ] Existing tests pass with `./mvnw test`.
- [ ] Relevant documentation is updated if behavior or diagnostics changed.

## Notes

<!-- Add links to related issues, PRs, docs, or suspected files. -->
