/// # Semantic type symbols
///
/// Resolved Nova type categories used by semantic analysis.
///
/// Parser AST nodes preserve source type spelling as `parser.ast.nodes.type.TypeSyntax`.
/// Semantic analysis resolves that syntax into symbols from this package.
///
/// ## Current symbol categories
///
/// - `ValueTypeSymbol` for Nova value/math types and built-in primitive-like types.
/// - `ClassTypeSymbol` for object/class declarations.
/// - `ArrayTypeSymbol` for arrays with resolved element types.
/// - `GenericParameterSymbol` for visible generic parameters.
/// - `UnknownTypeSymbol` for unresolved type names after diagnostics have been reported.
package semantic.type.symbol;
