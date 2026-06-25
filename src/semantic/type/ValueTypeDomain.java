package semantic.type;

/// Domain of a Nova value type.
public enum ValueTypeDomain {

    /// Integral numeric values, such as `byte`, `int`, and `long`.
    INTEGRAL,

    /// Floating-point numeric values, such as `float` and `double`.
    FLOATING_POINT,

    /// Boolean truth values.
    BOOLEAN,

    /// Single-character values.
    CHARACTER,

    /// Text values.
    TEXT,

    /// The absence of a value.
    VOID,

    /// A future user-defined value/math type.
    CUSTOM;

    /// Returns the value-type domain for a built-in Nova value type name.
    /// @param name The built-in type name.
    /// @return The matching value-type domain, or {@link #CUSTOM} for future user-defined value types.
    public static ValueTypeDomain fromName(String name) {

        return switch (name) {
            case "byte", "int", "long" -> INTEGRAL;
            case "float", "double" -> FLOATING_POINT;
            case "bool" -> BOOLEAN;
            case "char" -> CHARACTER;
            case "string" -> TEXT;
            case "void" -> VOID;
            default -> CUSTOM;
        };
    }

    /// Returns whether this domain represents numeric values.
    /// @return `true` for integral and floating-point domains.
    public boolean isNumeric() { return this == INTEGRAL || this == FLOATING_POINT; }
}
