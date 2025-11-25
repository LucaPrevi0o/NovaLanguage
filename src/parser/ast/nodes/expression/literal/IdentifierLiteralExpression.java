package src.parser.ast.nodes.expression.literal;

import src.parser.ast.nodes.ExpressionNode;

public class IdentifierLiteralExpression extends ExpressionNode {
    
    String name;

    public IdentifierLiteralExpression(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    public String getName() { return name; }
}
