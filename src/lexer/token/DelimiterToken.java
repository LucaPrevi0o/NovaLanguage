package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.Delimiter;

/**
 * Token representing a delimiter ((, ), {, }, ;, ,).
 * The delimiter symbol is implicit from the TokenType.
 */
public class DelimiterToken extends Token {

    public DelimiterToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof Delimiter)) throw new IllegalArgumentException("TokenType " + type + " is not a delimiter");
    }
}
