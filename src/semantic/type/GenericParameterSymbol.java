package semantic.type;

/// Resolved generic parameter type symbol.
/// @param name The generic parameter name.
public record GenericParameterSymbol(String name) implements TypeSymbol {

    @Override
    public String getName() { return name; }

    @Override
    public TypeKind getKind() { return TypeKind.GENERIC_PARAMETER; }
}
