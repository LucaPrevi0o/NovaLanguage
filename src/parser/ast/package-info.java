/// # Abstract Syntax Tree
///
/// Core AST contracts for Nova source representation.
///
/// The AST is the parser's source-level representation of a Nova program. It preserves the
/// structure of the code as written before semantic resolution, lowering, optimization, or
/// backend-specific transformation.
///
/// ## Responsibilities
///
/// - Provide common node contracts used by statements, expressions, and type syntax.
/// - Preserve enough source structure for diagnostics and semantic analysis.
/// - Keep syntax representation separate from semantic declarations and resolved types.
/// - Support debugging and inspection through printable tree structures.
///
/// ## Not a backend IR
///
/// AST nodes should remain close to the source language. A future lowered IR should be a
/// separate representation built only after syntax and semantic checks are complete.
package parser.ast;
