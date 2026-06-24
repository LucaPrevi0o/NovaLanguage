/// # Semantic types
///
/// Semantic type symbols and type-resolution support.
///
/// This package contains the compiler's resolved representation of Nova types. It is the
/// destination for source-level type syntax after semantic name resolution.
///
/// ## Responsibilities
///
/// - Represent primitive type symbols.
/// - Represent class type symbols.
/// - Represent array type symbols.
/// - Represent generic-parameter type symbols.
/// - Represent unknown types so diagnostics can continue after failed resolution.
/// - Resolve parsed type syntax into semantic type symbols.
///
/// ## Why this package matters
///
/// The compiler is moving away from using lexer token classes as semantic type meaning. This
/// package is the foundation for a real Nova type model, including future support for class
/// types, mathematical/value-like Nova types, generics, overloads, and standard-library type
/// declarations.
///
/// ## Boundary
///
/// Parsed type syntax lives in `parser.ast.nodes.type`; resolved type meaning lives here.
package semantic.type;
