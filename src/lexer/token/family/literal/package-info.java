/// # Literal token families
///
/// Token-family definitions for values written directly in Nova source code.
///
/// This package describes literal categories before they are transformed into AST literal
/// expression nodes.
///
/// ## Covered source forms
///
/// - Identifier-like lexemes
/// - String literals
/// - Character literals
/// - Boolean literals
/// - Numeric literals
///
/// ## Pipeline role
///
/// Literal token-family classes remain close to source text. The parser later turns them into
/// expression nodes, and semantic analysis decides their resolved type and contextual validity.
package lexer.token.family.literal;
