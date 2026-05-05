package lexer;

import token.TokenFamily;

//// Base class for all tokens produced by the lexer.
/// Each token has a type (from a specific TokenFamily) and positional information (line and column).
public class Token {

    private final TokenFamily type;
    private final int line;
    private final int column;

    /// Constructs a Token with the given type, line, and column.
    /// @param type The specific token type (must be a member of a TokenFamily).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public Token(TokenFamily type, int line, int column) {

        this.type = type;
        this.line = line;
        this.column = column;
    }

    /// Returns the token's type.
    /// @return The token type.
    public TokenFamily getType() { return type; }

    /// Returns the line number where the token appears.
    /// @return The line number.
    public int getLine() { return line; }

    /// Returns the column number where the token starts.
    /// @return The column number.
    public int getColumn() { return column; }

    @Override
    public String toString() { return this.getClass().getSimpleName() + "{type=" + type + ", line=" + line + ", col=" + column + "}"; }
}
