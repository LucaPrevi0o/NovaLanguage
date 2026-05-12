package lexer.token.type;

import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.Operator;

/**
 * Token representing an operator.
 */
public class OperatorToken extends Token {

    /// Constructs a new OperatorToken with the specified operator, line, and column.
    /// @param operator The specific operator token type (must be a member of the {@link Operator} family).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public OperatorToken(Operator operator, int line, int column) {
        super(operator, line, column);
    }
}
