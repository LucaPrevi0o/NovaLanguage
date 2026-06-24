package parser.ast.nodes.expression.literal.number;

import lexer.token.family.PrimitiveType;
import lexer.token.type.TypeToken;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;

/// Represents a byte literal expression in the AST.
///
/// A byte literal expression represents a byte value in the source code, such as `0x1F` or `127`.
public final class ByteLiteralExpression extends NumberLiteralExpression {

    /// Constructs a new ByteLiteralExpression with the specified line, column, and byte value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param value The byte value of this literal expression.
    public ByteLiteralExpression(int line, int column, Byte value) { super(line, column, new TypeToken(PrimitiveType.BYTE, line, column), value); }

    @Override
    public Byte getValue() { return (Byte) value; }
}