package lexer;

import lexer.token.TokenClass;
import lexer.token.family.Literal;

/// Base class for all tokens produced by the lexer.
/// Each token has a type (from a specific TokenClass) and positional information (line and column).
public class Token {

    private final TokenClass type;
    private final int line;
    private final int column;
    private final String lexeme;

    /// Constructs a Token with the given type, line, and column.
    /// @param type The specific token type (must be a member of a TokenClass).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public Token(TokenClass type, int line, int column) { this(type, line, column, null); }

    /// Constructs a Token with an explicit source lexeme.
    /// @param type The specific token type (must be a member of a TokenClass).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    /// @param lexeme The raw source text for this token, when it should be preserved separately from the token class.
    public Token(TokenClass type, int line, int column, String lexeme) {

        this.type = type;
        this.line = line;
        this.column = column;
        this.lexeme = lexeme;
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

    /// Returns the raw source text for this token when explicitly preserved by the lexer.
    /// @return The raw token lexeme, or null if the token class fully describes the token text.
    public String getLexeme() { return lexeme; }

    @Override
    public String toString() {

        return this.getClass().getSimpleName() + "{line=" + line + ", col=" + column + "} = " +
        (type instanceof Literal ? "\"" + type.token() + "\"" : lexeme != null ? "\"" + lexeme + "\"" : type);
    }
}
