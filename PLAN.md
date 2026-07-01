# Nova Compiler Upgrade Plan

Last updated: 2026-06-30

This file tracks the current upgrade path for the Nova compiler front end. The goal is to keep the project moving in small, testable steps while preserving the existing recursive-descent architecture until there is a clear reason to replace it.
The goal of these incremental steps is to guide the implementation plan for the Nova compiler front end, while conserving a list of committable upgrades that can follow a clear path toward a more robust, testable, and maintainable compiler.
In this sense, the plan is a living document that can be updated as the project progresses - and every step should be addressed with a Git commit that is small enough to be reviewed and tested in isolation.

## Instructions To Use

- Keep this plan aligned with the actual implementation state before starting a new task and after completing one.
- Keep documentation updated with each meaningful change, including `README.md`, the Markdown reference files in `docs/`, relevant `package-info.java` files, and this plan.
- Use the GitHub Project and open issues as the source of truth for implementation workflow: select the issue that represents the current task, keep its metadata current, and let it guide the branch/commit scope.
- If a new sub-task is required to complete an issue, create a dedicated sub-issue that references the parent issue before implementing it. This keeps the feature history explicit and makes it clear which steps were needed to complete the parent.
- Keep commits narrow and issue-scoped. When a branch addresses multiple issues, split the commits so each commit has one clear responsibility.
- Use dedicated pull requests for advanced language features. Each PR should focus on mastering the integration of one language property across syntax, AST shape, semantic analysis, diagnostics, tests, and documentation before moving to the next property.

## Current Status

Current focus: Phase 6 - multi-file project pipeline design.

| Phase                         | Status      | Summary                                                                                                                                                                             |
|-------------------------------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1. Build health               | Complete    | Maven wrapper and baseline test flow are restored.                                                                                                                                  |
| 2. Parser semantics           | Complete    | Parser cursor contract, expression parsing, parser package layout, recovery, and class grammar have been tightened.                                                                 |
| 3. Diagnostics                | Complete    | Lexer and parser diagnostics now share a structured model without global error state or legacy error wrappers.                                                                      |
| 4. Semantic analysis split    | Complete    | Parser-generated ASTs are syntax-only; semantic declaration collection, scopes, diagnostics, type checks, return/l-value checks, and loop-control checks own meaning.              |
| 5. Type model                 | Complete    | Declaration AST nodes expose parsed type syntax directly; semantic symbols distinguish Nova type categories, and syntaxless `ReturnType` fallback is isolated.                     |
| 6. Multi-file pipeline        | In progress | Project-level compiler contracts are being designed before implementation begins.                                                                                                   |
| 7. Standard library as source | Not started | Parser hard-coded builtins are gone; semantic builtin declarations and source loading are not implemented.                                                                          |
| 8. IR preparation             | Not started | No backend-neutral lowered representation yet.                                                                                                                                      |
| 9. Advanced Nova features     | Not started | Generics, lambdas, monomorphization, and related features should wait.                                                                                                              |

## Working Rules

- Keep `./mvnw test` green after each step.
- Prefer narrow commits that each improve one behavior or one layer.
- Add or update tests before changing behavior when the expected behavior is not already covered.
- Keep parsing syntactic. Name resolution, type checks, l-value checks, duplicate checks, and context checks should move toward semantic passes.
- Do not start advanced language features until diagnostics, parsing stability, and semantic separation are reliable.
- Keep source and test packages grouped by compiler layer so ownership boundaries stay visible.

## Repository Organization

Status: Current.

