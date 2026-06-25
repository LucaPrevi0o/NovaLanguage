package semantic.type.symbol;

import semantic.type.TypeKind;

/// Placeholder semantic type symbol used when type resolution fails.
/// @param name The unresolved type name.
public record UnknownTypeSymbol(String name) implements TypeSymbol {

    @Override
    public String getName() { return name; }

    @Override
    public TypeKind getKind() { return TypeKind.UNKNOWN; }
}
