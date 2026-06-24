/**
 * Semantic type symbols and type-resolution support.
 *
 * <p>This package contains the compiler's semantic representation of Nova types. It is the
 * destination for resolved type meaning after the parser has preserved source-level type
 * syntax.</p>
 *
 * <p>The long-term direction is for type checking to use these semantic type symbols instead
 * of comparing lexer token classes or parser adapters. This package is therefore central to
 * the transition from syntactic type names to a real Nova type model.</p>
 */
package semantic.type;
