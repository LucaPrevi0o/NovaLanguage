package src.parser.ast.nodes.expression.literal;

import src.parser.ast.nodes.ExpressionNode;
import src.token.family.PrimitiveType;
import src.lexer.token.TypeToken;

public abstract class NumberLiteralExpression extends ExpressionNode {

    public final static class IntegerLiteralExpression extends NumberLiteralExpression {

        public IntegerLiteralExpression(int line, int column, Integer value) { super(line, column, new TypeToken(PrimitiveType.INT, line, column), value); }

        @Override
        public Integer getValue() { return (Integer) value; }
    }

    public final static class LongLiteralExpression extends NumberLiteralExpression {

        public LongLiteralExpression(int line, int column, Long value) { super(line, column, new TypeToken(PrimitiveType.LONG, line, column), value); }

        @Override
        public Long getValue() { return (Long) value; }
    }

    public final static class FloatLiteralExpression extends NumberLiteralExpression {

        public FloatLiteralExpression(int line, int column, Float value) { super(line, column, new TypeToken(PrimitiveType.FLOAT, line, column), value); }

        @Override
        public Float getValue() { return (Float) value; }
    }

    public final static class DoubleLiteralExpression extends NumberLiteralExpression {

        public DoubleLiteralExpression(int line, int column, Double value) { super(line, column, new TypeToken(PrimitiveType.DOUBLE, line, column), value); }

        @Override
        public Double getValue() { return (Double) value; }
    }

    public final static class ByteLiteralExpression extends NumberLiteralExpression {

        public ByteLiteralExpression(int line, int column, Byte value) { super(line, column, new TypeToken(PrimitiveType.BYTE, line, column), value); }

        @Override
        public Byte getValue() { return (Byte) value; }
    }

    TypeToken typeToken;
    Number value;

    public NumberLiteralExpression(int line, int column, TypeToken typeToken, Number value) {

        super(line, column);
        this.typeToken = typeToken;
        this.value = value;
    }

    public TypeToken getTypeToken() { return typeToken; }
    public abstract Number getValue();
}
