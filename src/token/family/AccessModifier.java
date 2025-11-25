package src.token.family;

import src.token.TokenFamily;

public enum AccessModifier implements TokenFamily { 

    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected");

    private final String keyword;

    AccessModifier(String keyword) { this.keyword = keyword; }

    public String get() { return keyword; }
}
