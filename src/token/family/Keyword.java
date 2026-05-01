package src.token.family;

import src.token.TokenFamily;

/// Represents keywords in the programming language, such as if, else, while, for, class, and return.
public enum Keyword implements TokenFamily {

    /// The if keyword, used for conditional statements.
    IF("if"),

    /// The else keyword, used for providing an alternative block of code in conditional statements.
    ELSE("else"),

    /// The while keyword, used for creating loops that execute as long as a specified condition is true.
    WHILE("while"),

    /// The for keyword, used for creating loops that iterate over a range of values or collections.
    FOR("for"),

    /// The class keyword, used for defining classes in object-oriented programming.
    CLASS("class"),

    /// The return keyword, used for returning a value from a function or method.
    RETURN("return");

    private final String keyword;

    /// Constructs a new Keyword with the specified string representation.
    /// @param keyword The string representation of the keyword (e.g., "if", "else", "while").
    Keyword(String keyword) { this.keyword = keyword; }

    @Override
    public String get() { return keyword; }
}
