# Multi-File Project Pipeline

This document defines the Phase 6 contracts for moving Nova from a single-file
front end to a project-level compiler pipeline.

The goal is design clarity before implementation. The current compiler still
parses one token stream at a time, but the next implementation steps should fit
the contracts described here.

## Goals

- Lex and parse every source file before semantic analysis begins.
- Preserve the parser/semantic boundary: parsing remains file-local and syntax-only.
- Collect declarations across all parsed units before resolving names or types.
- Keep diagnostics tied to the source file that produced them.
- Leave package and import syntax as explicit placeholders until package semantics
  are designed.

## Non-Goals

- Do not implement full multi-file compilation in this design step.
- Do not introduce Orbit, Quark dependency resolution, or package manager behavior.
- Do not load the standard library as source yet.
- Do not introduce IR or backend behavior.

## Planned Flow

```text
SourceFile list
  -> lex every source file
  -> parse every token stream
  -> produce CompilationUnit values
  -> build a ProjectContext
  -> collect declarations across all units
  -> build project-level semantic scopes
  -> resolve names and types across the project
  -> run semantic validation passes
  -> return units, project context, and diagnostics
```

The first implementation should run semantic analysis only after all files have
finished lexical and parser work. If lexer or parser errors exist, the compiler
can return the collected units and diagnostics without running semantic passes.
A later recovery mode may choose to run semantic checks over recovered ASTs, but
that should be an explicit option rather than an accidental cascade.

## `SourceFile`

`SourceFile` is the immutable input identity and source text.

Implementation status: `compiler.SourceFile` and `compiler.SourceOrigin` are
available.

It should own:

- a stable file identity, such as a normalized path or display name;
- the complete source text;
- optional origin metadata, such as whether the file came from disk, memory, or
  a future standard-library source loader.

It should not own:

- tokens;
- AST nodes;
- semantic declarations;
- semantic scopes;
- diagnostics produced by a compiler phase.

Keeping `SourceFile` small makes it suitable for user files, standard-library
files, generated test snippets, and future package-managed files.

## `CompilationUnit`

`CompilationUnit` is the syntax result for one `SourceFile`.

Implementation status: `compiler.CompilationUnit` is available.

It should own:

- the `SourceFile`;
- the token stream produced for that file;
- lexer diagnostics for that file;
- parser diagnostics for that file;
- top-level AST statements, including successfully recovered statements when
  parser recovery produced a partial AST;
- future package/import placeholder data once the grammar recognizes it.

It should not own semantic meaning. A `CompilationUnit` may expose source-level
syntax facts, but it should not decide whether a referenced class exists, whether
an imported symbol is visible, or whether a declaration is duplicated.

## `ProjectContext`

`ProjectContext` is the project-level semantic workspace.

It should own:

- the ordered list of `CompilationUnit` values;
- the project diagnostic collection;
- the project declaration collection or declaration index;
- the root project/global semantic scope;
- future package/import placeholder state;
- future standard-library and native/builtin declaration inputs.

It should not perform lexing or parsing itself. The `Compiler` should build a
`ProjectContext` from completed units, then semantic passes should consume that
context.

The initial package model should be simple: all files belong to one default
project namespace unless a later package/import design says otherwise. Duplicate
top-level declarations from different files are therefore duplicates in that
shared namespace.

## `Compiler`

`Compiler` is the orchestrator. It should coordinate phases without becoming a
semantic pass or parser helper.

The first project-level entry point should accept a list of `SourceFile` values
and return a result that includes:

- the generated `CompilationUnit` values;
- the `ProjectContext`, when semantic setup was possible;
- all diagnostics, grouped or wrapped with file identity;
- a clear success/error status.

Recommended phase order:

1. Validate source-file identities, reporting duplicate input identities as
   project diagnostics.
2. Lex every source file.
3. Parse every token stream.
4. Stop before semantic analysis if any lexer/parser errors should block
   semantic checks in the first implementation.
5. Create the `ProjectContext`.
6. Collect declarations from all units.
7. Build project-level scopes.
8. Resolve names and type syntax across the whole project.
9. Run duplicate, type, l-value, return, and loop-control checks.

## Diagnostics

Current diagnostics carry phase, message, line, column, optional span, and token
context. Phase 6 needs file identity without forcing every compiler layer to know
about paths.

Implementation status: `compiler.SourceDiagnostic` wraps existing diagnostics
with optional source-file identity.

The project pipeline should therefore wrap diagnostics at the boundary:

```text
SourceFile + Diagnostic
```

Project-level diagnostics that are not tied to one file, such as duplicate input
identities, can use an empty source-file reference.

Do not encode file paths into diagnostic messages. Formatting belongs at the
reporting boundary, where file identity, line, and column can be combined for the
CLI or test assertions.

## Declaration Collection

Declaration collection should run after all files have parsed.

The project-level collector should see all units before name or type resolution
begins. This enables forward references and mutual references across files, such
as one class declaring a field whose type is declared in another file.

The public contract should stay file-aware even if an early implementation
internally reuses existing single-file collectors. Flattening all statements into
one list may be acceptable as an implementation bridge, but file identity must
remain available for diagnostics and future package/import behavior.

## Package And Import Placeholders

Phase 6 should reserve space for package/import behavior without implementing the
full semantics.

Initial rules:

- every file belongs to the default project namespace;
- imports do not affect name visibility yet;
- package/import syntax, once parsed, should be stored on `CompilationUnit`;
- `ProjectContext` should be the future owner of package/import resolution.

This keeps the multi-file pipeline compatible with later Orbit/Quark dependency
work without pulling package management into the compiler front end too early.

## First Acceptance Target

Issue #23 should use this design to define the first two-file acceptance test.

The minimal target should prove that declaration collection happens before
semantic body/type resolution across files. For example:

```nova
// a.nv
public class A {
  public B peer;
}
```

```nova
// b.nv
public class B {}
```

The test should compile both files through the project-level entry point and
should not rely on parsing one file before the other for semantic visibility.

## Follow-Up Implementation Issues

The design implies these implementation tasks:

- use the existing immutable `SourceFile` and `CompilationUnit` models as the
  input/output boundary for project orchestration;
- use `SourceDiagnostic` for file-aware diagnostic aggregation;
- add `ProjectContext` through issue #72;
- add a `Compiler` entry point through issue #72 that lexes and parses all files
  before semantic analysis;
- adapt declaration collection and semantic passes to consume project-level
  inputs;
- implement the two-file cross-reference acceptance test from issue #23.

Each implementation task should be tracked by an issue before code changes begin
if it is not already covered by an open issue.
