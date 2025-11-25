package src.parser.ast.nodes.expression;

import src.lexer.token.OperatorToken;
import src.parser.ast.nodes.ExpressionNode;

public class UnaryExpression extends ExpressionNode {
    
    private final OperatorToken operator;
    private final ExpressionNode operand;

    public UnaryExpression(int line, int column, OperatorToken operator, ExpressionNode operand) {
        
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }

    public OperatorToken getOperator() { return operator; }
    public ExpressionNode getOperand() { return operand; }
}
