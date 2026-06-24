# Compiler architecture

This document describes the current Nova compiler-front-end architecture.

For the implementation roadmap, see [`../PLAN.md`](../PLAN.md). For the language vision, see [`language-design.md`](language-design.md).

## Current pipeline

The repository currently implements a single-file compiler-front-end pipeline:

```text
Nova source
  ↓
Lexer
  ↓
Token stream + lexer diagnostics
  ↓
Parser
  ↓
AST + parser diagnostics
  ↓
Semantic declaration collection
  ↓
Semantic scope construction
  ↓
Semantic analysis passes
  ↓
Semantic diagnostics
```

There is no backend-neutral IR, optimizer, or native code generator yet.

## Main layers

### Lexer

The lexer turns source text into tokens and reports lexical diagnostics. It should not know about AST nodes, scopes, semantic declarations, or type checking.

Responsibilities:

- recognize keywords, identifiers, literals, operators, delimiters, and special tokens;
- preserve enough source position information for diagnostics;
- recover from invalid characters or malformed literals where possible;
- produce a token stream for the parser.

### Parser

The parser is a recursive-descent parser. Its job is to recognize syntax and build AST nodes.

Responsibilities:

- consume the token stream according to the Nova grammar;
- build statement and expression AST nodes;
- recover from syntax errors where possible;
- report parser diagnostics through the shared diagnostic model;
- avoid semantic checks whenever possible.

The parser is currently split into grammar-focused helpers such as declaration, class, and expression parsing.

### AST

The AST is the source-level representation produced by the parser. It should represent what the user wrote, not a lowered or optimized form.

Responsibilities:

- preserve syntactic structure;
- keep statement and expression shapes explicit;
- provide enough information for semantic passes;
- avoid becoming a backend IR.

### Semantic declarations and scopes

After parsing, semantic passes collect declarations and construct lexical scopes from the AST.

Responsibilities:

- represent declarations independently from parser helper state;
- create global, class, function, constructor, block, loop, switch, and branch scopes;
- allow later analysis passes to resolve names and validate language rules.

### Semantic analysis

Semantic analysis validates meaning after syntax has been parsed.

Current semantic responsibilities include, or are being moved toward:

- name resolution;
- unknown type/name diagnostics;
- duplicate declaration validation;
- initial type checking;
- l-value validation;
- return checking;
- loop-control checking.

Future responsibilities include:

- complete type checking;
- inheritance validation;
- overload resolution;
- access-control checks;
- generic constraints;
- standard-library integration through source declarations.

## Important architectural boundary

The parser should answer:

> Is this valid Nova syntax?

Semantic analysis should answer:

> Does this syntactically valid Nova program make sense?

Examples:

```nova
unknownVariable + 1;
```

This should parse as an expression statement, then fail during semantic name resolution.

```nova
1 = 2;
```

This should parse as an assignment expression, then fail during semantic l-value analysis.

```nova
public class A {}
public class A {}
```

This should parse as two class declarations, then fail during duplicate declaration validation.

> [!IMPORTANT]
> This boundary is intentional architecture.
> Parser changes should not reintroduce checks for undefined names, duplicate declarations, invalid assignment targets, type mismatches, overload resolution, access control, or inheritance validity.
> Those checks belong in semantic passes so the front end can produce a complete AST and deterministic diagnostics.

## Scope ownership

Parser-owned `SymbolTable` scope construction has been removed.

The parser now builds AST nodes and parser diagnostics. It does not register variables, functions, classes, methods, fields, parameters, or local block declarations into parser scopes.

Semantic declaration collection and semantic scope construction are the source of truth for lexical visibility and language meaning.

One transitional parser dependency remains: declaration and class parsing still use `TypeRegistry` as a parse-session adapter for `ReturnType` metadata. Expression parsing no longer depends on it, and it must not be used for semantic validation. Phase 5 should replace this remaining adapter with parsed type syntax nodes and resolved semantic type symbols.

## Future architecture

The planned long-term compiler pipeline is:

```text
Project source files
  ↓
Lexer per source file
  ↓
Parser per source file
  ↓
Compilation unit ASTs
  ↓
Project-level declaration collection
  ↓
Project-level semantic analysis
  ↓
Lowered IR
  ↓
Optimization passes
  ↓
Backend code generation
```

The next major architecture milestones are:

1. introduce a real semantic type model;
2. introduce a multi-file compilation pipeline;
3. load the standard library as Nova source;
4. lower semantically valid ASTs into an IR;
5. add a backend after the front end is stable.
