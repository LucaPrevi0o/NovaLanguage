package parser.ast.nodes.expression.literal;

import parser.ast.nodes.ExpressionNode;
import parser.ast.visitor.NodeVisitor;

/// Represents a null literal expression, used to represent the absence of a value.
public class NullLiteralExpression extends ExpressionNode {

    /// Constructs a new NullLiteralExpression with the specified line and column.
    /// @param line The line number where the null literal appears.
    /// @param column The column number where the null literal starts.
    public NullLiteralExpression(int line, int column) {

        super(line, column);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitNullLiteral(this); }
}
