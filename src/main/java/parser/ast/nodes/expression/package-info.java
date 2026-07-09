/// # Expression AST nodes
///
/// AST nodes for Nova expressions.
///
/// Expression nodes represent source-level computations and values. They preserve operator
/// structure, operands, calls, object creation, and other expression forms so semantic passes
/// can later resolve names and types.
///
/// ## Examples
///
/// - Assignment expressions
/// - Unary, binary, postfix, and ternary expressions
/// - Function and method calls
/// - Object creation expressions
/// - Literal and identifier expressions
/// - Member and array access expressions
///
/// ## Parser/semantic boundary
///
/// These nodes describe expression syntax. They should not decide whether an expression is
/// meaningful. For example, an assignment node may exist even when semantic l-value checking
/// later rejects the assignment target.
package parser.ast.nodes.expression;
