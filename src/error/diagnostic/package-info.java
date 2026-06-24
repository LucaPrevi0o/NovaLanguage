/**
 * Structured diagnostics for Nova compiler front-end errors and warnings.
 *
 * <p>This package contains the shared diagnostic model used across lexer, parser, and
 * semantic-analysis phases. Diagnostics preserve phase, severity, source position, message,
 * and optional expected/actual context so compiler errors can be tested and displayed
 * deterministically.</p>
 *
 * <p>Compiler layers should report through this structured model instead of relying on
 * global error state or legacy exception-only paths.</p>
 */
package error.diagnostic;
