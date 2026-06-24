/// # Parser support
///
/// Shared parser infrastructure used by Nova grammar parsers.
///
/// This package contains the state and helpers that make the grammar parsers predictable:
/// cursor movement, token inspection, required-token handling, parser diagnostics, and
/// temporary syntax adapters.
///
/// ## Cursor contract
///
/// - `check(...)` inspects the current token without consuming it.
/// - `match(...)` consumes only when the current token matches.
/// - `consume(...)` requires a token and reports a parser diagnostic if it is missing.
///
/// ## Design principle
///
/// Grammar code should be easy to audit locally. If a parser method moves the cursor, that
/// movement should be visible from the method body or from a well-known support helper.
///
/// ## Transition role
///
/// Some support classes still adapt parsed syntax to older parser-facing models. Those
/// adapters should shrink as semantic type symbols and source type syntax become the primary
/// representation.
package parser.support;
