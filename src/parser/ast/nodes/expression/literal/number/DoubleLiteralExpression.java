package parser.ast.nodes.expression.literal.number;

import lexer.token.family.PrimitiveType;
import lexer.token.type.TypeToken;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;

/// Represents a double literal expression in the AST.
///
/// A double literal expression represents a double-precision floating-point number in the source code, such as `3.14` or `2.0`.
/// It is used to represent floating-point values that require more precision than the `float` type can provide.
public final class DoubleLiteralExpression extends NumberLiteralExpression {

    /// Constructs a new DoubleLiteralExpression with the specified line, column, and double value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The double value of this literal expression.
    public DoubleLiteralExpression(int line, int column, Double value) { super(line, column, new TypeToken(PrimitiveType.DOUBLE, line, column), value); }

    @Override
    public Double getValue() { return (Double) value; }
}