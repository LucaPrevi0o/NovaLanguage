# Contributing notes

This project is still evolving quickly. These notes describe the development style that keeps compiler changes reviewable.

For the implementation plan, see [`../PLAN.md`](../PLAN.md).

## Core rule

Keep changes small, testable, and tied to one compiler layer whenever possible.

A good commit usually does one thing:

- fixes one parser rule;
- adds one diagnostic;
- moves one semantic check out of the parser;
- adds one semantic validation;
- adds one regression test;
- updates one documentation page.

## Keep tests green

Before committing or opening a pull request, run:

```bash
./mvnw test
```

The GitHub Actions workflow also runs tests on pushed branches and pull requests targeting `main`.

## Parser changes

Parser changes should stay syntactic.

The parser should not reject code only because a name is undefined, a type does not exist, a declaration is duplicated, or an assignment target is semantically invalid.

Prefer this flow:

```text
parse syntactically
  ↓
build AST
  ↓
report semantic error later
```

## Semantic changes

Semantic changes should usually include tests.

When adding a new semantic rule:

1. add or identify the smallest source snippet that should fail;
2. assert the diagnostic;
3. implement the rule;
4. keep the parser behavior unchanged unless syntax truly changed.

## Diagnostics

Diagnostics should be structured and deterministic.

Prefer diagnostics that include:

- phase;
- severity;
- message;
- source line and column where available;
- expected token/type information where useful;
- actual token/type information where useful.

Avoid reintroducing global error state.

## Documentation changes

Keep the README short and factual.

Use the `docs/` directory for detailed explanations:

- architecture details belong in [`architecture.md`](architecture.md);
- parser details belong in [`parser.md`](parser.md);
- semantic details belong in [`semantic-analysis.md`](semantic-analysis.md);
- long-term language design belongs in [`language-design.md`](language-design.md);
- roadmap explanations belong in [`compiler-roadmap.md`](compiler-roadmap.md).

The README should describe what is true today. Ambitious future language ideas should stay in the design or roadmap documents.

## Advanced features

Do not start advanced Nova features too early.

Features such as generics, lambdas, variadic generics, monomorphization, operator-overloadable Nova types, and backend code generation depend on a stable semantic model.

Follow the phase ordering in [`../PLAN.md`](../PLAN.md) unless there is a strong reason to change it.
