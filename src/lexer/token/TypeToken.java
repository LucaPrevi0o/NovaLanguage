package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.NonPrimitiveType;
import src.token.family.PrimitiveType;

/// Represents a token that corresponds to a type in the programming language, such as primitive types (int, long, byte,
/// float, double, char, bool, void) or non-primitive types (class names, interface names).
public class TypeToken extends Token {

    /// Constructs a TypeToken with the given type, line, and column.
    /// @param type The specific type.
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    /// @throws IllegalArgumentException if the provided type is not a valid type token.
    public TypeToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof PrimitiveType || type instanceof NonPrimitiveType)) throw new IllegalArgumentException("TokenType " + type + " is not a type");
    }
}
