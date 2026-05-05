package parser.ast.nodes.expression;

import parser.ast.nodes.ExpressionNode;
import parser.ast.visitor.NodeVisitor;

/// Represents a ternary conditional expression: {@code condition ? thenExpr : elseExpr}.
public class TernaryExpression extends ExpressionNode {

    private final ExpressionNode condition;
    private final ExpressionNode thenExpr;
    private final ExpressionNode elseExpr;

    /// Constructs a new TernaryExpression.
    /// @param line      The line number where this expression occurs.
    /// @param column    The column number where this expression starts.
    /// @param condition The condition expression.
    /// @param thenExpr  The expression evaluated when the condition is truthy.
    /// @param elseExpr  The expression evaluated when the condition is falsy.
    public TernaryExpression(int line, int column, ExpressionNode condition, ExpressionNode thenExpr, ExpressionNode elseExpr) {

        super(line, column);
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    /// Returns the condition expression.
    /// @return The condition.
    public ExpressionNode getCondition() { return condition; }

    /// Returns the expression evaluated when the condition is truthy.
    /// @return The then-expression.
    public ExpressionNode getThenExpr() { return thenExpr; }

    /// Returns the expression evaluated when the condition is falsy.
    /// @return The else-expression.
    public ExpressionNode getElseExpr() { return elseExpr; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitTernary(this); }
}
