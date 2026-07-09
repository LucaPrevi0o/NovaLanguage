package semantic.type;

/// High-level semantic category for resolved Nova types.
public enum TypeKind {

    /// A value or mathematical type, including built-in primitive-like types.
    VALUE,

    /// A class type backed by a Nova class declaration.
    CLASS,

    /// An array type with one or more dimensions.
    ARRAY,

    /// A generic type parameter visible from the current semantic scope.
    GENERIC_PARAMETER,

    /// A placeholder used after type resolution fails.
    UNKNOWN
}
