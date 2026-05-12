package error.syntax;

import error.Error;

/// Represents a syntax error caused by a token whose character sequence is not part of the language alphabet.
///
/// An unrecognized token error is raised when the lexer produces a {@link lexer.token.family.Special#UNKNOWN}
/// token for a character (or sequence of characters) that does not belong to any valid token class.
/// The error records the raw text and the exact source position so that the user can locate the offending
/// character in their source file.
@SyntaxError
public class UnrecognizedTokenError implements Error {

    private final String value;
    private final int line;
    private final int column;

    /// Constructs an UnrecognizedTokenError for the given character sequence at the specified source location.
    /// @param value  The raw text of the unrecognized token.
    /// @param line   The source line where the unrecognized token was found.
    /// @param column The source column where the unrecognized token starts.
    public UnrecognizedTokenError(String value, int line, int column) {

        this.value  = value;
        this.line   = line;
        this.column = column;
    }

    @Override
    public String getMessage() {
        return String.format("[line %d, col %d] Unrecognized token: '%s'", line, column, value);
    }
}
