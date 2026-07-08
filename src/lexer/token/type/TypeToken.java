package lexer.token.type;

import lexer.Token;
import lexer.token.family.PrimitiveType;

/// Represents a token that corresponds to a primitive type in the programming language,
/// such as int, long, byte, float, double, char, bool, string, or void.
public class TypeToken extends Token {

    /// Constructs a new TypeToken with the specified primitive type, line, and column.
    /// @param primitiveType The specific primitive type token (must be a member of the {@link PrimitiveType} family).
    /// @param line The line number where the token appears.
    /// @param column The column number where the token starts.
    public TypeToken(PrimitiveType primitiveType, int line, int column) {
        super(primitiveType, line, column);
    }
}