- [x] Parser grammar implementation lives in `parser.grammar`.
- [x] Parser cursor and shared parser helpers live in `parser.support`.
- [x] Structured diagnostics and parse exceptions live in `error.diagnostic`.
- [x] Legacy `error.Error`, `error.syntax`, and `error.parsing` wrappers have been removed.
- [x] Tests are grouped by compiler layer: `lexer`, `parser`, `semantic`, `error.diagnostic`, and `integration`.
- [x] Semantic source and test packages are split into `semantic.analysis`, `semantic.declaration`, `semantic.scope`, and `semantic.type`.
- [x] Parser-owned `SymbolTable` scope construction and symbol registration have been removed; semantic scopes now own name visibility.
- [x] GitHub issue milestones now own roadmap grouping for Project automation; Phase 1-8 work is tracked under `Nova MVP compiler`, Phase 9 work uses dedicated advanced-feature milestones, and custom Project `Phase`/`Kind` duplicates are removed in favor of milestones and labels.
- [x] GitHub issue creation uses YAML issue forms so metadata is selected through structured fields before automation syncs milestones, labels, and Project fields.
- [x] Closed issues marked `Done` can be archived from the roadmap Project, while reopened/open issues can be made visible again without changing their historical status.

## Phase 1 - Restore Build Health

Status: Complete.

Goal: make the current branch compile and be testable from a clean checkout.

Completed:

- [x] Add and commit the Maven wrapper: `mvnw`, `mvnw.cmd`, `.mvn/`.
- [x] Restore parser cursor helpers: `peek`, `previous`, `advance`, `check`, `match`, `consume`.
- [x] Remove or bypass duplicate/unused error paths that blocked compilation.
- [x] Restore local `javac` and Maven test execution.
- [x] Update README references that drifted from current names, such as `TokenFamily` versus `TokenClass`.

Exit criteria:

- [x] A clean clone can run `./mvnw test`.
- [x] Parser classes compile without cursor API confusion.
- [x] Tests can be used as the baseline for future phases.

## Phase 2 - Stabilize Parser Semantics

Status: Complete.

Goal: make parsing predictable, syntactic, and stable under invalid input.

Completed:

- [x] Restore a clear parser cursor contract.
- [x] Keep `check(...)` as inspection only.
- [x] Keep `match(...)` as conditional consume.
- [x] Keep `consume(...)` as required-token handling.
- [x] Remove direct `ErrorCollector` writes from `ExpressionParser`.
- [x] Tighten assignment target parsing with explicit assignable-target checks.
- [x] Add expression AST-shape tests for the updated parsing behavior.
- [x] Split parser implementation packages into grammar parsers and shared parser support.
- [x] Audit parser null-valued paths; current `null` uses represent optional syntax rather than parse-failure sentinels.
- [x] Keep parser failure handling based on `ParseException` plus explicit recovery boundaries; do not introduce AST error nodes yet.
- [x] Add class-body member recovery so malformed class members do not discard the whole class AST.
- [x] Add regression tests for class-member token advancement and recovery.
- [x] Review recovery loops that inspected tokens with `check(...)` before local recovery catches.
- [x] Add block-level unknown-token recovery that preserves valid statements before and after the error.
- [x] Review class parsing against the current language design and parser tests.
- [x] Require explicit access modifiers on class fields, methods, constructors, and inner classes.
- [x] Add class-member recovery coverage for missing access modifiers.
- [x] Split class parsing into smaller header, body, member, and AST-population helpers.

Remaining:

- [x] No planned parser-semantics cleanup remains before the TypeRegistry/type-model decision.

Exit criteria:

- [x] Lexer/parser tests pass after parser-failure cleanup.
- [x] Bad syntax reports diagnostics without corrupting parser state.
- [x] Valid class, function, statement, and expression examples produce stable ASTs.
- [x] Parser behavior is syntactic; semantic validation is clearly deferred.

## Phase 3 - Redesign Diagnostics

Status: Complete.

Goal: replace scattered/global error handling with structured, deterministic compiler diagnostics.

Completed:

