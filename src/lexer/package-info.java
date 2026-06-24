/**
 * Lexical-analysis entry points for the Nova compiler front end.
 *
 * <p>This package contains the scanner components that transform raw Nova source text into
 * a stream of {@link lexer.Token} instances. The lexer is responsible for recognizing
 * language-level tokens, preserving source-position information, and reporting lexical
 * diagnostics for malformed input where recovery is possible.</p>
 *
 * <p>The lexer should remain independent from parser, AST, and semantic-analysis details.
 * Its output is consumed by the parser, but it should not decide whether names, types,
 * declarations, or expressions are semantically valid.</p>
 */
package lexer;
