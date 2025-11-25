package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.*;

public class BlockStatement extends StatementNode {
    
    private final StatementNode[] statements;

    public BlockStatement(int line, int column, StatementNode[] statements) {

        super(line, column);
        this.statements = statements;
    }

    public StatementNode[] getStatements() { return statements; }
}
