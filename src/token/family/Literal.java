package src.token.family;

import src.token.TokenFamily;

public abstract class Literal implements TokenFamily {

    public static final class StringLiteral extends Literal {

        public StringLiteral(String string) { super(string); }
        public StringLiteral() { super(null); }
    }

    public static final class NumberLiteral extends Literal {

        public NumberLiteral(String number) { super(number); }
        public NumberLiteral() { super(null); }
    }

    public static final class BooleanLiteral extends Literal {

        private BooleanLiteral(String bool) { super(bool); }

        public static final BooleanLiteral TRUE = new BooleanLiteral("true");
        public static final BooleanLiteral FALSE = new BooleanLiteral("false");
    }

    public static final class IdentifierLiteral extends Literal {

        public IdentifierLiteral(String identifier) { super(identifier); }
        public IdentifierLiteral() { super(null); }
    }

    private final String name;

    public Literal(String name) { this.name = name; }

    public String get() { return name; }
}
