package parser.ast.nodes.expression.literal.number;

import lexer.token.family.PrimitiveType;
import lexer.token.type.TypeToken;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;

/// Represents a float literal expression in the AST.
///
/// A float literal expression represents a floating-point number in the source code, such as `3.14f` or `2.0f`.
public final class FloatLiteralExpression extends NumberLiteralExpression {

    /// Constructs a new FloatLiteralExpression with the specified line, column, and float value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The float value of this literal expression.
    public FloatLiteralExpression(int line, int column, Float value) { super(line, column, new TypeToken(PrimitiveType.FLOAT, line, column), value); }

    @Override
    public Float getValue() { return (Float) value; }
}