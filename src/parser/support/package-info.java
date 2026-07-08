/// # Parser support
///
/// Shared parser infrastructure used by Nova grammar parsers.
///
/// This package contains the state and helpers that make the grammar parsers predictable:
/// cursor movement, token inspection, required-token handling, and parser diagnostics.
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
/// ## Boundary
///
/// Support classes should help grammar code consume tokens predictably. They should not own
/// declaration meaning, semantic type resolution, or project-level orchestration.
package parser.support;
