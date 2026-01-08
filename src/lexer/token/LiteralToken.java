package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;

/**
 * Token representing a literal value (number, identifier, string).
 * Stores the actual value as a string.
 */
public class LiteralToken extends Token {

    private final String value;

    public LiteralToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        this.value = type.get();
    }

    public String getValue() { return value; }
    
    @Override
    public String toString() { return "LiteralToken {value='" + value + "', line=" + getLine() + ", col=" + getColumn() + "}"; }
}
