package lexer.token.family;

import lexer.token.TokenClass;

/// Represents literals in the programming language, such as string literals, number literals, boolean literals, and identifier literals.
public abstract class Literal implements TokenClass {

    private final String value;

    /// Constructs a new Literal with the specified name or value.
    /// @param value The string representation of the literal, which can be a string value, a numeric value, a boolean
    /// value, or an identifier name, depending on the type of literal being represented.
    public Literal(String value) { this.value = value; }

    @Override
    public String token() { return value; }
}
