/// # Parsed type syntax AST nodes
///
/// AST nodes for Nova type syntax as written in source code.
///
/// This package preserves type names and type shapes before semantic resolution. It is a key
/// part of the transition away from treating lexer token classes as resolved semantic types.
///
/// ## Responsibilities
///
/// - Represent named type syntax.
/// - Represent array type syntax.
/// - Represent generic-parameter type syntax where supported by the parser.
/// - Preserve source type structure for the semantic type resolver.
///
/// ## Important distinction
///
/// Type syntax is not the same thing as a resolved type. A source file may mention a type name
/// that parses correctly but fails semantic resolution later.
package parser.ast.nodes.type;
