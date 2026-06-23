package error.syntax;

import error.Error;
import lexer.Token;
import lexer.token.TokenClass;

/// Represents a syntax error that occurs when an unexpected token is encountered during parsing.
///
/// An unexpected token error indicates that the parser encountered a token that does not fit the
/// expected grammar at that point in the source code.  The error records the offending token type
/// together with the exact source position so that diagnostic messages can pinpoint the problem.
@SyntaxError
public class UnexpectedTokenError implements Error {

    private final TokenClass expected;
    private final TokenClass actual;
    private final int line;
    private final int column;

    /// Constructs an UnexpectedTokenError with full position details.
    /// @param tokenClass The token type that was encountered but was not expected.
    /// @param line       The source line where the unexpected token was found.
    /// @param column     The source column where the unexpected token was found.
    public UnexpectedTokenError(TokenClass tokenClass, int line, int column) { this(null, tokenClass, line, column); }

    /// Constructs an UnexpectedTokenError with expected and actual token details.
    /// @param expected The token type expected at the error location.
    /// @param actual   The token type encountered at the error location.
    /// @param line     The source line where the unexpected token was found.
    /// @param column   The source column where the unexpected token was found.
    public UnexpectedTokenError(TokenClass expected, TokenClass actual, int line, int column) {

        this.expected = expected;
        this.actual   = actual;
        this.line     = line;
        this.column   = column;
    }

    /// Constructs an UnexpectedTokenError from the encountered token.
    /// @param actual The token encountered at the error location.
    public UnexpectedTokenError(Token actual) { this(null, actual); }

    /// Constructs an UnexpectedTokenError from an expected token class and the encountered token.
    /// @param expected The token type expected at the error location.
    /// @param actual   The token encountered at the error location.
    public UnexpectedTokenError(TokenClass expected, Token actual) {
        this(
            expected,
            actual != null ? actual.getType() : null,
            actual != null ? actual.getLine() : -1,
            actual != null ? actual.getColumn() : -1
        );
    }

    /// Constructs an UnexpectedTokenError without position details.
    /// Prefer the three-argument constructor when source-location information is available.
    /// @param tokenClass The TokenClass object representing the unexpected token.
    public UnexpectedTokenError(TokenClass tokenClass) { this(tokenClass, -1, -1); }

    @Override
    public String getMessage() {

        var detail = expected != null
            ? String.format("expected '%s' but found '%s'", describe(expected), describe(actual))
            : String.format("'%s'", describe(actual));

        if (line >= 0) return String.format("[line %d, col %d] Unexpected token: %s", line, column, detail);
        return "Unexpected token: " + detail;
    }

    private static String describe(TokenClass tokenClass) {

        if (tokenClass == null) return "<unknown>";

        var token = tokenClass.token();
        if (token != null) return token;

        return tokenClass.getClass().getSimpleName();
    }
}
