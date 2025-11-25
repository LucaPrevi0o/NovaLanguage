package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.*;

public class ReturnStatement extends StatementNode {
    
    private final ExpressionNode returnValue;

    public ReturnStatement(int line, int column, ExpressionNode returnValue) {

        super(line, column);
        this.returnValue = returnValue;
    }

    public ExpressionNode getReturnValue() { return returnValue; }
}
