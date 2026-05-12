package error.syntax;

import error.Error;
import lexer.token.TokenClass;

/// Represents a syntax error where an expected token is absent at a given position in the source.
///
/// This error is raised by the parser when {@code consume()} is called and the current token does
/// not match the expected token type (i.e., the expected token is simply missing from the source).
/// The error records the source position and the token that was expected, so that diagnostic messages
/// can pinpoint exactly where the omission occurred.
@SyntaxError
public class MissingTokenError implements Error {

    private final int line;
    private final int column;
    private final TokenClass expected;

    /// Constructs a MissingTokenError with full position and expectation details.
    /// @param line     The source line where the missing token was expected.
    /// @param column   The source column where the missing token was expected.
    /// @param expected The token type that was expected but not found.
    public MissingTokenError(int line, int column, TokenClass expected) {

        this.line     = line;
        this.column   = column;
        this.expected = expected;
    }

    /// Constructs a MissingTokenError without position or expectation details.
    /// Prefer the three-argument constructor when source-location information is available.
    public MissingTokenError() { this(-1, -1, null); }

    @Override
    public String getMessage() {

        if (line >= 0 && expected != null)
            return String.format("[line %d, col %d] Missing token: expected '%s'", line, column, expected.token());
        return "Missing token";
    }
}
