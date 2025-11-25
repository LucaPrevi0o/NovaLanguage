package src.parser.ast.nodes.expression.access;

import src.parser.ast.nodes.ExpressionNode;

/**
 * Represents array access expression: array[index]
 * Examples: arr[0], matrix[i][j]
 */
public class ArrayAccessExpression extends ExpressionNode {
    
    private final ExpressionNode array;       // The array being accessed
    private final ExpressionNode index;       // The index expression
    
    public ArrayAccessExpression(int line, int column, ExpressionNode array, ExpressionNode index) {

        super(line, column);
        this.array = array;
        this.index = index;
    }

    public ExpressionNode getArray() { return array; }

    public ExpressionNode getIndex() { return index; }
}
