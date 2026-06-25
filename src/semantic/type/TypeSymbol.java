package semantic.type;

/// A resolved semantic type used by semantic analysis after parsing type syntax.
public sealed interface TypeSymbol permits ValueTypeSymbol, ClassTypeSymbol, GenericParameterSymbol, ArrayTypeSymbol, UnknownTypeSymbol {

    /// Returns the semantic type name.
    /// @return The type name.
    String getName();

    /// Returns the high-level semantic type category.
    /// @return The semantic type kind.
    TypeKind getKind();

    /// Returns whether this symbol represents a successfully resolved type.
    /// @return `true` for resolved types, `false` for unknown placeholders.
    default boolean isResolved() { return getKind() != TypeKind.UNKNOWN; }

    /// Returns whether this symbol represents a Nova value/math type.
    /// @return `true` for value types.
    default boolean isValueType() { return getKind() == TypeKind.VALUE; }

    /// Returns whether this symbol represents a Nova class type.
    /// @return `true` for class types.
    default boolean isClassType() { return getKind() == TypeKind.CLASS; }
}
