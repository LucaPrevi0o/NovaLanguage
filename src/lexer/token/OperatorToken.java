package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.Operator;

/**
 * Token representing an operator.
 */
public class OperatorToken extends Token {

    /// Constructs an OperatorToken with the given type, line, and column.
    /// @param type The specific operator type.
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public OperatorToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof Operator)) throw new IllegalArgumentException("TokenType " + type + " is not an operator");
    }
}
