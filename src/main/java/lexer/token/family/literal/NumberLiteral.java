package lexer.token.family.literal;

import lexer.token.family.Literal;

/// Represents a number literal in the programming language, which can be an integer or a floating-point number (e.g., 42, 3.14).
public final class NumberLiteral extends Literal {

    /// Constructs a new NumberLiteral with the specified numeric value.
    /// @param number The string representation of the numeric value (e.g., "42",
    public NumberLiteral(String number) { super(number); }

    /// Constructs a new NumberLiteral with a null value, which can be used as a placeholder or default value.
    public NumberLiteral() { super(null); }
}