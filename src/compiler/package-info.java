/// # Compiler project pipeline
///
/// Project-level compiler-front-end models.
///
/// This package contains the Phase 6 foundation for compiling more than one Nova
/// source file at a time. These models sit above lexer/parser single-file APIs
/// and below future project-level semantic orchestration.
///
/// ## Current contents
///
/// - `SourceFile` stores immutable source identity, text, and origin metadata.
/// - `CompilationUnit` stores one file's tokens, AST, lexer diagnostics, parser
///   diagnostics, and package/import placeholders.
/// - `SourceDiagnostic` wraps an existing diagnostic with source-file identity.
/// - `SourceOrigin` classifies where source text entered the project pipeline.
///
/// ## Boundary
///
/// These models should not perform parsing, name resolution, type checking, or
/// backend work. They preserve data that later `Compiler` and `ProjectContext`
/// implementations can orchestrate across files.
package compiler;
