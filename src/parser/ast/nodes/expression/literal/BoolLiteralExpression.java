package src.parser.ast.nodes.expression.literal;

import src.parser.ast.nodes.ExpressionNode;

public class BoolLiteralExpression extends ExpressionNode {
    
    boolean value;

    public BoolLiteralExpression(int line, int column, boolean value) {

        super(line, column);
        this.value = value;
    }

    public boolean getValue() { return value; }
}
