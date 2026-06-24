/**
 * Parser orchestration for the Nova compiler front end.
 *
 * <p>This package exposes the parser entry point that coordinates grammar-specific parser
 * helpers, consumes the lexer token stream, and produces source-level AST nodes together
 * with structured parser diagnostics.</p>
 *
 * <p>The parser is intended to remain syntactic. It should recognize valid Nova source
 * shapes and recover from malformed syntax where possible, while leaving name resolution,
 * type checking, duplicate validation, and contextual checks to semantic-analysis passes.</p>
 */
package parser;
