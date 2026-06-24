package lexer.token.family;

import lexer.token.TokenClass;

/// Represents a non-primitive type, which is a user-defined class or interface type.
public record NonPrimitiveType(String typeName) implements TokenClass {

    @Override
    public String token() { return typeName; }
}
