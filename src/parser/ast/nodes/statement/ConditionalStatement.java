package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.*;

public abstract class ConditionalStatement extends StatementNode {

    private final ExpressionNode condition;

    public ConditionalStatement(int line, int column, ExpressionNode condition) {

        super(line, column);
        this.condition = condition;
    }

    public ExpressionNode getCondition() { return condition; }
}
