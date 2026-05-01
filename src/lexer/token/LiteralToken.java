package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;

 /// Token representing a literal value (number, identifier, string).
 /// Stores the actual value as a string.
public class LiteralToken extends Token {

    private final String value;

    /// Constructs a LiteralToken with the given type, line, and column.
    /// The value is derived from the TokenFamily's {@code get()} method.
    /// @param type The specific literal type.
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public LiteralToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        this.value = type.get();
    }

    /// Returns the literal value as a string.
    /// @return The literal value.
    public String getValue() { return value; }
    
    @Override
    public String toString() { return "LiteralToken {value='" + value + "', line=" + getLine() + ", col=" + getColumn() + "}"; }
}
