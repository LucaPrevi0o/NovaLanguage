package parser.ast.nodes.expression.literal;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import lexer.token.type.TypeToken;

import java.util.List;

/// Represents a numeric literal expression in the AST, which can be an integer, long, float, double, or byte literal.
public abstract class NumberLiteralExpression extends ExpressionNode implements Printable {

    private final TypeToken typeToken;

    /// The numeric value of this literal expression, which can be of various numeric types (Integer, Long, Float, Double, Byte).
    protected final Number value;

    /// Constructs a new NumberLiteralExpression with the specified line, column, type token, and numeric value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param typeToken The type token representing the specific numeric type of this literal expression.
    /// @param value The numeric value of this literal expression.
    public NumberLiteralExpression(int line, int column, TypeToken typeToken, Number value) {

        super(line, column);
        this.typeToken = typeToken;
        this.value = value;
    }

    /// Returns the numeric value of this literal expression.
    /// @return The numeric value.
    public abstract Number getValue();

    /// Returns the type token representing the specific numeric type of this literal expression.
    /// @return The type token of this literal expression.
    public TypeToken getTypeToken() { return typeToken; }

    @Override
    public String toPrintString() { return "NumberLiteralExpression [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("Value: " + getValue())); }
}
