/**
 * AST nodes for parsed Nova type syntax.
 *
 * <p>This package represents type names and type shapes as they appear in source code before
 * semantic resolution. Parsed type syntax preserves named types, array type structure, and
 * generic-parameter syntax so later passes can resolve them into semantic type symbols.</p>
 *
 * <p>These nodes are a bridge away from treating lexer token classes as semantic types. They
 * should remain syntax-oriented and should not decide whether a referenced type exists.</p>
 */
package parser.ast.nodes.type;
