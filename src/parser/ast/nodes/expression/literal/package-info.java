/// # Literal expression AST nodes
///
/// AST nodes for literal and identifier expressions.
///
/// Literal expressions are the parser-level representation of values and names that appear
/// directly in source code.
///
/// ## Covered expression forms
///
/// - String literals
/// - Character literals
/// - Boolean literals
/// - Numeric literals
/// - Identifier expressions
///
/// ## Identifier note
///
/// Identifier expressions preserve source names. They do not know which declaration the name
/// refers to until semantic name resolution runs.
///
/// ## Type note
///
/// Literal nodes may strongly suggest a type, but final type compatibility still belongs to
/// semantic analysis and the semantic type model.
package parser.ast.nodes.expression.literal;
