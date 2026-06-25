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
/// - Accept `ReturnType` adapters only as compatibility fallbacks when parsed syntax is absent.
///
/// ## Why this package matters
///
/// The compiler is moving away from using lexer token classes as semantic type meaning. This
/// package is the foundation for a real Nova type model: class types model object identity,
/// value types model mathematical/value-like semantics, arrays preserve their element type, and
/// generic parameters stay explicit until later specialization work can resolve them.
///
/// ## Boundary
///
/// Parsed type syntax lives in `parser.ast.nodes.type`; resolved type meaning lives here.
/// Semantic passes should pass parsed `TypeSyntax` to the resolver first and use legacy
/// `ReturnType` metadata only for compatibility-only AST nodes.
package semantic.type;
