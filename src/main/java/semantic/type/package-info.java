/// # Semantic types
///
/// Semantic type symbols and type-resolution support.
///
/// This package contains the compiler's resolved representation of Nova types. It is the
/// destination for source-level type syntax after semantic name resolution.
///
/// ## Responsibilities
///
/// - Represent Nova value/math type symbols, including built-in primitive-like values.
/// - Represent class type symbols.
/// - Represent array type symbols.
/// - Represent generic-parameter type symbols.
/// - Represent unknown types so diagnostics can continue after failed resolution.
/// - Classify resolved symbols by high-level `TypeKind`.
/// - Resolve parsed type syntax into semantic type symbols.
///
/// ## Why this package matters
///
/// The compiler uses semantic type symbols instead of lexer token classes for type meaning. This
/// package is the foundation for Nova's type model: class types model object identity,
/// value types model mathematical/value-like semantics, arrays preserve their element type, and
/// visible class generic parameter names stay explicit until later constraint and specialization
/// work can resolve generic instantiations.
///
/// ## Boundary
///
/// Parsed type syntax lives in `parser.ast.nodes.type`; resolved type meaning lives here.
/// Semantic passes should pass parsed `TypeSyntax` to the resolver.
/// The human-readable version of this boundary is documented in `docs/architecture.md`.
package semantic.type;
