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
/// - Provide token-level adapters consumed by parser code.
/// - Keep token representation separate from resolved semantic symbols.
///
/// ## Current transition
///
/// Some classes in this package still act as temporary adapters between token-level type
/// information and later semantic type information. The long-term direction is to keep this
/// package lexical/syntactic and move resolved meaning into `semantic.type`.
package lexer.token;
