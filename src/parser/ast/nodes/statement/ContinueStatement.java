package parser.ast.nodes.statement;

import parser.ast.nodes.StatementNode;
import parser.ast.visitor.NodeVisitor;

public class ContinueStatement extends StatementNode {

    public ContinueStatement(int line, int column) {
        super(line, column);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitContinue(this); }
}
