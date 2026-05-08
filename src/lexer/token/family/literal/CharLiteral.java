package lexer.token.family.literal;

import lexer.token.family.Literal;

/// Represents a character literal in the programming language, which is a single character enclosed in single quotes (e.g., 'a', '\n', '\\').
public final class CharLiteral extends Literal {

    /// Constructs a new CharLiteral with the specified character value.
    /// @param ch The character value of the literal (e.g., 'a', '\n').
    public CharLiteral(char ch) { super(String.valueOf(ch)); }

    /// Constructs a new CharLiteral with a null value, which can be used as a placeholder or default value.
    public CharLiteral() { super(null); }
}
