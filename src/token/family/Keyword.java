package src.token.family;

import src.token.TokenFamily;

public enum Keyword implements TokenFamily {
    
    IF("if"), 
    ELSE("else"), 
    WHILE("while"), 
    FOR("for"), 
    CLASS("class"),
    RETURN("return");

    private final String keyword;

    Keyword(String keyword) { this.keyword = keyword; }

    public String get() { return keyword; }
}
