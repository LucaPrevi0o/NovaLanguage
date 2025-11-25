package src.parser.ast.nodes;

import src.parser.ast.AstNode;

public abstract class StatementNode extends AstNode {

    public StatementNode(int line, int column) { super(line, column); }
}
