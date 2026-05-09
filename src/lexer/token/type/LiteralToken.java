package lexer.token.type;

import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.Literal;

/// Token representing a literal value (number, identifier, string).
 /// Stores the actual value as a string.
public class LiteralToken extends Token {

    /// Constructs a new LiteralToken with the specified type, value, line, and column.
    /// @param type The specific literal token type (must be a member of a TokenClass representing literals).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public LiteralToken(Literal type, int line, int column) {
        super(type, line, column);
    }
}
