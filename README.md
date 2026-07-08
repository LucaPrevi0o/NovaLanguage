# NovaLanguage

NovaLanguage is the reference compiler project for the Nova programming language.

This repository currently focuses on the **compiler front end**: reading Nova source code, turning it into tokens and an AST, and running early semantic validation passes. The backend is intentionally not implemented yet.

## Current status

The implementation follows the living roadmap in [`PLAN.md`](PLAN.md).

Current focus: **Phase 7 - standard library as source planning**.

Phase 4 is complete: parsing now builds syntax-only ASTs, while semantic passes own name resolution, scope construction, duplicate checks, type checks, return checks, l-value checks, and loop-control validation.
Phase 5 type-model groundwork is complete enough for Phase 6: parsed type syntax is preserved by declaration AST nodes, class declarations preserve multiple generic parameter names, and semantic type symbols now distinguish Nova type categories.
Phase 6 is complete: the project-level compiler entry point accepts multiple source files, creates compilation units for every input before semantic analysis, preserves file-aware diagnostics, and runs declaration collection plus semantic checks across the combined project AST.
Roadmap issues for Phases 1 through 8 are grouped under the `Nova MVP compiler`
milestone; advanced Phase 9 language features are tracked as separate post-MVP
milestones.

Implemented or partially implemented today:

- Lexical analysis
- Recursive-descent parsing
- AST construction with parsed type-syntax preservation
- Declaration AST constructors that accept parsed `TypeSyntax` directly
- Class declaration syntax for multiple generic parameters, preserved as parsed type syntax
- Parser recovery and structured lexer/parser diagnostics
- Semantic declaration collection
- Semantic scope construction
- Name resolution
- Duplicate declaration validation, including overload-signature duplicate checks
- Parser-side type-registry metadata removal
- Semantic type symbols for Nova value/math, class/object, array, generic-parameter, and unknown types
- Semantic type resolution through parsed `TypeSyntax`, with syntaxless `ReturnType` fallbacks isolated behind a compatibility bridge
- Semantic recognition of visible class generic parameter names
- Phase 6 source-file, compilation-unit, and file-aware diagnostic models
- Project-level `Compiler` and `ProjectContext` orchestration for multi-file front-end compilation
- Cross-file declaration collection, project scope construction, name/type resolution, and semantic checks
- Type checking for variables, initializers, assignments, function/method calls, arrays, direct and inherited class members, class subtype assignment, and basic overload selection
- Return checking
- L-value checking
- `break` / `continue` context checking
- JUnit tests grouped by compiler layer
- GitHub Actions Java CI, documentation checks, Wiki mirroring, Javadoc/GitHub Pages publishing, YAML issue forms, deliverable-based Project milestone automation, label-based issue-kind automation, and issue archive-state automation

Not implemented yet:

- Full removal of temporary `ReturnType` adapter constructors, compatibility getters, fallback bridges, and printer compatibility paths
- Full package/import semantics
- Semantic standard-library declarations
- Standard library loading from Nova source files
- IR generation
- Optimization
- Native/backend code generation
- Advanced Nova features such as generic constraints, generic instantiation/specialization, lambdas, variadic generics, monomorphization, access-control checks, override validation, inherited-member conflict checks, generic overload specificity, and operator-overloadable Nova types
- Ecosystem tools such as Pulsar, Orbit, Nebula, and Quark package resolution

For the detailed implementation plan, see [`PLAN.md`](PLAN.md). For compiler
architecture and pipeline details, see [`docs/architecture.md`](docs/architecture.md).

## Repository layout

```text
.
├── src/                  # Compiler front-end source code
├── test/                 # JUnit tests grouped by compiler layer
├── docs/                 # Consolidated design and operations documentation
├── PLAN.md               # Living implementation checklist
├── pom.xml               # Maven project configuration
├── mvnw / mvnw.cmd       # Maven Wrapper
└── .github/workflows/    # CI workflows
```

Important documentation:

- [`docs/architecture.md`](docs/architecture.md) - compiler layers, parser rules, semantic passes, type model, and project pipeline
- [`docs/language-design.md`](docs/language-design.md) - long-term Nova language and ecosystem vision
- [`docs/project-automation.md`](docs/project-automation.md) - GitHub Project, issue, pull request, Wiki, and Pages automation
- [`docs/contributing.md`](docs/contributing.md) - development rules, tests, CI behavior, and documentation conventions

## Requirements

- Java 24 or newer
- Maven Wrapper, included in the repository

The CI workflow currently tests with Temurin JDK 24 and 26.

## Build and test

From the repository root:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

The Java CI workflow runs the test suite automatically on pushes to `main`, pull requests targeting `main`, and manual dispatch. It tests with Temurin JDK 24 and 26, uploads Surefire reports, publishes a JUnit report, and checks local Markdown documentation links.

A separate documentation workflow mirrors each Markdown file from `docs/` into one GitHub Wiki page and publishes generated Javadocs to GitHub Pages when documentation, source, build, or publishing files change on `main`. Project automation workflows keep issue milestones, managed labels, PR-derived status, and archive visibility aligned with the roadmap Project.

## What Nova is intended to become

Nova is designed as a compiled, object-oriented language with a **hard-static** type system, a deliberate distinction between classes and mathematical/value-like types, and a long-term goal of native compilation.

> [!NOTE]
> In Nova, "hard-static" means ordinary language semantics should be known at compile time: overload resolution, operator dispatch, generic instantiation, and type-driven tooling should not depend on runtime reflection or dynamic method lookup.

The broader ecosystem vocabulary is also being reserved now: **Pulsar** for the compiler, **Orbit** for the package manager, **Nebula** for the community package registry, and **Quark** for standalone Nova artifacts.

Those language and ecosystem goals are documented separately from the implementation status because many of them are not implemented yet.

See [`docs/language-design.md`](docs/language-design.md) for the broader design vision and ecosystem naming guide.

## Development approach

The project intentionally grows in small, testable phases:

1. Keep `./mvnw test` green.
2. Keep parser work syntactic whenever possible.
3. Move name resolution, type checks, duplicate checks, l-value checks, and context checks into semantic passes.
4. Prefer focused commits that can be reviewed and tested independently.
5. Do not start advanced language features before the core front-end pipeline is stable.

See [`PLAN.md`](PLAN.md) for the authoritative checklist.

## License

This project is licensed under the Creative Commons Atribution-NonCommercial-ShareAlike 4.0 licensing (see [`LICENSE`](LICENSE)).
