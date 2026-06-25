package semantic.type.symbol;

import semantic.declaration.SemanticDeclaration;
import semantic.type.TypeKind;

/// Resolved class type symbol backed by a semantic class declaration.
/// @param name The class type name.
/// @param declaration The semantic declaration that defines the class.
public record ClassTypeSymbol(String name, SemanticDeclaration declaration) implements TypeSymbol {

    @Override
    public String getName() { return name; }

    @Override
    public TypeKind getKind() { return TypeKind.CLASS; }
}
