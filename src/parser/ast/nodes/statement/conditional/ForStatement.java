package src.parser.ast.nodes.statement.conditional;

import src.parser.ast.nodes.*;
import src.parser.ast.nodes.statement.ConditionalStatement;

public class ForStatement extends ConditionalStatement {
    
    private final StatementNode initialization;
    private final StatementNode increment;
    private final StatementNode body;

    public ForStatement(int line, int column, StatementNode initialization, ExpressionNode condition, StatementNode increment, StatementNode body) {

        super(line, column, condition);
        this.initialization = initialization;
        this.increment = increment;
        this.body = body;
    }

    public StatementNode getInitialization() { return initialization; }
    public StatementNode getIncrement() { return increment; }
    public StatementNode getBody() { return body; }
}
