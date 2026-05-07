package parser.ast.nodes.expression.literal;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import token.family.PrimitiveType;
import lexer.token.TypeToken;
import parser.ast.visitor.NodeVisitor;

import java.util.List;

/// Represents a numeric literal expression in the AST, which can be an integer, long, float, double, or byte literal.
public abstract class NumberLiteralExpression extends ExpressionNode implements Printable {

    /// Represents an integer literal expression in the AST.
    ///
    /// An integer literal expression represents a whole number in the source code, such as `42` or `-7`.
    public final static class IntegerLiteralExpression extends NumberLiteralExpression {

        /// Constructs a new IntegerLiteralExpression with the specified line, column, and integer value.
        /// @param line The line number in the source code where this expression occurs.
        /// @param column The column number in the source code where this expression starts.
        /// @param value The integer value of this literal expression.
        public IntegerLiteralExpression(int line, int column, Integer value) { super(line, column, new TypeToken(PrimitiveType.INT, line, column), value); }

        @Override
        public Integer getValue() { return (Integer) value; }
    }

    /// Represents a long literal expression in the AST.
    ///
    /// A long literal expression represents a long integer in the source code, such as `123L` or `456l`.
    /// It is used to represent integer values that exceed the range of the `int` type.
    public final static class LongLiteralExpression extends NumberLiteralExpression {

        /// Constructs a new LongLiteralExpression with the specified line, column, and long value.
        /// @param line The line number in the source code where this expression occurs.
        /// @param column The column number in the source code where this expression starts.
        /// @param value The long value of this literal expression.
        public LongLiteralExpression(int line, int column, Long value) { super(line, column, new TypeToken(PrimitiveType.LONG, line, column), value); }

        @Override
        public Long getValue() { return (Long) value; }
    }

    /// Represents a float literal expression in the AST.
    ///
    /// A float literal expression represents a floating-point number in the source code, such as `3.14f` or `2.0f`.
    public final static class FloatLiteralExpression extends NumberLiteralExpression {

        public FloatLiteralExpression(int line, int column, Float value) { super(line, column, new TypeToken(PrimitiveType.FLOAT, line, column), value); }

        @Override
        public Float getValue() { return (Float) value; }
    }

    /// Represents a double literal expression in the AST.
    ///
    /// A double literal expression represents a double-precision floating-point number in the source code, such as `3.14` or `2.0`.
    /// It is used to represent floating-point values that require more precision than the `float` type can provide.
    public final static class DoubleLiteralExpression extends NumberLiteralExpression {

        /// Constructs a new DoubleLiteralExpression with the specified line, column, and double value.
        /// @param line The line number in the source code where this expression occurs.
        /// @param column The column number in the source code where this expression starts.
        /// @param value The double value of this literal expression.
        public DoubleLiteralExpression(int line, int column, Double value) { super(line, column, new TypeToken(PrimitiveType.DOUBLE, line, column), value); }

        @Override
        public Double getValue() { return (Double) value; }
    }

    /// Represents a byte literal expression in the AST.
    ///
    /// A byte literal expression represents a byte value in the source code, such as `0x1F` or `127`.
    public final static class ByteLiteralExpression extends NumberLiteralExpression {

        /// Constructs a new ByteLiteralExpression with the specified line, column, and byte value.
        /// @param line The line number in the source code where this expression occurs.
        /// @param column The column number in the source code where this expression starts.
        /// @param value The byte value of this literal expression.
        public ByteLiteralExpression(int line, int column, Byte value) { super(line, column, new TypeToken(PrimitiveType.BYTE, line, column), value); }

        @Override
        public Byte getValue() { return (Byte) value; }
    }

    TypeToken typeToken;
    Number value;

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

    @Override
    public String toPrintString() { return "NumberLiteralExpression [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("Value: " + getValue())); }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitNumberLiteral(this); }
}
