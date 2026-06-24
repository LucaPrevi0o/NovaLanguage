/**
 * AST nodes for Nova control-flow statements.
 *
 * <p>This package contains statement nodes for conditional and looping constructs such as
 * {@code if}, {@code while}, {@code for}, {@code for-each}, and {@code switch}. These nodes
 * preserve source-level control-flow shape for semantic checks and future lowering.</p>
 *
 * <p>Contextual rules, such as whether {@code break} or {@code continue} appears in a valid
 * location, are handled by semantic analysis rather than by these AST classes.</p>
 */
package parser.ast.nodes.statement.conditional;
