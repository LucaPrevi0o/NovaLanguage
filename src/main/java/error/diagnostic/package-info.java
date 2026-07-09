/// # Diagnostics
///
/// Structured diagnostics for Nova compiler front-end errors and warnings.
///
/// This package defines the shared diagnostic model used by lexer, parser, and semantic
/// analysis phases. Diagnostics are designed to be deterministic, testable, and independent
/// from global mutable error state.
///
/// ## Responsibilities
///
/// - Represent diagnostic severity and compiler phase.
/// - Preserve source line, column, and optional span information.
/// - Store human-readable messages.
/// - Carry expected and actual token or type context where useful.
/// - Support aggregate diagnostic reporting after recovery.
///
/// ## Design principle
///
/// Compiler layers should report through this structured model instead of relying on legacy
/// exception-only paths or global error collectors.
package error.diagnostic;
