package parser.ast.nodes.expression.literal.number;

import lexer.token.family.PrimitiveType;
import lexer.token.type.TypeToken;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;

/// Represents an integer literal expression in the AST.
///
/// An integer literal expression represents a whole number in the source code, such as `42` or `-7`.
public final class IntegerLiteralExpression extends NumberLiteralExpression {

    /// Constructs a new IntegerLiteralExpression with the specified line, column, and integer value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The integer value of this literal expression.
    public IntegerLiteralExpression(int line, int column, Integer value) { super(line, column, new TypeToken(PrimitiveType.INT, line, column), value); }

    @Override
    public Integer getValue() { return (Integer) value; }
}