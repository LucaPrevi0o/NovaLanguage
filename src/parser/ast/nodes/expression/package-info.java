/**
 * AST nodes for Nova expressions.
 *
 * <p>Expression nodes represent source-level computations such as assignments, binary and
 * unary operations, calls, object creation, ternary expressions, and postfix operations.
 * They preserve syntactic structure for later name resolution, type checking, l-value
 * validation, and lowering.</p>
 *
 * <p>These nodes should not decide whether an expression is semantically valid. For example,
 * an assignment expression can be represented here even when a later semantic pass rejects
 * its target as non-assignable.</p>
 */
package parser.ast.nodes.expression;
