package parser.ast.nodes.expression.literal;

import parser.ast.nodes.ExpressionNode;

/// Represents a string literal expression in the AST.
public class StringLiteralExpression extends ExpressionNode {
    
    String value;

    /// Constructs a new StringLiteralExpression with the specified line, column, and string value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The string value of this literal expression.
    public StringLiteralExpression(int line, int column, String value) {

        super(line, column);
        this.value = value;
    }

    /// Returns the string value of this literal expression.
    /// @return The string value.
    public String getValue() { return value; }
}
