package semantic.type;

/// Resolved primitive type symbol, such as `int`, `bool`, or `void`.
/// @param name The primitive type name.
public record PrimitiveTypeSymbol(String name) implements TypeSymbol {

    @Override
    public String getName() { return name; }
}
