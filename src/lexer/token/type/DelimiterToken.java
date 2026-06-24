package lexer.token.type;

import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.Delimiter;
import lexer.token.family.Special;

/// Token representing a delimiter (parentheses, comma, colon...).
public class DelimiterToken extends Token {

    /// Constructs a new DelimiterToken with the specified delimiter, value, line, and column.
    /// @param delimiter The specific delimiter token type (must be a member of the {@link Delimiter} family).
    /// @param value The string value of the delimiter token (e.g., "(", ")", ",").
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public DelimiterToken(Delimiter delimiter, int line, int column) {
        super(delimiter, line, column);
    }

    /// Constructs a new DelimiterToken with the specified special token, value, line, and column.
    /// @param specialToken The specific special token type (must be a member of the {@link Special} family).
    /// @param value The string value of the special token (e.g., ":", ";").
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public DelimiterToken(Special specialToken, int line, int column) {
        super(specialToken, line, column);
    }
}
