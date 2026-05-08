package lexer.token.family;

import lexer.token.TokenFamily;

/// Represents delimiters in the programming language, such as parentheses, braces, and semicolons.
public enum Delimiter implements TokenFamily {

    /// The left parenthesis delimiter, used for grouping expressions and function calls.
    LPAREN("("),

    /// The right parenthesis delimiter, used for grouping expressions and function calls.
    RPAREN(")"),

    /// The left brace delimiter, used for defining blocks of code (e.g., function bodies, loops).
    LBRACE("{"),

    /// The right brace delimiter, used for defining blocks of code (e.g., function bodies, loops).
    RBRACE("}"),

    /// The left square bracket delimiter, used for array indexing and list literals.
    LSQUARE("["),

    /// The right square bracket delimiter, used for array indexing and list literals.
    RSQUARE("]"),

    /// The semicolon delimiter, used to terminate statements.
    SEMICOLON(";"),

    /// The comma delimiter, used to separate items in lists and function arguments.
    COMMA(","),

    /// The colon delimiter, used in various contexts such as type annotations and case statements.
    COLON(":"),

    /// The double colon delimiter, used for namespace resolution and method references.
    DOUBLE_COLON("::"),

    /// The dot delimiter, used for member access and method calls.
    DOT(".");

    private final String symbol;

    /// Constructs a new Delimiter with the specified symbol.
    /// @param symbol The string representation of the delimiter (e.g., "(", ")", "{", "}").
    Delimiter(String symbol) { this.symbol = symbol; }

    @Override
    public String get() { return symbol; }
}
