package lexer.token.family;

import lexer.token.TokenClass;

/// Represents a non-primitive type, which is a user-defined class or interface type.
/// It is used in the context of programming languages to define complex data structures and behaviors.
/// @param typeName The name of the non-primitive type.
public record NonPrimitiveType(String typeName) implements TokenClass {

    @Override
    public String token() { return typeName; }
}
