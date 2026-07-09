/// # Access expression AST nodes
///
/// AST nodes for expression forms that access nested or indexed values.
///
/// This package represents syntax such as member access and array indexing. These nodes keep
/// the access chain visible to later compiler phases without deciding whether the access is
/// valid.
///
/// ## Responsibilities
///
/// - Represent member access syntax.
/// - Represent array indexing syntax.
/// - Preserve target expressions and index/member components.
/// - Provide semantic analysis with enough structure to validate access rules.
///
/// ## Semantic checks performed later
///
/// - Whether the target is an array, class instance, or another valid access target.
/// - Whether the selected field or method exists.
/// - Whether the access is allowed by visibility rules.
/// - Whether an indexed expression uses an integer-compatible index.
package parser.ast.nodes.expression.access;
