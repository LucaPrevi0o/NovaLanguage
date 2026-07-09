/// # Numeric literal expression AST nodes
///
/// AST nodes for Nova numeric literal expressions.
///
/// This package contains concrete expression nodes for numeric source literals. The parser
/// preserves the literal category so semantic analysis can infer or validate the expression's
/// type in context.
///
/// ## Typical literal categories
///
/// - Byte literals
/// - Integer literals
/// - Long literals
/// - Float literals
/// - Double literals
///
/// ## Design note
///
/// Numeric literal nodes should preserve syntax and parsed value information. Numeric
/// promotion, assignment compatibility, overload selection, and user-defined numeric behavior
/// belong to semantic analysis and later type-system work.
package parser.ast.nodes.expression.literal.number;
