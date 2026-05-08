package lexer.token.family;

import lexer.token.TokenFamily;

/// Represents literals in the programming language, such as string literals, number literals, boolean literals, and identifier literals.
public abstract class Literal implements TokenFamily {

    private final String name;

    /// Constructs a new Literal with the specified name or value.
    /// @param name The string representation of the literal, which can be a string value, a numeric value, a boolean
    /// value, or an identifier name, depending on the type of literal being represented.
    public Literal(String name) { this.name = name; }

    @Override
    public String get() { return name; }
}
