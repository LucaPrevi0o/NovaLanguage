package lexer.token.type;

import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.AccessModifier;
import lexer.token.family.Keyword;

 /// Token representing a language keyword ({@code if}, {@code else}, {@code while}, etc.).
public class KeywordToken extends Token {

    /// Constructs a new KeywordToken with the specified keyword, value, line, and column.
    /// @param keyword The specific keyword token type (must be a member of the {@link Keyword} family).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public KeywordToken(Keyword keyword, int line, int column) {
        super(keyword, line, column);
    }

    /// Constructs a new KeywordToken with the specified access modifier, value, line, and column.
    /// @param accessModifier The specific access modifier token type (must be a member of the {@link AccessModifier} family).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public KeywordToken(AccessModifier accessModifier, int line, int column) {
        super(accessModifier, line, column);
    }
}
