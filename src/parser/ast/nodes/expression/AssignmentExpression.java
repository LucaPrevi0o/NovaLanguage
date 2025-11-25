package src.parser.ast.nodes.expression;

import src.parser.ast.nodes.*;

public class AssignmentExpression extends ExpressionNode {

    private final ExpressionNode target;  // Can be IdentifierLiteralExpression, ArrayAccessExpression, or MemberAccessExpression
    private final ExpressionNode value;

    public AssignmentExpression(int line, int column, ExpressionNode target, ExpressionNode value) {

        super(line, column);
        this.target = target;
        this.value = value;
    }

    public ExpressionNode getTarget() { return target; }
    public ExpressionNode getValue() { return value; }
}