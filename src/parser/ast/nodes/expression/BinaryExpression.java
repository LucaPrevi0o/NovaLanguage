package src.parser.ast.nodes.expression;

import src.parser.ast.nodes.ExpressionNode;
import src.lexer.token.OperatorToken;

public class BinaryExpression extends ExpressionNode {
    
    private final ExpressionNode left;
    private final OperatorToken operator;
    private final ExpressionNode right;

    public BinaryExpression(int line, int column, ExpressionNode left, OperatorToken operator, ExpressionNode right) {

        super(line, column);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ExpressionNode getLeft() { return left; }
    public OperatorToken getOperator() { return operator; }
    public ExpressionNode getRight() { return right; }
}
