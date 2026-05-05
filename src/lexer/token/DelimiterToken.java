package lexer.token;

import lexer.Token;
import token.TokenFamily;
import token.family.Delimiter;

/// Token representing a delimiter (parentheses, comma, colon...).
public class DelimiterToken extends Token {

    /// Constructs a DelimiterToken with the given type, line, and column.
    /// @param type The specific delimiter type (e.g., LEFT_PAREN, RIGHT_BRACE).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public DelimiterToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof Delimiter)) throw new IllegalArgumentException("TokenType " + type + " is not a delimiter");
    }
}