- [x] Improve `UnexpectedTokenError` with expected/actual token context.
- [x] Improve `UnrecognizedTokenError` with raw lexeme support and token constructors.
- [x] Preserve raw lexemes for lexer `UNKNOWN` tokens where available.
- [x] Cover empty and unterminated character literals in lexer edge-case tests.
- [x] Cover unknown single-character tokens in lexer edge-case tests.
- [x] Cover unterminated string literals and unterminated string escapes in lexer edge-case tests.
- [x] Make parser handling of `Special.UNKNOWN` consistently produce `UnrecognizedTokenError`.
- [x] Add parser/integration tests for unknown-token recovery.
- [x] Add `Diagnostic`, `DiagnosticBag`, `DiagnosticSeverity`, and `DiagnosticPhase`.
- [x] Expose structured diagnostics from parse exceptions without replacing existing call sites.
- [x] Add a parser-owned `DiagnosticBag` populated during parse recovery.
- [x] Report parser diagnostics from the CLI without reading the static `ErrorCollector`.
- [x] Build structured expected/actual diagnostics for required-token parser failures.
- [x] Add a shared parser helper for structured parse errors and migrate `ExpressionParser`.
- [x] Migrate `DeclarationParser` direct parse errors to structured diagnostics.
- [x] Migrate `ClassParser` direct parse errors to structured diagnostics.
- [x] Pass diagnostics through lexer runs instead of relying only on token recovery.
- [x] Remove the unused static `ErrorCollector` implementation.
- [x] Centralize parser error reporting in `ParserState` to prepare nested recovery.
- [x] Add block-level statement recovery for declaration lists inside braces.
- [x] Move parse exceptions into the diagnostic package.
- [x] Remove the old error wrapper hierarchy in favor of direct structured diagnostics.

Next:

- [x] Store line, column, optional span, message, expected token, and actual token in the diagnostic model.
- [x] Start with top-level recovery, then add block/statement recovery.
- [x] Continue diagnostics work in Phase 4 only where semantic diagnostics need the same model.

Exit criteria:

- [x] A compiler-front-end run returns AST plus diagnostics, or one aggregate diagnostic failure.
- [x] Multiple syntax errors are reported deterministically.
- [x] Diagnostics can be tested without global state leaks.
- [x] Lexer and parser diagnostics share one data model.

## Phase 4 - Split Semantic Analysis From Parsing

Status: Complete.

Goal: make the parser build syntax only, then validate meaning through semantic passes.

Tasks:

- [x] Stop resolving undefined variables directly in expression parsing.
- [x] Stop resolving undefined classes directly in expression parsing.
- [x] Add declaration collection.
- [x] Add scope construction.
- [x] Add name resolution.
- [x] Add duplicate declaration validation.
- [x] Add initial assignment and initializer type checking.
- [x] Expand type checking across calls, member access, arrays, inheritance, and overload rules.
- [x] Add return checking.
- [x] Add l-value checking.
- [x] Add `break`/`continue` context checking.
- [x] Move symbol-table validation out of parser code where possible.
- [x] Keep parser-generated AST simple and complete.

Parser-owned semantic checks inventory:

