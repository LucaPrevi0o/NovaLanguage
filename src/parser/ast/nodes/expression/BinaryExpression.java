package parser.ast.nodes.expression;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import lexer.token.type.OperatorToken;

import java.util.List;

/// Represents a binary expression in the AST, consisting of a left expression, an operator, and a right expression.
public class BinaryExpression extends ExpressionNode implements Printable {
    
    private final ExpressionNode left;
    private final OperatorToken operator;
    private final ExpressionNode right;

    /// Constructs a new BinaryExpression with the specified line, column, left expression, operator, and right expression.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param left The left-hand side expression of the binary operation.
    /// @param operator The operator token representing the binary operation (e.g., +, -, *, /).
    /// @param right The right-hand side expression of the binary operation.
    public BinaryExpression(int line, int column, ExpressionNode left, OperatorToken operator, ExpressionNode right) {

        super(line, column);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    /// Returns the left-hand side expression of the binary operation.
    /// @return The left expression.
    public ExpressionNode getLeft() { return left; }

    /// Returns the operator token representing the binary operation.
    /// @return The operator token.
    public OperatorToken getOperator() { return operator; }

    /// Returns the right-hand side expression of the binary operation.
    /// @return The right expression.
    public ExpressionNode getRight() { return right; }

    @Override
    public String toPrintString() { return "BinaryExpression [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {
        return List.of(
            new PrintEntry.Info("Operator: " + operator.getType()),
            new PrintEntry.Child("Left", left),
            new PrintEntry.Child("Right", right)
        );
    }
}
