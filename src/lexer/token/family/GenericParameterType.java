package lexer.token.family;

import lexer.token.TokenClass;

/// Represents a generic parameter type, which is a placeholder for a type that will be specified when the generic class or method is instantiated.
/// It is used in the context of generic programming to allow for type flexibility and reuse of code with different types.
/// @param typeName The name of the generic parameter type.
public record GenericParameterType(String typeName) implements TokenClass {

    @Override
    public String token() { return typeName; }
}
