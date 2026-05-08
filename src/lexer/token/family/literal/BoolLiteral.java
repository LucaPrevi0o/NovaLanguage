package lexer.token.family.literal;

import lexer.token.family.Literal;

/// Represents a boolean literal in the programming language, which can be either true or false.
public final class BoolLiteral extends Literal {

    /// Constructs a new BooleanLiteral with the specified boolean value.
    /// @param bool The string representation of the boolean value (e.g., "true", "false").
    private BoolLiteral(String bool) { super(bool); }

    /// The boolean literal representing the true value in the programming language.
    public static final BoolLiteral TRUE = new BoolLiteral("true");

    /// The boolean literal representing the false value in the programming language.
    public static final BoolLiteral FALSE = new BoolLiteral("false");
}