package token.family;

import token.TokenFamily;

/// Represents operators in the programming language, such as arithmetic, assignment, and logical operators.
public enum Operator implements TokenFamily {

    /// The plus operator, used for addition.
    PLUS("+"),

    /// The minus operator, used for subtraction.
    MINUS("-"),

    /// The multiply operator, used for multiplication.
    MULTIPLY("*"),

    /// The divide operator, used for division.
    DIVIDE("/"),

    /// The modulo operator, used for computing the remainder of division.
    MODULO("%"),

    /// The assign operator, used for assignment.
    ASSIGN("="),

    /// The plus-assign operator, used for addition assignment (a += 5).
    PLUS_ASSIGN("+="),

    /// The minus-assign operator, used for subtraction assignment (a -= 5).
    MINUS_ASSIGN("-="),

    /// The multiply-assign operator, used for multiplication assignment (a *= 5).
    MULTIPLY_ASSIGN("*="),

    /// The divide-assign operator, used for division assignment (a /= 5).
    DIVIDE_ASSIGN("/="),

    /// The equal operator, used for equality comparison.
    EQUAL("=="),

    /// The not operator, used for logical negation.
    NOT("!"),

    /// The not equal operator, used for inequality comparison.
    NOT_EQUAL("!="),

    /// The less than operator, used for comparing if one value is less than another.
    LESS_THAN("<"),

    /// The greater than operator, used for comparing if one value is greater than another.
    GREATER_THAN(">"),

    /// The less than or equal to operator, used for comparing if one value is less than or equal to another.
    LESS_EQUAL("<="),

    /// The greater than or equal to operator, used for comparing if one value is greater than or equal to another.
    GREATER_EQUAL(">="),

    /// The increment operator, used for increasing a value by one.
    INCREMENT("++"),

    /// The decrement operator, used for decreasing a value by one.
    DECREMENT("--"),

    /// The logical AND operator, used for logical conjunction.
    LOGICAL_AND("&&"),

    /// The logical OR operator, used for logical disjunction.
    LOGICAL_OR("||");

    private final String symbol;

    /// Constructs a new Operator with the specified symbol.
    /// @param symbol The string representation of the operator (e.g., "+", "-", "*", "/").
    Operator(String symbol) { this.symbol = symbol; }

    @Override
    public String get() { return symbol; }
}
