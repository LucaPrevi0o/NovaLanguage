package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.NonPrimitiveType;
import src.token.family.PrimitiveType;

/**
 * Token representing a data type (int, float, double, char, void, etc.).
 * The type is implicit from the TokenType.
 */
public class TypeToken extends Token {

    public TypeToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof PrimitiveType || type instanceof NonPrimitiveType)) throw new IllegalArgumentException("TokenType " + type + " is not a type");
    }
}
