package error.syntax;

import error.Error;
import lexer.token.TokenClass;

/// Represents a syntax error that occurs when an unexpected token is encountered during parsing.
///
/// An unexpected token error indicates that the parser encountered a token that does not fit the
/// expected grammar at that point in the source code.  The error records the offending token type
/// together with the exact source position so that diagnostic messages can pinpoint the problem.
@SyntaxError
public class UnexpectedTokenError implements Error {

    private final TokenClass tokenClass;
    private final int line;
    private final int column;

    /// Constructs an UnexpectedTokenError with full position details.
    /// @param tokenClass The token type that was encountered but was not expected.
    /// @param line       The source line where the unexpected token was found.
    /// @param column     The source column where the unexpected token was found.
    public UnexpectedTokenError(TokenClass tokenClass, int line, int column) {

        this.tokenClass = tokenClass;
        this.line       = line;
        this.column     = column;
    }

    /// Constructs an UnexpectedTokenError without position details.
    /// Prefer the three-argument constructor when source-location information is available.
    /// @param tokenClass The TokenClass object representing the unexpected token.
    public UnexpectedTokenError(TokenClass tokenClass) { this(tokenClass, -1, -1); }

    @Override
    public String getMessage() {

        if (line >= 0)
            return String.format("[line %d, col %d] Unexpected token: '%s'", line, column, tokenClass.token());
        return "Unexpected token: " + tokenClass.token();
    }
}
