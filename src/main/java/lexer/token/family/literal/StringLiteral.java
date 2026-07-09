package lexer.token.family.literal;

import lexer.token.family.Literal;

/// Represents a string literal in the programming language, which is a sequence of characters enclosed in double quotes (e.g., "Hello, World!").
public final class StringLiteral extends Literal {

    /// Constructs a new StringLiteral with the specified string value.
    /// @param string The string value of the literal (e.g., "Hello, World!").
    public StringLiteral(String string) { super(string); }

    /// Constructs a new StringLiteral with a null value, which can be used as a placeholder or default value.
    public StringLiteral() { super(null); }
}