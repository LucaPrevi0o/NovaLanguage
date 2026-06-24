/**
 * Semantic-analysis passes for syntactically valid Nova ASTs.
 *
 * <p>This package contains validation passes that answer whether parsed Nova programs make
 * sense according to the language rules. Responsibilities include name resolution, type
 * checking, duplicate validation, l-value validation, return checking, and loop-control
 * context checking.</p>
 *
 * <p>Semantic analysis should consume AST nodes and semantic scopes rather than pushing
 * meaning back into parser code. When a construct is syntactically valid but meaningless,
 * this package should report the diagnostic.</p>
 */
package semantic.analysis;
