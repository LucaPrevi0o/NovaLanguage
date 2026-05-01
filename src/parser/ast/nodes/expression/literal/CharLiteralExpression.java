package src.parser.ast.nodes.expression.literal;

import src.parser.ast.nodes.ExpressionNode;

/// Represents a character literal expression, such as 'a', '\n', or '\\'.
public class CharLiteralExpression extends ExpressionNode {

    private final char value;

    /// Constructs a new CharLiteralExpression with the specified line, column, and character value.
    /// @param line The line number where the character literal appears.
    /// @param column The column number where the character literal starts.
    /// @param value The character value of the literal.
    public CharLiteralExpression(int line, int column, char value) {

        super(line, column);
        this.value = value;
    }

    /// Returns the character value of this literal expression.
    /// @return The character value of this literal expression.
    public char getValue() { return value; }
}

