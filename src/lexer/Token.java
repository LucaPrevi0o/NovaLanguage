package lexer;

import lexer.token.TokenClass;
import lexer.token.family.Literal;

//// Base class for all tokens produced by the lexer.
/// Each token has a type (from a specific TokenClass) and positional information (line and column).
public class Token {

    private final TokenClass type;
    private final int line;
    private final int column;

    /// Constructs a Token with the given type, line, and column.
    /// @param type The specific token type (must be a member of a TokenClass).
    /// @param value The string value of the token as it appears in the source code.
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public Token(TokenClass type, int line, int column) {

        this.type = type;
        this.line = line;
        this.column = column;
    }

    /// Returns the token's type.
    /// @return The token type.
    public TokenClass getType() { return type; }

    /// Returns the line number where the token appears.
    /// @return The line number.
    public int getLine() { return line; }

    /// Returns the column number where the token starts.
    /// @return The column number.
    public int getColumn() { return column; }

    @Override
    public String toString() {

        return this.getClass().getSimpleName() + "{line=" + line + ", col=" + column + "} = " +
        (type instanceof Literal ? "\"" + type.token() + "\"" : type);
    }
}
