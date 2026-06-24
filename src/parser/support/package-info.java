/**
 * Shared parser infrastructure used by Nova grammar parsers.
 *
 * <p>This package contains parser-state management, cursor helpers, structured parse-error
 * helpers, and adapter utilities used while translating syntax into AST nodes.</p>
 *
 * <p>The support layer should make grammar code predictable: {@code check} inspects,
 * {@code match} conditionally consumes, and {@code consume} requires a token or reports a
 * parser diagnostic. Keeping these contracts stable is essential for parser recovery.</p>
 */
package parser.support;
