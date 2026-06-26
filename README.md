# NovaLanguage

NovaLanguage is the reference compiler project for the Nova programming language.

This repository currently focuses on the **compiler front end**: reading Nova source code, turning it into tokens and an AST, and running early semantic validation passes. The backend is intentionally not implemented yet.

## Current status

The implementation follows the living roadmap in [`PLAN.md`](PLAN.md).

Current focus: **Phase 5 - type model groundwork**.

Phase 4 is still tracked as in progress because the parser-generated AST still needs a final simplicity/completeness cleanup, but the main parser/semantic split is already in place.
Roadmap issues for Phases 1 through 8 are grouped under the `Nova MVP compiler`
milestone; advanced Phase 9 language features are tracked as separate post-MVP
milestones.

Implemented or partially implemented today:

- Lexical analysis
- Recursive-descent parsing
- AST construction with parsed type-syntax preservation
- Parser recovery and structured lexer/parser diagnostics
- Semantic declaration collection
- Semantic scope construction
- Name resolution
- Duplicate declaration validation, including overload-signature duplicate checks
- Parser-side type-registry metadata removal
- Semantic type symbols for Nova value/math, class/object, array, generic-parameter, and unknown types
- Semantic type resolution through parsed `TypeSyntax`, with syntaxless `ReturnType` fallbacks isolated behind a compatibility bridge
- Type checking for variables, initializers, assignments, function/method calls, arrays, direct and inherited class members, class subtype assignment, and basic overload selection
- Return checking
- L-value checking
- `break` / `continue` context checking
- JUnit tests grouped by compiler layer
- GitHub Actions Java CI, documentation checks, Wiki mirroring, Javadoc/GitHub Pages publishing, and deliverable-based Project milestone automation

Not implemented yet:

- Full removal of temporary `ReturnType` adapters from declaration AST construction and printer compatibility paths
- Multi-file project compilation
- Semantic standard-library declarations
- Standard library loading from Nova source files
- IR generation
- Optimization
- Native/backend code generation
- Advanced Nova features such as full generics, lambdas, variadic generics, monomorphization, access-control checks, override validation, inherited-member conflict checks, generic overload specificity, and operator-overloadable Nova types
- Ecosystem tools such as Pulsar, Orbit, Nebula, and Quark package resolution

For the detailed implementation plan, see [`PLAN.md`](PLAN.md) and [`docs/compiler-roadmap.md`](docs/compiler-roadmap.md).

## Repository layout

```text
.
├── src/                  # Compiler front-end source code
├── test/                 # JUnit tests grouped by compiler layer
├── docs/                 # Design and implementation documentation
├── PLAN.md               # Living implementation checklist
├── pom.xml               # Maven project configuration
├── mvnw / mvnw.cmd       # Maven Wrapper
└── .github/workflows/    # CI workflows
```

Important documentation:

- [`docs/architecture.md`](docs/architecture.md) - current compiler-front-end architecture
- [`docs/parser.md`](docs/parser.md) - parser structure and responsibilities
- [`docs/semantic-analysis.md`](docs/semantic-analysis.md) - semantic passes and scope model
- [`docs/language-design.md`](docs/language-design.md) - long-term Nova language vision
- [`docs/ecosystem.md`](docs/ecosystem.md) - Nova ecosystem naming and package vocabulary
- [`docs/compiler-roadmap.md`](docs/compiler-roadmap.md) - roadmap explained in narrative form
- [`docs/testing.md`](docs/testing.md) - test and CI notes
- [`docs/project-automation.md`](docs/project-automation.md) - GitHub Project and issue automation notes
- [`docs/contributing.md`](docs/contributing.md) - development rules for future changes

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

A separate documentation workflow mirrors Markdown files from `docs/` into the GitHub Wiki and publishes generated Javadocs to GitHub Pages when documentation, source, build, or publishing files change on `main`.

## What Nova is intended to become

Nova is designed as a compiled, object-oriented language with a **hard-static** type system, a deliberate distinction between classes and mathematical/value-like types, and a long-term goal of native compilation.

> [!NOTE]
> In Nova, "hard-static" means ordinary language semantics should be known at compile time: overload resolution, operator dispatch, generic instantiation, and type-driven tooling should not depend on runtime reflection or dynamic method lookup.

The broader ecosystem vocabulary is also being reserved now: **Pulsar** for the compiler, **Orbit** for the package manager, **Nebula** for the community package registry, and **Quark** for standalone Nova artifacts.

Those language and ecosystem goals are documented separately from the implementation status because many of them are not implemented yet.

See [`docs/language-design.md`](docs/language-design.md) for the broader design vision and [`docs/ecosystem.md`](docs/ecosystem.md) for the ecosystem naming guide.

## Development approach

The project intentionally grows in small, testable phases:

1. Keep `./mvnw test` green.
2. Keep parser work syntactic whenever possible.
3. Move name resolution, type checks, duplicate checks, l-value checks, and context checks into semantic passes.
4. Prefer focused commits that can be reviewed and tested independently.
5. Do not start advanced language features before the core front-end pipeline is stable.

See [`PLAN.md`](PLAN.md) for the authoritative checklist.

## License

No license file is currently included in this repository.
