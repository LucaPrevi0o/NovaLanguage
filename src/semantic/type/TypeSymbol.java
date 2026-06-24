package semantic.type;

/// A resolved semantic type used by semantic analysis after parsing type syntax.
public sealed interface TypeSymbol permits PrimitiveTypeSymbol, ClassTypeSymbol, GenericParameterSymbol, ArrayTypeSymbol, UnknownTypeSymbol {

    /// Returns the semantic type name.
    /// @return The type name.
    String getName();

    /// Returns whether this symbol represents a successfully resolved type.
    /// @return `true` for resolved types, `false` for unknown placeholders.
    default boolean isResolved() { return !(this instanceof UnknownTypeSymbol); }
}
