/// # Conditional and control-flow statement AST nodes
///
/// AST nodes for Nova control-flow statements.
///
/// This package preserves the source-level shape of branching, looping, and switch-like
/// constructs for later semantic checks and eventual lowering.
///
/// ## Covered constructs
///
/// - `if` statements
/// - `while` loops
/// - `for` loops
/// - `for-each` loops
/// - `switch` statements
///
/// ## Semantic checks performed later
///
/// - Condition expression validity.
/// - Loop variable declaration and visibility.
/// - `break` and `continue` placement.
/// - Switch case validation.
/// - Control-flow-sensitive return behavior, once implemented.
///
/// `ForEachStatement` exposes parsed element `TypeSyntax` for semantic type resolution while
/// keeping its temporary `ReturnType` adapter for compatibility.
package parser.ast.nodes.statement.conditional;