- [x] Name resolution: `ExpressionParser` no longer rejects unresolved identifiers through `symbolTable.lookup(...)`.
- [x] Type/name resolution: `ExpressionParser` no longer rejects `new UndefinedClass()` through `typeRegistry.isCustomType(...)`.
- [x] Declaration inventory: semantic declaration collection records classes, functions, methods, fields, parameters, variables, constructors, and for-each variables.
- [x] Scope model: semantic scope construction creates global, class, function, constructor, block, switch, and loop scopes from the AST.
- [x] Name resolution pass: unresolved expression identifiers and constructed class names produce semantic diagnostics.
- [x] Duplicate validation pass: repeated non-constructor declarations in the same semantic scope produce semantic diagnostics.
- [x] Type checking pass: initializer and assignment type mismatches produce semantic diagnostics for value and constructed class types.
- [x] Type checking pass: identifier-based function calls validate argument count and argument types, and expose their declared return type to surrounding expressions.
- [x] Type checking pass: array access validates integer indexes, rejects non-array targets, and exposes element types to surrounding expressions.
- [x] Type checking pass: direct class field access and direct class method calls validate member existence, target type, argument count, and argument types.
- [x] Type checking pass: class subtype assignment and argument compatibility follow resolved superclass chains.
- [x] Type checking pass: inherited class fields and methods are visible to member access and method-call analysis.
- [x] Type checking pass: function and method overload calls select the best parameter-type match and report no-match or ambiguity diagnostics.
- [x] Duplicate validation pass: function, method, and constructor overloads are grouped by parameter signature, while repeated signatures remain semantic diagnostics.
- [x] Return checking pass: invalid return placement, value presence, and simple missing-return cases produce semantic diagnostics.
- [x] Loop-control checking pass: invalid `break` and `continue` placement produces semantic diagnostics.
- [x] Type/name resolution: `ClassParser` no longer rejects unknown superclasses; semantic name resolution reports them.
- [x] Declaration validation: duplicate declarations are no longer rejected during parsing; semantic duplicate validation reports them.
- [x] L-value validation: `ExpressionParser` no longer rejects invalid assignment targets; semantic l-value checking reports them.
- [x] Type syntax/resolution coupling: `DeclarationParser.parseTypeSyntax()` accepts identifier type syntax; semantic name resolution reports unknown types.
- [x] Scope construction coupling: declaration and class parsers no longer build parser symbol-table scopes or register symbols while parsing syntax.
- [x] TypeRegistry boundary decision: expression parsing no longer depends on parser type metadata, and `TypeRegistry` is not used for semantic validation.
- [x] Type syntax nodes: declaration/class type parsing now builds parsed `TypeSyntax` nodes for declared source types.
- [x] Type syntax exposure: declaration AST nodes and semantic declarations expose parsed `TypeSyntax` directly while keeping `ReturnType` compatibility getters.
- [x] Type syntax construction: declaration/class parsing now passes parsed `TypeSyntax` directly into declaration AST constructors instead of building parser-owned `ReturnType` adapters.
- [x] Semantic type resolution: name resolution now resolves declared types through semantic `TypeSymbol` objects.
- [x] TypeRegistry adapter removal: declaration/class parsing no longer uses a parser-side type registry.
- [x] Parser-generated AST boundary audit: unresolved names, invalid assignment targets, duplicate declarations, unknown declared types, and unknown superclasses remain parser-accepted syntax with semantic-owned meaning.

Exit criteria:

- [x] `unknownVar + 1;` parses syntactically, then fails semantic name resolution.
- [x] `1 = 2;` parses syntactically, then fails l-value analysis.
- [x] Duplicate declarations become semantic diagnostics, not parser crashes.
- [x] Parser semantic-boundary regression tests preserve AST shape for representative semantically invalid programs.

## Phase 5 - Introduce A Real Type Model

Status: Complete.

Goal: stop using lexer token classes as the semantic type model.

Tasks:

- [x] Add parsed type syntax nodes, such as `TypeSyntax`, `ArrayTypeSyntax`, and `GenericTypeSyntax`.
- [x] Add resolved semantic type symbols, such as `ValueTypeSymbol`, `ClassTypeSymbol`, and `GenericParameterSymbol`.
- [x] Resolve declared type names through semantic type symbols during name resolution.
- [x] Keep legacy `ReturnType` adapters able to expose source `TypeSyntax` while downstream compatibility paths are retired.
- [x] Migrate type checking to semantic type symbols instead of `ReturnType` token-class comparisons.
- [x] Keep `ReturnType` as a source-syntax-first compatibility adapter alongside direct `TypeSyntax` AST APIs.
- [x] Expose parsed `TypeSyntax` directly from declaration AST nodes and semantic declarations.
- [x] Resolve semantic declaration types from `TypeSyntax` first, with `ReturnType` only as a compatibility fallback.
- [x] Remove parser `TypeRegistry` once parsed type syntax nodes can preserve class/generic metadata without it.
- [x] Model Nova classes and Nova value/math types separately, matching the README design.
- [x] Isolate syntaxless `ReturnType` fallback conversion in `semantic.type.ReturnTypeSyntaxBridge`.
- [x] Add regression tests for semantic type categories: value/math, class/object, array, generic-parameter, and unknown.
- [x] Document the `TypeSyntax` to `TypeSymbol` boundary for parser-owned syntax and semantic-owned meaning.

