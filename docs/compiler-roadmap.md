# Compiler roadmap

This document is the narrative version of [`../PLAN.md`](../PLAN.md).

`PLAN.md` remains the authoritative implementation checklist. This file explains the same direction in a more readable form for contributors and future readers.

For the ecosystem naming vocabulary, see [`ecosystem.md`](ecosystem.md).

## Current focus

Current focus: **Phase 5 - type model groundwork**.

The main goal is to introduce a real type model without undoing the parser/semantic boundary.
GitHub milestones track deliverables rather than internal phase numbers: Phase 1
through Phase 8 work belongs to the `Nova MVP compiler` milestone, while Phase 9
advanced language features are tracked as separate post-MVP milestones.

In practical terms:

- invalid syntax should be reported by the parser;
- syntactically valid but meaningless code should be reported by semantic analysis;
- parsed type syntax should be preserved before semantic resolution;
- future compiler stages should consume a stable AST plus semantic type information.

## Ecosystem direction

The compiler itself is planned under the name **Pulsar**.

The package ecosystem is planned around these names:

- **Quark**: a standalone Nova artifact or package;
- **Orbit**: the package manager that resolves and updates Quark dependencies;
- **Nebula**: the community registry that hosts published Quarks;
- **Core**: the minimal standard base package;
- **Solar**, **Pulse**, **Spectrum**, **Echo**, and **Atlas**: optional standard-library package families.

These names are documentation and design commitments, not current implementation status. Package resolution, registry access, and standard-package loading are future phases.

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

Status: **complete**.

The parser is now syntactic, predictable, and recoverable under the invalid-input cases covered so far.

Completed work includes:

- clearer parser cursor behavior;
- assignment target parsing cleanup;
- expression AST-shape tests;
- package split between parser grammar and parser support;
- top-level, block-level, and class-body recovery improvements;
- a current decision to use `ParseException` plus explicit recovery boundaries instead of AST error nodes;
- class-member parsing aligned with the language rule that every member declares its own access modifier;
- smaller class-parser helpers for header parsing, body parsing, member parsing, and AST population.

Future parser work should be driven by concrete bugs or grammar changes, especially:

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

Status: **complete**.

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
- class subtype assignment and argument compatibility through resolved superclass chains;
- inherited class field and method lookup;
- basic function and method overload selection with no-match and ambiguity diagnostics;
- duplicate validation for repeated overload signatures while allowing distinct function, method, and constructor signatures;
- return checking;
- l-value checking;
- `break` / `continue` context checking;
- moving undefined identifier and undefined class checks out of expression parsing;
- removing parser-owned symbol-table scope construction and symbol registration;
- adding parsed type syntax nodes that declaration AST constructors receive directly;
- removing the parser-side `TypeRegistry` adapter.
- parser semantic-boundary regression tests for representative semantically invalid programs.

Future semantic work is intentionally scoped to later phases:

- deferring advanced semantic rules, such as access control, override compatibility, inherited-member conflicts, and generic overload specificity, until the type model boundary is cleaner.

## Phase 5 - Real type model

Status: **in progress**.

Parser-created declarations preserve source `TypeSyntax`, and semantic type symbols carry
Nova-level type categories. Declaration AST constructors now accept parsed `TypeSyntax`
directly, while syntaxless `ReturnType` compatibility fallbacks are isolated behind
`semantic.type.ReturnTypeSyntaxBridge` for older manually constructed AST nodes.

Completed work:

- parsed type syntax nodes for named, array, and generic-parameter type syntax;
- declaration AST constructors accept parsed `TypeSyntax` directly;
- parser-created declarations no longer need temporary `ReturnType` adapters;
- declaration AST nodes expose parsed `TypeSyntax` directly while keeping `ReturnType` compatibility getters;
- semantic declarations preserve both parsed `TypeSyntax` and the temporary `ReturnType` adapter;
- semantic type symbols for value, class, array, generic-parameter, and unknown types;
- semantic type kinds that distinguish value/math types from class/object types;
- name resolution resolves declared types through the semantic type resolver;
- type checking uses semantic type symbols internally instead of `ReturnType` token-class comparisons;
- `ReturnType` is now a source-syntax-first compatibility adapter alongside direct `TypeSyntax` AST APIs;
- syntaxless `ReturnType` fallback conversion is isolated in `ReturnTypeSyntaxBridge`;
- the parser-side `TypeRegistry` adapter has been removed;
- built-in primitive-like declarations resolve to Nova value type symbols;
- the `TypeSyntax` to `TypeSymbol` boundary is documented in [`type-model.md`](type-model.md).

Planned work:

- audit remaining manual AST construction and printer paths that still rely on compatibility adapters;
- remove the compatibility bridge once syntaxless adapter fallbacks are no longer needed;
- continue refining inheritance and overload behavior after the AST/type boundary is cleaner.

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

This phase is also the first real prerequisite for later Quark dependency support, because Pulsar needs a project-level view before Orbit can invoke it with a dependency graph.

## Phase 7 - Standard library as source

Status: **not started**.

The parser no longer hard-codes standard-library function registration. The long-term direction is for user code and standard-library code to share the same compiler path, but builtin and standard-library declarations are not modeled semantically yet.

Planned standard-package direction:

- **Core** should contain the minimal base package and fundamental declarations;
- **Solar** should cover math and numeric utilities;
- **Pulse** should cover event-driven programming, I/O, and asynchronous interaction;
- **Spectrum** should cover graphics, rendering, and GPU-oriented utilities;
- **Echo** should cover audio utilities;
- **Atlas** should cover data structures and collections.

Planned work:

- create `stdlib/` with Nova source files;
- decide how native/builtin declarations enter semantic declaration collection before source loading exists;
- preload Core through the multi-file compiler pipeline;
- represent standard-library functions as Nova source declarations where possible;
- represent native/builtin functions as declarations with backend flags;
- model optional standard packages as Quarks once package metadata exists;
- keep user code and standard-library code on the same symbol/type path.

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
Each accepted advanced feature should use its own milestone so design, syntax,
semantic analysis, diagnostics, tests, and documentation can be completed as one
focused integration track.

Planned work includes:

- advanced overload and override rules;
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

## Orbit, Nebula, and Quarks

Status: **future design item**.

Package-management work should come after the multi-file compiler pipeline is reliable.

Expected direction:

- Orbit reads project metadata and resolves dependency graphs;
- Nebula hosts published Quarks and version metadata;
- each Quark exposes source files, metadata, dependencies, and eventually build/test documentation;
- Pulsar compiles the project plus all resolved Quark dependencies using the same project-level pipeline.

This phase should not start until compilation units, project contexts, and cross-file semantic analysis are stable.

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
