package parser.ast.nodes.expression.literal.number;

import lexer.token.family.PrimitiveType;
import lexer.token.type.TypeToken;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;

/// Represents a long literal expression in the AST.
///
/// A long literal expression represents a long integer in the source code, such as `123L` or `456l`.
/// It is used to represent integer values that exceed the range of the `int` type.
public final class LongLiteralExpression extends NumberLiteralExpression {

    /// Constructs a new LongLiteralExpression with the specified line, column, and long value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The long value of this literal expression.
    public LongLiteralExpression(int line, int column, Long value) { super(line, column, new TypeToken(PrimitiveType.LONG, line, column), value); }

    @Override
    public Long getValue() { return (Long) value; }
}