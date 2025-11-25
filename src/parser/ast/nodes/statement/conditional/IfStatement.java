package src.parser.ast.nodes.statement.conditional;

import src.parser.ast.nodes.*;
import src.parser.ast.nodes.statement.ConditionalStatement;

public class IfStatement extends ConditionalStatement {

    private final StatementNode thenBlock;
    private final StatementNode elseBlock;

    public IfStatement(int line, int column, ExpressionNode condition, StatementNode thenBlock, StatementNode elseBlock) {

        super(line, column, condition);
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public StatementNode getThenBlock() { return thenBlock; }
    public StatementNode getElseBlock() { return elseBlock; }
}
