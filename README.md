# Nova — Compiler Front-End
A modular, self-contained tokenizer and parser for the **Nova** programming language.

## What is Nova?
Nova is a compiled, object-oriented programming language designed around a single ambition: take everything the modern language ecosystem does well, and commit to it fully — rather than bolting it on as an afterthought.

It draws from the performance and directness of C, the natural OOP model of Java, the modularity of Python, and the package ecosystem philosophy of Ruby — while addressing what each of those leaves incomplete.
The standard library is designed to be truly exhaustive: I/O, math, string utilities, event-driven programming, GPU-dispatched graphics, audio interfaces, and more are all first-class citizens, injectable on demand, rather than delegated to fragile third-party dependencies.

Nova introduces a deliberate distinction between two fundamental concepts that most languages conflate:

- **Classes** — Java-style objects with identity, behavior, and lifecycle, inheriting from a common root that provides sensible defaults.
- **Types** — algebraic entities defined entirely by their mathematical properties, supporting operator overloading to construct structures like tensors, matrices, and other composable forms directly in the language.

A class is not a type, and a type is not a class.
This separation is a first-class design decision, not a convention.

The syntax is intentionally familiar — close to C and Java — so that the language feels natural from day one, without the cognitive overhead of learning an entirely new grammar.

Nova source files use the **`.nv`** extension. The package manager is **Orbit**, and the community package registry is **the Nebula**.

