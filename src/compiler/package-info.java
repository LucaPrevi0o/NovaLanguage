/// # Compiler project pipeline
///
/// Project-level compiler-front-end models.
///
/// This package contains the Phase 6 foundation for compiling more than one Nova
/// source file at a time. These models and orchestrators sit above lexer/parser
/// single-file APIs and coordinate project-level semantic analysis.
///
/// ## Current contents
///
/// - `SourceFile` stores immutable source identity, text, and origin metadata.
/// - `CompilationUnit` stores one file's tokens, AST, lexer diagnostics, parser
///   diagnostics, and package/import placeholders.
/// - `ProjectContext` stores project units, aggregated diagnostics,
///   declarations, and root semantic scope data.
/// - `Compiler` coordinates lexing, parsing, declaration collection, scope
///   construction, and semantic checks across source files.
/// - `SourceDiagnostic` wraps an existing diagnostic with source-file identity.
/// - `SourceOrigin` classifies where source text entered the project pipeline.
///
/// ## Boundary
///
/// `Compiler` coordinates phases but should not implement grammar or semantic
/// rules directly. `SourceFile`, `CompilationUnit`, `ProjectContext`,
/// `SourceDiagnostic`, and `SourceOrigin` preserve project data without owning
/// backend or IR behavior.
package compiler;
