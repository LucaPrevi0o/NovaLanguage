package parser.ast.nodes.expression.literal;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;

import java.util.List;

/// Represents a null literal expression, used to represent the absence of a value.
public class NullLiteralExpression extends ExpressionNode implements Printable {

    /// Constructs a new NullLiteralExpression with the specified line and column.
    /// @param line The line number where the null literal appears.
    /// @param column The column number where the null literal starts.
    public NullLiteralExpression(int line, int column) {

        super(line, column);
    }

    @Override
    public String toPrintString() { return "NullLiteralExpression [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("Value: null")); }
}
