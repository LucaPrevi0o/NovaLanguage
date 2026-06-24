/// # Lexer
///
/// Lexical-analysis entry points for the Nova compiler front end.
///
/// This package owns the first transformation in the compiler pipeline:
///
/// ```text
/// source text -> tokens + lexer diagnostics
/// ```
///
/// ## Responsibilities
///
/// - Scan Nova source text character by character.
/// - Recognize keywords, identifiers, literals, operators, delimiters, and special tokens.
/// - Preserve source-position information for later diagnostics.
/// - Recover from malformed input when a useful token stream can still be produced.
/// - Report lexical diagnostics without depending on parser or semantic state.
///
/// ## Layer boundary
///
/// The lexer should not decide whether a name exists, whether a type is valid, or whether an
/// expression makes sense. Those are parser and semantic-analysis responsibilities.
package lexer;
