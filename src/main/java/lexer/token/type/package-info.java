/// # Concrete token types
///
/// Concrete token wrappers for specific Nova token categories.
///
/// This package turns broad token-family information into category-specific token objects that
/// can move through the parser and diagnostic pipeline with a common token contract.
///
/// ## Responsibilities
///
/// - Wrap family-specific token information.
/// - Preserve the original source lexeme and location.
/// - Provide concrete tokens for keywords, literals, operators, delimiters, and type-like syntax.
/// - Keep token representation lightweight and parser-friendly.
///
/// ## Design note
///
/// Concrete token classes should remain syntax-facing. They should not grow into semantic
/// declarations, resolved types, or backend concepts.
package lexer.token.type;
