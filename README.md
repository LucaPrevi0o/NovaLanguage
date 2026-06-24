# NovaLanguage

NovaLanguage is the reference compiler project for the Nova programming language.

This repository currently focuses on the **compiler front end**: reading Nova source code, turning it into tokens and an AST, and running early semantic validation passes. The backend is intentionally not implemented yet.

## Current status

The implementation follows the living roadmap in [`PLAN.md`](PLAN.md).

Current focus: **Phase 4 - semantic analysis split**.

Implemented or partially implemented today:

- Lexical analysis
- Recursive-descent parsing
- AST construction
- Parser recovery and structured diagnostics
- Semantic declaration collection
- Semantic scope construction
- Name resolution
- Duplicate declaration validation
- Initial type checking for variables, assignments, calls, arrays, and direct class members
- Return checking
- L-value checking
- `break` / `continue` context checking
- JUnit tests grouped by compiler layer
- GitHub Actions test automation

Not implemented yet:

- A complete semantic type model independent from lexer token classes
- Multi-file project compilation
- Semantic standard-library declarations
- Standard library loading from Nova source files
- IR generation
- Optimization
- Native/backend code generation
- Advanced Nova features such as full generics, lambdas, variadic generics, monomorphization, access-control checks, and operator-overloadable Nova types

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
- [`docs/compiler-roadmap.md`](docs/compiler-roadmap.md) - roadmap explained in narrative form
- [`docs/testing.md`](docs/testing.md) - test and CI notes
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

The GitHub Actions workflow runs the test suite automatically on pushed branches and pull requests targeting `main`. It also checks local Markdown documentation links.

## What Nova is intended to become

Nova is designed as a compiled, object-oriented language with a **hard-static** type system, a deliberate distinction between classes and mathematical/value-like types, and a long-term goal of native compilation.

> [!NOTE]
> In Nova, "hard-static" means ordinary language semantics should be known at compile time: overload resolution, operator dispatch, generic instantiation, and type-driven tooling should not depend on runtime reflection or dynamic method lookup.

Those language goals are documented separately from the implementation status because many of them are not implemented yet.

See [`docs/language-design.md`](docs/language-design.md) for the broader design vision.

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
