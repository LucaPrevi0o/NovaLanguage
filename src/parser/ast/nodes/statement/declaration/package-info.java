/// # Declaration statement AST nodes
///
/// AST nodes for source-level Nova declarations.
///
/// Declaration statements introduce named program entities into the source tree. The parser
/// records the declaration syntax here; semantic declaration collection later turns these AST
/// nodes into semantic declarations owned by semantic scopes.
///
/// ## Examples
///
/// - Variable declarations
/// - Function declarations
/// - Parameter-bearing declaration forms
/// - Shared declaration abstractions used by class-owned declarations
///
/// ## Layer boundary
///
/// Declaration AST nodes may carry names and declared type syntax, but they do not determine
/// duplicate validity, type existence, visibility, or overload compatibility.
package parser.ast.nodes.statement.declaration;
