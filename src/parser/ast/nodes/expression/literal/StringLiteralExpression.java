package src.parser.ast.nodes.expression.literal;

import src.parser.ast.nodes.ExpressionNode;

public class StringLiteralExpression extends ExpressionNode {
    
    String value;

    public StringLiteralExpression(int line, int column, String value) {

        super(line, column);
        this.value = value;
    }

    public String getValue() { return value; }
}
