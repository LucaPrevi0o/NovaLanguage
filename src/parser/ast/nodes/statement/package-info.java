/// # Statement AST nodes
///
/// AST nodes for Nova statements.
///
/// Statements form the top-level and block-level structure consumed by semantic declaration
/// collection, scope construction, and validation passes.
///
/// ## Examples
///
/// - Blocks
/// - Class declarations
/// - Variable declarations
/// - Expression statements
/// - Return statements
/// - Conditional and loop statements
/// - Control-transfer statements such as `break` and `continue`
///
/// ## Compiler role
///
/// Statement nodes preserve program structure. Semantic passes decide whether declarations are
/// duplicated, returns are valid, control-transfer statements are in valid contexts, and
/// statement-level type rules are satisfied. Declaration-style statement nodes expose parsed
/// `TypeSyntax` where the source contains a declared type, but they do not resolve that type.
package parser.ast.nodes.statement;
