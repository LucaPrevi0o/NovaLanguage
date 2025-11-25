package src.parser.ast.nodes.statement.conditional;

import src.parser.ast.nodes.*;
import src.parser.ast.nodes.statement.ConditionalStatement;

public class WhileStatement extends ConditionalStatement {

    private final StatementNode body;

    public WhileStatement(int line, int column, ExpressionNode condition, StatementNode body) {

        super(line, column, condition);
        this.body = body;
    }

    public StatementNode getBody() { return body; }
}