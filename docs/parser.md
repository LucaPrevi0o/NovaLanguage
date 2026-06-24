# Parser

The Nova parser is a recursive-descent parser that consumes lexer tokens and builds the AST.

For the general architecture, see [`architecture.md`](architecture.md). For semantic passes, see [`semantic-analysis.md`](semantic-analysis.md).

## Responsibility

The parser should validate **syntax**, not meaning.

It should accept syntactically valid programs even when they contain semantic errors. Those errors should be reported by later semantic passes.

Examples:

```nova
unknownName + 1;
```

This is syntactically valid and should parse as an expression statement. Name resolution should report the undefined name later.

```nova
1 = 2;
```

This is syntactically an assignment expression. Semantic l-value analysis should report that the left side is not assignable.

## Parser structure

The parser is split into focused grammar helpers.

Typical responsibilities are:

- top-level parser orchestration;
- declaration parsing;
- class parsing;
- expression parsing;
- shared parser support and cursor helpers;
- parser diagnostics and recovery.

This keeps individual grammar methods smaller and makes token consumption easier to reason about.

## Cursor contract

The parser cursor follows three core operations:

- `check(...)`: inspect without consuming;
- `match(...)`: consume only if the current token matches;
- `consume(...)`: require a token and report a parser diagnostic if it is missing.

A useful rule while editing parser code:

> Cursor movement should be obvious from the local method body.

Unexpected token advancement is one of the easiest ways to introduce parser bugs.

## Expression parsing

Expression parsing follows precedence levels.

The high-level shape is:

```text
assignment
  ↓
ternary
  ↓
logical or
  ↓
logical and
  ↓
equality
  ↓
comparison
  ↓
term
  ↓
factor
  ↓
unary
  ↓
call / member / array / postfix
  ↓
primary
```

This structure should be preserved unless there is a strong reason to replace the expression parser.

## Declarations

Declaration parsing recognizes source-level declarations such as:

- classes;
- constructors;
- methods;
- functions;
- variables;
- fields;
- parameters.

The parser no longer constructs lexical symbol scopes for these declarations. It preserves declaration names, types, parameters, bodies, and member lists in the AST; semantic declaration collection and semantic scopes decide visibility and validity later.

## Class parsing

Class parsing currently supports Nova class syntax that includes fields, methods, constructors, inheritance syntax, generic syntax, and nested class structure where supported by the AST.

Important boundary:

- the parser may recognize superclass syntax;
- semantic analysis should decide whether a superclass actually exists;
- the parser may recognize generic type syntax;
- a future type model should decide whether generic parameters are valid and visible.

The parser still has a temporary parse-session `TypeRegistry` for class and generic type recognition. That registry is not a semantic scope model and should be replaced by real type syntax and semantic type symbols in Phase 5.

## Error recovery

The parser should report errors and continue when possible.

Recovery is especially important at:

- top-level declaration boundaries;
- statement boundaries;
- block boundaries;
- class-body member boundaries.

A parser recovery change should usually include a regression test that proves valid code after the error is still parsed.

## What not to put in the parser

Avoid adding new parser checks for:

- undefined variables;
- undefined classes;
- duplicate declarations;
- declaration scope construction or symbol registration;
- invalid assignment targets;
- invalid return placement;
- invalid `break` or `continue` placement;
- type mismatches;
- overload resolution;
- access control.

Those belong in semantic analysis.

## Testing parser changes

Parser tests should usually assert one of these things:

- an AST shape;
- a diagnostic shape;
- successful recovery after invalid input;
- correct precedence and associativity;
- correct token consumption behavior.

When a parser bug is fixed, add the smallest source snippet that reproduces it.
