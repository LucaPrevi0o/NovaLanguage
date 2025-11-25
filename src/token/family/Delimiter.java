package src.token.family;

import src.token.TokenFamily;

public enum Delimiter implements TokenFamily {

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LSQUARE("["),
    RSQUARE("]"),
    SEMICOLON(";"),
    COMMA(","),
    COLON(":"),
    DOUBLE_COLON("::"),
    DOT(".");

    private final String symbol;

    Delimiter(String symbol) { this.symbol = symbol; }

    public String get() { return symbol; }
}
