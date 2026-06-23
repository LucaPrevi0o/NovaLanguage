# Nova Compiler Upgrade Plan

Last updated: 2026-06-23

This file tracks the current upgrade path for the Nova compiler front end. The goal is to keep the project moving in small, testable steps while preserving the existing recursive-descent architecture until there is a clear reason to replace it.
The goal of these incremental steps is to guide the implementation plan for the Nova compiler front end, while conserving a list of committable upgrades that can follow a clear path toward a more robust, testable, and maintainable compiler.
In this sense, the plan is a living document that can be updated as the project progresses - and every step should be addressed with a Git commit that is small enough to be reviewed and tested in isolation.

## Current Status

Current focus: Phase 4 - semantic analysis split.

| Phase                         | Status      | Summary                                                                                                                         |
|-------------------------------|-------------|---------------------------------------------------------------------------------------------------------------------------------|
| 1. Build health               | Complete    | Maven wrapper and baseline test flow are restored.                                                                              |
| 2. Parser semantics           | In progress | Parser cursor contract and expression parsing have been tightened, but recovery and parser/semantic separation still need work. |
| 3. Diagnostics                | Complete    | Lexer and parser diagnostics now share a structured model without global error state.                                           |
| 4. Semantic analysis split    | In progress | Parser-owned semantic checks are being identified before moving them into semantic passes.                                      |
| 5. Type model                 | Not started | Lexer token classes are still used too deeply as semantic type representation.                                                  |
| 6. Multi-file pipeline        | Not started | Current compiler flow is still single-file oriented.                                                                            |
| 7. Standard library as source | Not started | Builtins are still registered manually.                                                                                         |
| 8. IR preparation             | Not started | No backend-neutral lowered representation yet.                                                                                  |
| 9. Advanced Nova features     | Not started | Generics, lambdas, monomorphization, and related features should wait.                                                          |

## Working Rules

- Keep `./mvnw test` green after each step.
- Prefer narrow commits that each improve one behavior or one layer.
- Add or update tests before changing behavior when the expected behavior is not already covered.
- Keep parsing syntactic. Name resolution, type checks, l-value checks, duplicate checks, and context checks should move toward semantic passes.
- Do not start advanced language features until diagnostics, parsing stability, and semantic separation are reliable.

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

Status: In progress.

Goal: make parsing predictable, syntactic, and stable under invalid input.

Completed:

- [x] Restore a clear parser cursor contract.
- [x] Keep `check(...)` as inspection only.
- [x] Keep `match(...)` as conditional consume.
- [x] Keep `consume(...)` as required-token handling.
- [x] Remove direct `ErrorCollector` writes from `ExpressionParser`.
- [x] Tighten assignment target parsing with explicit assignable-target checks.
- [x] Add expression AST-shape tests for the updated parsing behavior.

Remaining:

- [ ] Audit parser methods that still return `null` after parse failures.
- [ ] Decide whether those paths should throw `ParseException`, return an error node, or recover at a higher boundary.
- [ ] Add tests specifically for token advancement and parser recovery bugs.
- [ ] Review class parsing against the previous working shape and current README grammar.
- [ ] Keep grammar methods small enough that cursor movement is obvious from local code.

Exit criteria:

- [ ] Lexer/parser tests pass after parser-failure cleanup.
- [ ] Bad syntax reports diagnostics without corrupting parser state.
- [ ] Valid class, function, statement, and expression examples produce stable ASTs.
- [ ] Parser behavior is syntactic; semantic validation is clearly deferred.

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

Status: In progress.

Goal: make the parser build syntax only, then validate meaning through semantic passes.

Tasks:

- [ ] Stop resolving undefined variables and classes directly in expression parsing.
- [ ] Add declaration collection.
- [ ] Add scope construction.
- [ ] Add name resolution.
- [ ] Add duplicate declaration validation.
- [ ] Add type checking.
- [ ] Add return checking.
- [ ] Add l-value checking.
- [ ] Add `break`/`continue` context checking.
- [ ] Move symbol-table validation out of parser code where possible.
- [ ] Keep parser-generated AST simple and complete.

Parser-owned semantic checks inventory:

- [ ] Name resolution: `ExpressionParser` rejects unresolved identifiers through `symbolTable.lookup(...)`.
- [ ] Type/name resolution: `ExpressionParser` rejects `new UndefinedClass()` through `typeRegistry.isCustomType(...)`.
- [ ] Type/name resolution: `ClassParser` rejects unknown superclasses while parsing inheritance lists.
- [ ] Declaration validation: `SymbolTable.register(...)` rejects duplicate declarations during parsing.
- [ ] L-value validation: `ExpressionParser` rejects invalid assignment targets during parsing.
- [ ] Type syntax/resolution coupling: `DeclarationParser.parseType()` requires class names to already be registered.
- [ ] Scope construction coupling: declaration and class parsers build scopes and register symbols while parsing syntax.

Exit criteria:

- [ ] `unknownVar + 1;` parses syntactically, then fails name resolution.
- [ ] `1 = 2;` parses syntactically, then fails l-value analysis.
- [ ] Duplicate declarations become semantic diagnostics, not parser crashes.

## Phase 5 - Introduce A Real Type Model

Status: Not started.

Goal: stop using lexer token classes as the semantic type model.

Tasks:

- [ ] Add parsed type syntax nodes, such as `TypeSyntax`, `ArrayTypeSyntax`, and `GenericTypeSyntax`.
- [ ] Add resolved semantic type symbols, such as `PrimitiveTypeSymbol`, `ClassTypeSymbol`, and `GenericParameterSymbol`.
- [ ] Keep `ReturnType` only as a temporary adapter, or replace it fully.
- [ ] Model Nova classes and Nova types separately, matching the README design.

Exit criteria:

- [ ] Type names can be parsed before they are resolved.
- [ ] Forward and mutual references become possible.
- [ ] Semantic types no longer depend on lexer token classes.

## Phase 6 - Multi-File Project Pipeline

Status: Not started.

Goal: move from a single-file parser to a project compiler front end.

Tasks:

- [ ] Add `Compiler`.
- [ ] Add `SourceFile`.
- [ ] Add `CompilationUnit`.
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

Goal: remove hard-coded standard-library function registration from `Parser`.

Tasks:

- [ ] Create `stdlib/` with Nova source files.
- [ ] Preload standard-library source through the multi-file compiler pipeline.
- [ ] Represent native/builtin functions as declarations with backend flags.
- [ ] Add later tests for `print`, `println`, `parseInt`, collections, and other standard-library entries.

Exit criteria:

- [ ] `Parser` no longer registers standard-library functions manually.
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

- [ ] Method overloading by signature.
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

1. Start moving name-resolution checks out of `ExpressionParser`.
2. Decide whether `error.Error` legacy wrappers should remain as compatibility adapters once semantic diagnostics exist.
3. Move duplicate declaration validation out of `SymbolTable.register(...)` after declaration collection exists.
