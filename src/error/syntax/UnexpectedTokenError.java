package error.syntax;

import error.Error;
import lexer.token.TokenClass;

/// Represents a syntax error that occurs when an unexpected token is encountered during parsing.
///
/// An unexpected token error indicates that the parser encountered a token that does not fit the expected grammar at that point in the source code.
/// This error includes the offending token, which can be used to provide detailed error messages and assist in debugging syntax issues in the source code.
@SyntaxError
public class UnexpectedTokenError implements Error {

    private final TokenClass tokenClass;

    /// Constructs a new UnexpectedTokenError with the specified token that caused the syntax error.
    /// @param tokenClass The TokenClass object representing the unexpected token that was encountered during parsing.
    public UnexpectedTokenError(TokenClass tokenClass) { this.tokenClass = tokenClass; }

    @Override
    public String getMessage() { return "Unexpected token: " + tokenClass.token(); }
}
