package lexer.token.type;

import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;

/// Represents a token that corresponds to a type in the programming language, such as primitive types (int, long, byte,
/// float, double, char, bool, void) or non-primitive types (class names, interface names).
public class TypeToken extends Token {

    /// Constructs a new TypeToken with the specified primitive type, line, and column.
    /// @param primitiveType The specific primitive type token (must be a member of the {@link PrimitiveType} family).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public TypeToken(PrimitiveType primitiveType, int line, int column) {
        super(primitiveType, line, column);
    }

    /// Constructs a new TypeToken with the specified non-primitive type, line, and column.
    /// @param nonPrimitiveType The specific non-primitive type token (must be a member of the {@link NonPrimitiveType} family).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public TypeToken(NonPrimitiveType nonPrimitiveType, int line, int column) {
        super(nonPrimitiveType, line, column);
    }
}
