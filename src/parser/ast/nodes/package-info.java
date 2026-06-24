/// # Shared AST nodes
///
/// Shared AST node types used by Nova statements, expressions, symbols, and type syntax.
///
/// This package contains common node-level abstractions reused by more specific AST packages.
/// These classes form the bridge between parser output and later semantic analysis.
///
/// ## Typical contents
///
/// - Base node helpers
/// - Symbol-like AST structures
/// - Shared statement and expression abstractions
/// - Common printable or inspectable node contracts
///
/// ## Design note
///
/// Shared AST nodes should avoid owning semantic meaning. They may carry names, tokens, and
/// source syntax, but resolved declarations and resolved types belong in the semantic layer.
package parser.ast.nodes;