> [!NOTE]
> This repository contains the **compiler front-end**: the lexer and recursive-descent parser that produce the AST and Symbol Table from Nova source code.
> The backend (IR generation, optimization, and native code emission) is a future milestone described in the [Further development](#further-development-detailed-roadmap) section.

## Requirements
- **Maven** (see [pom.xml](pom.xml))
- **Java** (version 24+ - suggested: [**OpenJDK release 26**](https://jdk.java.net/26/) for latest features and improvements)

> [!NOTE]
> The use of newer Java versions is recommended to take advantage of the latest language features and performance improvements.
> Switching to a newer version may require updating the `source` and `target` properties in the `pom.xml` file to match the Java version you are using.
>
> Refer to the [Maven Compiler Plugin documentation](https://maven.apache.org/plugins/maven-compiler-plugin/) for details on how to configure the Java version in your project.
> Use `java -version` in your terminal to check your current Java version and ensure compatibility with the project requirements.

## Overview
A modular Java-based code analyzer that produces the *AST* (Abstract Syntax Tree) and *Symbol Table* of the source code.
Full grammar explanation in class documentation (see [ClassParser.java](src/parser/parser/ClassParser.java), [DeclarationParser.java](src/parser/parser/DeclarationParser.java) and [ExpressionParser.java](src/parser/parser/ExpressionParser.java) for details).

### Main features
- Lexical analysis
  - Source code tokenization with line escapes, space comprehension and comment avoidance
  - Token list available for parsing or direct analysis
- Source parsing
  - AST production with specific context-aware nodes
  - Support for class-level defined custom return types, with the help of a parsing-level [`TypeRegistry`](src/token/TypeRegistry.java) definition
  - Support for multidimensional array type with optional static size definition
  - Support for class inheritance and generic type parameters
  - Implementation of symbol scoping for variables, classes and functions
    - Full-depth scope tree production from global scope
    - Semantic analysis for out-of-scope symbols
  - Recursive printer helper classes for the AST and the scoped symbol table

### Example source code
```
public class TestClass {

  private int field1;
  private float field2 = 3.14;
  private int[5][] arrayField;

  public void method() {

    // List of statements...
  }
}

public class NewClass [T] :: TestClass {

  private T genericTypeField;

  public int method(int param1, int param2) { return param1 + param2; }

  public NewClass(T[10] params) {

    for (int a = 0; a < 10; a++) {
      if (a == 5) { genericTypeField = params[i]; }
    }

    /* Other statements... */
  }
}
```

## Architecture

The project is organized into four main layers: the **Lexer**, the **Parser** (composed of specialized sub-parsers), the **AST node hierarchy**, and the **Printers**.
Each layer has a well-defined responsibility and communicates with the next through clean interfaces.

### Lexer

The [`Lexer`](src/lexer/Lexer.java) performs lexical analysis, converting raw source text into a flat list of [`Token`](src/lexer/Token.java) objects.
It handles:

- **Whitespace and comment skipping** — both single-line (`//`) and multi-line (`/* */`) comments are discarded transparently.
- **Literal recognition** — numbers (with optional type suffixes `l/L`, `f/F`, `d/D`, `b/B`), strings, character literals, and booleans (`true`/`false`) are each tokenized into a dedicated [`LiteralToken`](src/lexer/token/LiteralToken.java) subtype.
- **Keyword and type disambiguation** — identifiers are checked against a static map of known keywords ([`Keyword`](src/token/family/Keyword.java), [`AccessModifier`](src/token/family/AccessModifier.java)) and primitive types ([`PrimitiveType`](src/token/family/PrimitiveType.java)), producing [`KeywordToken`](src/lexer/token/KeywordToken.java) or [`TypeToken`](src/lexer/token/TypeToken.java) respectively; unrecognized sequences become [`LiteralToken`](src/lexer/token/LiteralToken.java) identifiers.
- **Operator and delimiter tokenization** — both one- and two-character forms (e.g. `=` vs `==`, `:` vs `::`) are resolved greedily.

Every token carries its **source position** (line and column), which propagates through the parser into error messages and AST nodes.

### Token families

All token types implement the [`TokenFamily`](src/token/TokenFamily.java) marker interface, organized into enums and classes under `token/family/`:

| Type                                                     | Examples                                       |
|----------------------------------------------------------|------------------------------------------------|
| [`PrimitiveType`](src/token/family/PrimitiveType.java)   | `int`, `float`, `string`, `void`, …            |
| [`Keyword`](src/token/family/Keyword.java)               | `if`, `while`, `for`, `class`, `return`, …     |
| [`AccessModifier`](src/token/family/AccessModifier.java) | `public`, `private`, `protected`               |
| [`Operator`](src/token/family/Operator.java)             | `+`, `==`, `&&`, `++`, `+=`, …                 |
| [`Delimiter`](src/token/family/Delimiter.java)           | `(`, `{`, `[`, `;`, `::`, …                    |
| [`Literal`](src/token/family/Literal.java)               | identifiers, numbers, strings, booleans, chars |
| [`Special`](src/token/family/Special.java)               | `EOF`, `UNKNOWN`                               |

Non-primitive class names are represented at parse time by [`NonPrimitiveType`](src/token/family/NonPrimitiveType.java), and generic parameters by [`GenericParameterType`](src/token/family/GenericParameterType.java).

### Parser

The [`Parser`](src/parser/Parser.java) is the entry point for syntactic analysis.
It owns a per-session [`TypeRegistry`](src/token/TypeRegistry.java) (pre-populated with all primitive types and extended as classes are declared), a global [`SymbolTable`](src/parser/ast/SymbolTable.java), and a small set of built-in standard-library stubs (`print`, `println`, `parseInt`, etc.).
Parsing is delegated to three specialized sub-parsers:

#### DeclarationParser
[`DeclarationParser`](src/parser/parser/DeclarationParser.java) handles the top-level grammar: access-modified class declarations, function declarations (with pre-registration for recursion support), variable declarations (including comma-separated multi-variable forms), and all statement types — `if`/`else`, `while`, `for`, for-each, `switch`/`case`, `return`, `break`, `continue`, and block statements.
It also owns type parsing (`primitiveType | className arrayDimensions?`) and parameter list parsing, both used by the other sub-parsers.

#### ClassParser
[`ClassParser`](src/parser/parser/ClassParser.java) handles class body parsing.
After consuming the class header (name, optional generic parameter `[T]`, optional superclass list `:: A, B`), it registers the class early in both the `TypeRegistry` and the enclosing `SymbolTable` so that forward references within the same file resolve correctly.
It then iterates over class members, dispatching to dedicated helpers for **fields** (with optional initializers), **methods** (pre-registered before their body for recursive calls), **constructors**, and **inner classes**.

#### ExpressionParser
[`ExpressionParser`](src/parser/parser/ExpressionParser.java) implements a standard recursive-descent expression grammar with explicit precedence levels:

```
assignment → ternary → logicOr → logicAnd → equality → comparison → term → factor → unary → call → primary
```

Compound assignment operators (`+=`, `-=`, etc.) are desugared into their binary equivalents.
Postfix `++`/`--` are distinguished from prefix forms by a dedicated [`PostfixUnaryExpression`](src/parser/ast/nodes/expression/PostfixUnaryExpression.java) node.
The `primary` rule handles object creation (`new ClassName(...)`), parenthesized expressions, and all literal forms.

#### Error recovery
When a [`ParseException`](src/parser/parser/util/ParseException.java) is thrown at the top-level declaration boundary, the [`Parser`](src/parser/Parser.java) records the error and calls `synchronize()` — which skips tokens until a safe resumption point (a semicolon or a statement-opening keyword) — then continues parsing.
At the end of the run, all collected errors are bundled into a [`ParseErrorsException`](src/parser/parser/util/ParseErrorsException.java) and thrown together, giving the caller a complete picture of every problem in a single pass.

### Project orchestration (multi-file) — experimental

There is initial work toward multi-file project orchestration under [`src/parser/project`](src/parser/project).
The implementation is experimental and provides bootstrapping facilities, but is not yet a fully integrated build/compilation pipeline for large projects.

- [`ProjectParser`](src/parser/project/ProjectParser.java) — discovers `.nv` files and implements a first-pass collection for class names and top-level declarations to help resolve forward references across files. This reduces ordering constraints but does not yet implement a robust dependency solver for complex cycles.
- [`StdLibLoader`](src/parser/project/StdLibLoader.java) and [`FileSystemStdLibLoader`](src/parser/project/FileSystemStdLibLoader.java) — a pluggable mechanism to load standard library modules from the filesystem. The loader exists, but the provided standard library in `stdlib/` is minimal and primarily intended for tests and prototyping.
- [`ProjectCompiler`](src/parser/project/ProjectCompiler.java) — orchestration glue that invokes parsing, the semantic pass, and the backend scaffold. The backend currently emits pseudo-assembly for inspection only; no real code-generation backend is present.

Notes on current status:

- Multi-file support is usable for small examples and tests, but the orchestrator is not hardened for large codebases yet (ordering, mutual recursion across many files and incremental builds need work).
- The standard library is currently a small set of stubbed modules; it is not feature-complete and must be expanded and maintained as ordinary Nova source files.
- The `ProjectParser`/`ProjectCompiler` code is a good starting point for the requested multi-file parsing orchestrator, but additional integration, robust error reporting across files, and build orchestration are required before it can be considered production-ready.

### Diagnostics and recovery

Parser errors now expose structured diagnostics (`DiagnosticCode`, `SourceSpan`, `Diagnostic`) and include caret-style location markers in aggregated parse output.
Recovery has been extended in block/class/member parsing paths so multiple nested errors can be reported in one run.

### Semantic and backend foundations

- [`semantic/SemanticAnalyzer`](src/semantic/SemanticAnalyzer.java) introduces a first semantic pass (missing-return and unreachable-after-return checks).
- [`backend/`](src/backend) introduces the initial backend scaffold:
  - IR model (`IRProgram`, `IRInstruction`)
  - AST lowering (`AstLowerer`)
  - optimization hook (`Optimizer`)
  - pseudo-assembly emission (`PseudoAssemblyEmitter`)

### Symbol Table

The [`SymbolTable`](src/parser/ast/SymbolTable.java) is a tree of scopes.
Each scope holds a flat list of [`Symbol`](src/parser/ast/nodes/Symbol.java) entries (variables, functions, classes, parameters) and a reference to its parent.
Scopes are created on entry to every block, function, method, constructor, loop, and class body, and restored on exit — always via `try/finally` blocks to guarantee correct scope restoration even when parsing fails mid-construct.
Symbol lookup walks up the parent chain, enabling full lexical scoping; re-declaration within the same scope is a hard parse error.

### Type Registry

The [`TypeRegistry`](src/token/TypeRegistry.java) is a per-session catalogue of all known types.
It is pre-populated with the primitive types at construction and extended with each new [`ClassDeclarationStatement`](src/parser/ast/nodes/statement/ClassDeclarationStatement.java) as it is parsed.
The parser consults the registry to distinguish class names from plain identifiers during type parsing and `new` expressions, and to resolve superclass names in inheritance declarations.

### AST node hierarchy

All AST nodes extend [`AstNode`](src/parser/ast/AstNode.java) (carrying source position) and implement [`Printable`](src/parser/ast/Printable.java) (exposing a label and a list of [`PrintEntry`](src/parser/ast/Printable.java) values for tree rendering).
The hierarchy is split into two branches:

- **[`StatementNode`](src/parser/ast/nodes/StatementNode.java)** — declarations (`VariableDeclarationStatement`, `FunctionDeclarationStatement`, `ClassDeclarationStatement`, and their class-member variants), control-flow statements (`IfStatement`, `WhileStatement`, `ForStatement`, `ForEachStatement`, `SwitchStatement`), and structural nodes (`BlockStatement`, `ReturnStatement`, `BreakStatement`, `ContinueStatement`).
- **[`ExpressionNode`](src/parser/ast/nodes/ExpressionNode.java)** — literals (`NumberLiteralExpression` and its typed subclasses, `StringLiteralExpression`, `BoolLiteralExpression`, `CharLiteralExpression`, `NullLiteralExpression`, `IdentifierLiteralExpression`), operations (`BinaryExpression`, `UnaryExpression`, `PostfixUnaryExpression`, `TernaryExpression`, `AssignmentExpression`), and access forms (`CallExpression`, `ArrayAccessExpression`, `MemberAccessExpression`, `ObjectCreationExpression`).

### Printers

Two utility classes render the internal structures to standard output for inspection and debugging:

- **[`AstPrinter`](src/printer/AstPrinter.java)** — recursively walks the AST using each node's `Printable` interface, rendering the tree with `├─` / `└─` / `│` branch characters and managing indentation centrally. Array type annotations (e.g. `int[3][]`) are formatted by the shared `buildTypeStringWithSizes` helper.
- **[`SymbolTablePrinter`](src/printer/SymbolTablePrinter.java)** — walks the `SymbolTable` scope tree, printing each symbol with its kind and declared type, and attaching child scopes (function bodies, class bodies, anonymous blocks) as indented subtrees beneath their owning symbol.

## Further development (detailed roadmap)

This section documents missing features, current partial implementations, and a recommended sequence of work items to bring the front-end and the rest of the toolchain to a usable, multi-file compilation system.

1) Error handling (current state: partial)
   - Current: top-level synchronization and multi-error aggregation is implemented. Parser exposes structured diagnostics (`DiagnosticCode`, `SourceSpan`) and caret-style markers for aggregated outputs.
   - Missing / next steps:
      - Extend recovery to finer-grained units inside expressions and statements so more errors can be reported in a single parse pass.
      - Improve semantic diagnostics: type mismatch messages, expected vs actual type hints, candidate suggestions for unresolved symbols, and richer context for overload/duplicate definitions.
      - Produce editor-friendly diagnostic outputs (JSON/LS-style) to integrate with language servers and IDEs.
   - Proposed implementation plan (diagnostics expansion):
      - Phase 1 — Diagnostics pipeline hardening: normalize parser/semantic diagnostic payloads into a single transport model (`severity`, `code`, `message`, `span`, `context`, optional `relatedSpans`) and keep deterministic ordering for stable tests/tooling.
      - Phase 2 — Warning detection: add a semantic warning pass (unused variables/parameters, unreachable code, shadowing, redundant/null checks) with configurable warning levels.
      - Phase 3 — Suggestions (depends on warnings): attach machine-readable suggestions to each warning/error (e.g. rename candidate, remove dead statement, add missing return) and include a compilable replacement preview where possible.
      - Phase 4 — IDE/CLI delivery: expose diagnostics + suggestions in both human-readable CLI output and JSON protocol output for editor integration and auto-fix workflows.

2) Multi-file parsing & build orchestration (current state: experimental)
   - Current: `src/parser/project` contains `ProjectParser`, `StdLibLoader`, and `ProjectCompiler` as bootstrapping pieces.
   - Missing / next steps:
     - Implement a robust discovery and dependency resolution phase: collect exports/types per file, build a file dependency graph, and compute a safe parse/compile order. Support cycles via a two-pass strategy (declaration collection pass + definition pass).
     - Introduce a workspace builder that can accept a set of source roots and a standard-library root, and feed files to the lexer/parser with a shared `TypeRegistry` and `SymbolTable` session context.
     - Add incremental build support (timestamp or hash-based) so large projects don't require full rebuilds every change.
     - Provide a stable CLI entrypoint for `nova build` / `nova check` that uses the orchestrator.

3) Standard library (current state: minimal stubs)
   - Current: a handful of built-in stubs are available (e.g., `print`, `println`, `parseInt`) and `stdlib/` contains a few prototype modules used by tests.
   - Missing / next steps:
     - Author stdlib modules in Nova source and place them under `stdlib/` (I/O, collections, math, strings, concurrency primitives, file system, process, etc.).
     - Ensure the orchestrator preloads stdlib modules before parsing user code (so types and functions are globally available without imports).
     - Define a module/package manifest format (e.g., `module.nvm` or metadata comments) to allow selective preloading and easy extension.

4) Code generation and runtime (current state: none / scaffold only)
   - Current: a backend scaffold exists in `backend/` (IR model, lowering pass, optimizer hook, pseudo-assembly emitter) intended for experiments and prototyping.
   - Missing / next steps:
     - Implement a deterministic AST → IR lowering pass (`AstLowerer`) that emits a stable IR suitable for optimization.
     - Implement a minimal IR optimizer (constant folding, dead code elimination, simple inlining) and add regression tests.
     - Provide at least one backend target: JVM bytecode (fast path using ASM or similar) or a small custom VM with a bytecode interpreter. *Long-term*: native code via LLVM or a custom codegen pipeline.
     - Design and ship a small runtime library for operations not expressible in Nova source (I/O, memory management hooks, platform interop).

5) Testing, CI and developer ergonomics
   - Add more unit and integration tests around multi-file parsing, stdlib loading, semantic checks, and backend lowering.
   - Provide sample multi-file projects in `examples/` that exercise cross-file references, generics, and stdlib usage.
   - Add a reproducible CI job that runs the full parse+semantic pipeline and the backend scaffold to catch regressions early.

6) Migration & compatibility notes
   - Keep `Parser`-level hard-coded stubs only as a compatibility shim while migrating stdlib pieces into source modules.
   - Prefer progressively deprecating hard-coded behavior in favor of source-defined stdlib modules.

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request with your changes.
For major changes, please open an issue first to discuss what you would like to change.

## License
This project is licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License** (CC BY-NC-SA 4.0).
See the [LICENSE](LICENSE) file for details.
