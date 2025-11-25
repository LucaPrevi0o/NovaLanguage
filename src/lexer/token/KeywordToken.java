package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.AccessModifier;
import src.token.family.Keyword;

/**
 * Token representing a language keyword (if, else, while, etc.).
 * The value is implicit from the TokenType.
 */
public class KeywordToken extends Token {

    public KeywordToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof Keyword || type instanceof AccessModifier)) throw new IllegalArgumentException("TokenType " + type + " is not a keyword");
    }
}
