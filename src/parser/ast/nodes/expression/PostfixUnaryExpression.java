package parser.ast.nodes.expression;


/// Represents a postfix unary expression (e.g. {@code x++}, {@code x--}).
/// Distinct from {@link UnaryExpression} which handles prefix operators.
public class PostfixUnaryExpression extends ExpressionNode {

    private final ExpressionNode operand;
    private final OperatorToken operator;

    /// Constructs a new PostfixUnaryExpression.
    /// @param line     The line number in the source code where this expression occurs.
    /// @param column   The column number in the source code where this expression starts.
    /// @param operand  The expression on which the postfix operator is applied.
    /// @param operator The postfix operator token (e.g., {@code ++} or {@code --}).
    public PostfixUnaryExpression(int line, int column, ExpressionNode operand, OperatorToken operator) {

        super(line, column);
        this.operand = operand;
        this.operator = operator;
    }

    /// Returns the expression that is the operand of the postfix operation.
    /// @return The operand expression.
    public ExpressionNode getOperand() { return operand; }

    /// Returns the postfix operator token.
    /// @return The operator token.
    public OperatorToken getOperator() { return operator; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitPostfixUnary(this); }
}
