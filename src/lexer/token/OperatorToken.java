package src.lexer.token;

import src.lexer.Token;
import src.token.TokenFamily;
import src.token.family.Operator;

/**
 * Token representing an operator (+, -, *, /, =, ==, etc.).
 * The operator symbol is implicit from the TokenType.
 */
public class OperatorToken extends Token {

    public OperatorToken(TokenFamily type, int line, int column) {

        super(type, line, column);
        if (!(type instanceof Operator)) throw new IllegalArgumentException("TokenType " + type + " is not an operator");
    }
}
