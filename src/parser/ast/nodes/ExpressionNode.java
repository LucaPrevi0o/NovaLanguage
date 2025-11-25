package src.parser.ast.nodes;

import src.parser.ast.AstNode;

public abstract class ExpressionNode extends AstNode {
    
    public ExpressionNode(int line, int column) { super(line, column); }
}
