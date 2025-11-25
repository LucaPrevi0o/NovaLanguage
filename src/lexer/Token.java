package src.lexer;

import src.token.TokenFamily;

/**
 * Base class for all tokens produced by the lexer.
 * Contains common information like token type, line, and column.
 */
public class Token {

    private final TokenFamily type;
    private final int line;
    private final int column;

    public Token(TokenFamily type, int line, int column) {

        this.type = type;
        this.line = line;
        this.column = column;
    }

    public TokenFamily getType() { return type; }

    public int getLine() { return line; }

    public int getColumn() { return column; }

    @Override
    public String toString() {
        
        return String.format("%s {type=%s, line=%d, col=%d}",
            getClass().getSimpleName(), type, line, column);
    }
}
