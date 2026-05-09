package lexer.token.family;

import lexer.token.TokenClass;

/// Represents keywords in the programming language, such as if, else, while, for, class, and return.
public enum Keyword implements TokenClass {

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
    RETURN("return"),

    /// The null keyword, used for the null literal value.
    NULL("null"),

    /// The break keyword, used to exit from a loop prematurely.
    BREAK("break"),

    /// The continue keyword, used to skip the current iteration and continue with the next one in a loop.
    CONTINUE("continue"),

    /// The new keyword, used for creating instances of classes.
    NEW("new"),

    /// The switch keyword, used for multi-way branching.
    SWITCH("switch"),

    /// The case keyword, used for individual arms inside a switch statement.
    CASE("case"),

    /// The default keyword, used for the fallback arm inside a switch statement.
    DEFAULT("default"),

    /// The import keyword, reserved for future module import support.
    IMPORT("import");

    private final String keyword;

    /// Constructs a new Keyword with the specified string representation.
    /// @param keyword The string representation of the keyword (e.g., "if", "else", "while").
    Keyword(String keyword) { this.keyword = keyword; }

    @Override
    public String token() { return keyword; }
}
