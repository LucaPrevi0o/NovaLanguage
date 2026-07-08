/// # Lexer tokens
///
/// Token abstractions shared by the lexer, parser, and diagnostics.
///
/// A token is the compiler front end's compact representation of a source fragment after
/// lexical analysis. Token objects preserve enough information for syntax parsing and error
/// reporting while avoiding semantic decisions.
///
/// ## Responsibilities
///
/// - Represent token classes and lexemes emitted by the lexer.
/// - Carry source-location information used by diagnostics.
/// - Keep token representation separate from resolved semantic symbols.
///
/// ## Boundary
///
/// This package is lexical and syntactic. Resolved type meaning belongs in `semantic.type`,
/// and source-level type spelling belongs in `parser.ast.nodes.type`.
package lexer.token;
