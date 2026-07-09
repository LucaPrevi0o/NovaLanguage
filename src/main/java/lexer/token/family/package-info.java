/// # Token families
///
/// Token-family definitions for Nova lexical categories.
///
/// Token families describe the broad categories recognized by the lexer before concrete
/// token wrappers are consumed by the parser.
///
/// ## Examples
///
/// - Keywords
/// - Operators
/// - Delimiters
/// - Access modifiers
/// - Primitive type tokens
/// - Special tokens such as end-of-file or unknown input
///
/// ## Layer boundary
///
/// These classes describe lexical categories. They are not the final representation of Nova
/// semantic types. Resolved type meaning belongs in the semantic type model.
package lexer.token.family;
