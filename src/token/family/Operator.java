package src.token.family;

import src.token.TokenFamily;

public enum Operator implements TokenFamily {

    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    ASSIGN("="),
    EQUAL("=="),
    NOT("!"),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    INCREMENT("++"),
    DECREMENT("--"),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||");

    private final String symbol;

    Operator(String symbol) { this.symbol = symbol; }

    public String get() { return symbol; }
}
