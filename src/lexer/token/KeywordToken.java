package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.AccessModifier;
import src.token.family.Keyword;

 /// Token representing a language keyword ({@code if}, {@code else}, {@code while}, etc.).
public class KeywordToken extends Token {

    /// Constructs a KeywordToken with the given type, line, and column.
    /// @param type The specific keyword type.
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public KeywordToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof Keyword || type instanceof AccessModifier)) throw new IllegalArgumentException("TokenType " + type + " is not a keyword");
    }
}
