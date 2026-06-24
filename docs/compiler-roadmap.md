# Compiler roadmap

This document is the narrative version of [`../PLAN.md`](../PLAN.md).

`PLAN.md` remains the authoritative implementation checklist. This file explains the same direction in a more readable form for contributors and future readers.

## Current focus

Current focus: **Phase 4 - semantic analysis split**.

The main goal is to keep the parser syntactic and move language meaning into semantic passes.

In practical terms:

- invalid syntax should be reported by the parser;
- syntactically valid but meaningless code should be reported by semantic analysis;
- future compiler stages should consume a stable AST plus semantic information.

## Phase 1 - Build health

Status: **complete**.

The repository can be built and tested from a clean checkout using the Maven Wrapper.

Completed work includes:

- Maven Wrapper restoration;
- parser cursor helper restoration;
- baseline Maven/JUnit execution;
- cleanup of duplicate or obsolete error paths that blocked compilation.

Exit condition: `./mvnw test` works from a clean checkout.

## Phase 2 - Parser semantics

Status: **in progress**.

The parser is being kept syntactic, predictable, and recoverable under invalid input.

Completed work includes:

- clearer parser cursor behavior;
- assignment target parsing cleanup;
- expression AST-shape tests;
- package split between parser grammar and parser support;
- top-level, block-level, and class-body recovery improvements;
- a current decision to use `ParseException` plus explicit recovery boundaries instead of AST error nodes;
- class-member parsing aligned with the language rule that every member declares its own access modifier.

Remaining work includes:

- adding more focused tests for parser recovery and token advancement when new recovery bugs are found;
- keeping grammar methods small enough that cursor movement remains easy to audit.

## Phase 3 - Diagnostics

Status: **complete**.

Lexer and parser errors now use a shared structured diagnostic model.

Completed work includes:

- diagnostic severity and phase modeling;
- structured expected/actual token information;
- lexer diagnostics for malformed input;
- parser diagnostics owned by parser state;
- removal of old global/static error collection paths;
- top-level and block-level recovery support.

Exit condition: front-end runs can return an AST plus diagnostics without relying on global error state.

## Phase 4 - Semantic analysis split

Status: **in progress**.

The goal is to make parsing syntax-only and validate meaning through semantic passes.

Completed or partially completed work includes:

- declaration collection;
- semantic scope construction;
- name resolution;
- duplicate declaration validation;
- initializer and assignment type checking;
- function-call checks for argument count and argument types;
- array access validation;
- direct class field and method checks;
- return checking;
- l-value checking;
- `break` / `continue` context checking;
- moving undefined identifier and undefined class checks out of expression parsing;
- removing parser-owned symbol-table scope construction and symbol registration.

Remaining work includes:

- expanding type checking across inheritance, overloads, arrays, member access, and calls;
- replacing the remaining parser-side type-registry/type-syntax coupling;
- keeping parser-generated ASTs simple and complete.

## Phase 5 - Real type model

Status: **not started**.

The compiler still relies too much on lexer token classes as semantic type representation.

Planned work:

- add parsed type syntax nodes;
- add resolved semantic type symbols;
- separate primitive, class, array, and generic types at the semantic level;
- model Nova classes and Nova mathematical/value types separately.

This phase is important before implementing advanced language features.

## Phase 6 - Multi-file project pipeline

Status: **not started**.

The compiler flow is currently single-file oriented.

Planned work:

- introduce `Compiler`;
- introduce `SourceFile`;
- introduce `CompilationUnit`;
- introduce `ProjectContext`;
- lex and parse all files before project-level semantic analysis;
- collect declarations across files;
- add package/import placeholders.

Exit condition: two `.nv` files can reference each other.

## Phase 7 - Standard library as source

Status: **not started**.

The parser no longer hard-codes standard-library function registration. The long-term direction is for user code and standard-library code to share the same compiler path, but builtin and standard-library declarations are not modeled semantically yet.

Planned work:

- create `stdlib/`;
- decide how native/builtin declarations enter semantic declaration collection before source loading exists;
- represent standard-library functions as Nova source declarations where possible;
- represent native/builtin functions as declarations with backend flags;
- preload standard-library source through the multi-file compiler pipeline.

## Phase 8 - IR preparation

Status: **not started**.

The project should introduce a backend-neutral lowered representation before native code generation.

Planned work:

- lower literals, variables, assignments, blocks, conditionals, loops, and calls;
- keep lowering separate from optimization;
- add an IR printer;
- prepare for a later control-flow graph.

## Phase 9 - Advanced Nova features

Status: **not started**.

Ambitious language features should wait until the core front-end pipeline is stable.

Planned work includes:

- method overloading by signature;
- access control;
- inheritance conflict checks;
- generics;
- bounded generics;
- class parameters;
- operator-overloadable Nova types;
- lambdas;
- variadic generics;
- monomorphization.

> [!WARNING]
> Variadic generics and lambdas are not independent features.
> A likely lambda representation depends on `Function[{A}, R]`, where `{A}` is a variable-length argument-type list.
> That means the type checker, function signatures, overload resolution, and monomorphizer must understand variadic type parameters before lambdas can be considered complete.

## Compile-time metaprogramming

Status: **future design item**.

Nova's hard-static model deliberately excludes ordinary runtime reflection and dynamic method discovery from core language semantics.
Anything that would normally rely on runtime type inspection should be redesigned as a compile-time facility.

Examples include:

- test discovery;
- serialization metadata;
- dependency injection registries;
- plugin or command registries;
- schema generation;
- generated adapters for standard-library tooling.

The expected direction is compiler-assisted code generation: the compiler or build tool scans source-level constructs and emits direct registries or generated code.

> [!NOTE]
> This is not a backend-only concern.
> Compile-time metaprogramming needs a stable semantic model because generated code must be based on resolved declarations, resolved types, visibility rules, and eventually package/project boundaries.

## Development rule of thumb

When in doubt, prefer the smallest change that makes the compiler more testable.

Good compiler commits usually do one of these things:

- clarify one parser rule;
- add one diagnostic;
- move one semantic check out of parsing;
- add one semantic validation pass;
- add regression tests for one bug;
- simplify one representation boundary.
