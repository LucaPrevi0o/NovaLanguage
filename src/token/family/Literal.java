package src.token.family;

import src.token.TokenFamily;

/// Represents literals in the programming language, such as string literals, number literals, boolean literals, and identifier literals.
public abstract class Literal implements TokenFamily {

    /// Represents a string literal in the programming language, which is a sequence of characters enclosed in double quotes (e.g., "Hello, World!").
    public static final class StringLiteral extends Literal {

        /// Constructs a new StringLiteral with the specified string value.
        /// @param string The string value of the literal (e.g., "Hello, World!").
        public StringLiteral(String string) { super(string); }

        /// Constructs a new StringLiteral with a null value, which can be used as a placeholder or default value.
        public StringLiteral() { super(null); }
    }

    /// Represents a number literal in the programming language, which can be an integer or a floating-point number (e.g., 42, 3.14).
    public static final class NumberLiteral extends Literal {

        /// Constructs a new NumberLiteral with the specified numeric value.
        /// @param number The string representation of the numeric value (e.g., "42",
        public NumberLiteral(String number) { super(number); }

        /// Constructs a new NumberLiteral with a null value, which can be used as a placeholder or default value.
        public NumberLiteral() { super(null); }
    }

    /// Represents a boolean literal in the programming language, which can be either true or false.
    public static final class BooleanLiteral extends Literal {

        /// Constructs a new BooleanLiteral with the specified boolean value.
        /// @param bool The string representation of the boolean value (e.g., "true", "false").
        private BooleanLiteral(String bool) { super(bool); }

        /// The boolean literal representing the true value in the programming language.
        public static final BooleanLiteral TRUE = new BooleanLiteral("true");

        /// The boolean literal representing the false value in the programming language.
        public static final BooleanLiteral FALSE = new BooleanLiteral("false");
    }

    /// Represents a character literal in the programming language, which is a single character enclosed in single quotes (e.g., 'a', '\n', '\\').
    public static final class CharLiteral extends Literal {

        /// Constructs a new CharLiteral with the specified character value.
        /// @param ch The character value of the literal (e.g., 'a', '\n').
        public CharLiteral(char ch) { super(String.valueOf(ch)); }

        /// Constructs a new CharLiteral with a null value, which can be used as a placeholder or default value.
        public CharLiteral() { super(null); }
    }

    /// Represents an identifier literal in the programming language, which is a name used to identify variables,
    /// functions, classes, and other entities (e.g., myVariable, myFunction).
    public static final class IdentifierLiteral extends Literal {

        /// Constructs a new IdentifierLiteral with the specified identifier name.
        /// @param identifier The name of the identifier (e.g., "myVariable", "myFunction").
        public IdentifierLiteral(String identifier) { super(identifier); }

        /// Constructs a new IdentifierLiteral with a null value, which can be used as a placeholder or default value.
        public IdentifierLiteral() { super(null); }
    }

    private final String name;

    /// Constructs a new Literal with the specified name or value.
    /// @param name The string representation of the literal, which can be a string value, a numeric value, a boolean
    /// value, or an identifier name, depending on the type of literal being represented.
    public Literal(String name) { this.name = name; }

    @Override
    public String get() { return name; }
}
