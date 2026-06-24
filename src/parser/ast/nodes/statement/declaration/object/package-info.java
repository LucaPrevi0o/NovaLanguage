/// # Class-owned declaration AST nodes
///
/// AST nodes for declarations that belong to Nova classes.
///
/// This package represents class members as source syntax: fields, methods, constructors, and
/// the member information needed by later semantic passes.
///
/// ## Responsibilities
///
/// - Preserve member names and declared types.
/// - Preserve access modifiers required by the class grammar.
/// - Preserve method and constructor parameters.
/// - Preserve member bodies where applicable.
///
/// ## Semantic checks performed later
///
/// - Member duplication and overload compatibility.
/// - Constructor validity.
/// - Access-control rules.
/// - Inheritance and override rules.
/// - Field and method lookup from member-access expressions.
package parser.ast.nodes.statement.declaration.object;
