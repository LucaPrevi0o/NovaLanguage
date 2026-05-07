package parser.ast.nodes.expression.access;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.visitor.NodeVisitor;

import java.util.List;

/// Represents an array access expression.
///
/// Example: `myArray[5]`
public class ArrayAccessExpression extends ExpressionNode implements Printable {
    
    private final ExpressionNode array;       // The array being accessed
    private final ExpressionNode index;       // The index expression

    /// Constructs an ArrayAccessExpression with the given array and index expressions.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param array The expression representing the array being accessed.
    /// @param index The expression representing the index being accessed.
    public ArrayAccessExpression(int line, int column, ExpressionNode array, ExpressionNode index) {

        super(line, column);
        this.array = array;
        this.index = index;
    }

    /// Returns the expression representing the array being accessed.
    /// @return The array expression.
    public ExpressionNode getArray() { return array; }

    /// Returns the expression representing the index being accessed.
    /// @return The index expression.
    public ExpressionNode getIndex() { return index; }

    @Override
    public String toPrintString() { return "ArrayAccessExpression [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {
        return List.of(
            new PrintEntry.Child("Array", array),
            new PrintEntry.Child("Index", index)
        );
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitArrayAccess(this); }
}
