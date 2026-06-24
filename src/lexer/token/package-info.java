/**
 * Token abstractions shared by the lexer and parser.
 *
 * <p>This package defines the common token-facing model used after lexical analysis. Tokens
 * preserve the token class, source lexeme, and source location needed by later compiler
 * stages and diagnostics.</p>
 *
 * <p>Some classes in this package still act as temporary adapters between token-level type
 * information and semantic type information. The long-term compiler roadmap moves type
 * meaning into {@code semantic.type} while keeping this package focused on lexical and
 * syntactic token representation.</p>
 */
package lexer.token;
