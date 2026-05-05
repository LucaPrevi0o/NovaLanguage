package parser.ast.nodes.expression.literal;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.visitor.NodeVisitor;

import java.util.List;

/// Represents a boolean literal expression in the AST.
public class BoolLiteralExpression extends ExpressionNode implements Printable {
    
    boolean value;

    /// Constructs a new BoolLiteralExpression with the specified line, column, and boolean value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The boolean value of this literal expression.
    public BoolLiteralExpression(int line, int column, boolean value) {

        super(line, column);
        this.value = value;
    }

    /// Returns the boolean value of this literal expression.
    /// @return The boolean value.
    public boolean getValue() { return value; }

    @Override
    public String toPrintString() { return "BoolLiteralExpression [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("Value: " + value)); }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitBoolLiteral(this); }
}
