package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.*;

public class ExpressionStatement extends StatementNode {

    private final ExpressionNode expression;

    public ExpressionStatement(int line, int column, ExpressionNode expression) {
        
        super(line, column);
        this.expression = expression;
    }

    public ExpressionNode getExpression() { return expression; }
}