Exit criteria:

- [x] Type names can be parsed before they are resolved.
- [x] Forward and mutual references become possible.
- [x] Semantic analysis no longer reads lexer token classes outside the isolated `ReturnType` compatibility bridge.
- [x] Declaration AST constructors no longer require `ReturnType` compatibility adapters.
- [x] Semantic type-category behavior is covered by focused regression tests.
- [x] The parser/semantic type boundary is documented with examples.

## Phase 6 - Multi-File Project Pipeline

Status: In progress.

Goal: move from a single-file parser to a project compiler front end.

Tasks:

- [x] Design the Phase 6 project pipeline contracts for `Compiler`, `SourceFile`, `CompilationUnit`, and `ProjectContext`.
- [x] Add `SourceFile`.
- [x] Add `CompilationUnit`.
- [x] Add a file-aware diagnostic wrapper for project-level aggregation.
- [ ] Add `Compiler`.
- [ ] Add `ProjectContext`.
- [ ] Lex all files before semantic analysis.
- [ ] Parse all files before semantic analysis.
- [ ] Collect declarations across files.
- [ ] Resolve names and types across files.
- [ ] Run semantic checks across the project.
- [ ] Add package/import placeholders, even if package semantics are minimal at first.

Exit criteria:

- [ ] Two `.nv` files can reference each other.
- [ ] Classes can be discovered before method bodies are analyzed.
- [ ] The standard library can later plug into the same path.

## Phase 7 - Standard Library As Source

Status: Not started.

Goal: model the standard library through declarations instead of parser-owned hard-coded behavior.

Tasks:

- [ ] Create `stdlib/` with Nova source files.
- [ ] Decide how native/builtin declarations enter semantic declaration collection before source loading exists.
- [ ] Preload standard-library source through the multi-file compiler pipeline.
- [ ] Represent native/builtin functions as declarations with backend flags.
- [ ] Add later tests for `print`, `println`, `parseInt`, collections, and other standard-library entries.

Exit criteria:

- [x] `Parser` no longer registers standard-library functions manually.
- [ ] User code and standard-library code share the same symbol/type path.

## Phase 8 - IR Preparation

Status: Not started.

Goal: prepare for a backend without jumping straight to native code generation.

Tasks:

- [ ] Add a lowered IR independent of the source AST.
- [ ] Lower literals.
- [ ] Lower variables.
- [ ] Lower assignments.
- [ ] Lower blocks.
- [ ] Lower `if` and `while`.
- [ ] Lower function calls.
- [ ] Add a basic control-flow graph later.
- [ ] Keep optimizations separate from lowering.

Exit criteria:

- [ ] A valid semantic AST can lower to IR.
- [ ] An IR printer exists.
- [ ] Simple functions and classes can be represented backend-neutrally.

## Phase 9 - Advanced Nova Features

Status: Not started.

Goal: add ambitious language features only after the core front-end pipeline is stable.

Tasks:

- [ ] Advanced overload and override rules.
- [ ] Access control.
- [ ] Inheritance conflict checks.
- [ ] Generics.
- [ ] Bounded generics.
- [ ] Class parameters.
- [ ] Operator-overloadable Nova types.
- [ ] Lambdas.
- [ ] Variadic generics.
- [ ] Monomorphization.

Exit criteria:

- [ ] Earlier phases are complete enough that advanced features can be added without being coupled to parser recovery or global diagnostics.
- [ ] Variadic generics and lambdas are designed as one major milestone, not as disconnected parser sugar.

## Immediate Next Steps

1. Define the two-file cross-reference acceptance test that will prove the Phase 6 pipeline works without single-file shortcuts.
2. Implement the `ProjectContext` and `Compiler` orchestration layer tracked by issue #72.
