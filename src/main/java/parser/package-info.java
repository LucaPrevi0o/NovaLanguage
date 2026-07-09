/// # Parser
///
/// Parser orchestration for the Nova compiler front end.
///
/// This package exposes the parser entry point that coordinates grammar-specific parser
/// helpers, consumes lexer tokens, and produces source-level AST nodes together with parser
/// diagnostics.
///
/// ## Pipeline role
///
/// ```text
/// token stream -> AST + parser diagnostics
/// ```
///
/// ## Responsibilities
///
/// - Drive the recursive-descent parsing process.
/// - Coordinate declaration, class, and expression grammar parsers.
/// - Preserve parser diagnostics emitted during recovery.
/// - Return a source-level AST suitable for semantic declaration collection.
///
/// ## Boundary
///
/// The parser answers: **is this valid Nova syntax?**
///
/// It should not reject code only because a referenced name is undefined, a declaration is
/// duplicated, or an assignment target is semantically invalid. Those checks belong to
/// semantic analysis.
package parser;
