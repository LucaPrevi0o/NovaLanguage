package semantic.type.symbol;

import semantic.type.TypeKind;
import semantic.type.ValueTypeDomain;

/// Resolved Nova value type symbol, including built-in primitive-like and future user-defined math types.
/// @param name The value type name.
/// @param domain The value type domain.
public record ValueTypeSymbol(String name, ValueTypeDomain domain) implements TypeSymbol {

    /// Creates a value type symbol.
    public ValueTypeSymbol { domain = domain != null ? domain : ValueTypeDomain.CUSTOM; }

    /// Creates a value type symbol for a built-in Nova value type name.
    /// @param name The built-in value type name.
    /// @return The value type symbol.
    public static ValueTypeSymbol builtin(String name) {
        return new ValueTypeSymbol(name, ValueTypeDomain.fromName(name));
    }

    @Override
    public String getName() { return name; }

    @Override
    public TypeKind getKind() { return TypeKind.VALUE; }

    /// Returns whether this value type is numeric.
    /// @return `true` for integral and floating-point domains.
    public boolean isNumeric() { return domain.isNumeric(); }
}
