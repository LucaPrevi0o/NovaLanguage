package src.parser.ast.nodes.expression;

import src.parser.ast.nodes.ExpressionNode;

public class CallExpression extends ExpressionNode {
    
    private final ExpressionNode callee;
    private final ExpressionNode[] arguments;

    public CallExpression(int line, int column, ExpressionNode callee, ExpressionNode[] arguments) {

        super(line, column);
        this.callee = callee;
        this.arguments = arguments;
    }

    public ExpressionNode getCallee() { return callee; }
    public ExpressionNode[] getArguments() { return arguments; }
}
