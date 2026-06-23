package parser.parser.util;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Token;

/// Represents an exception that occurs during parsing when the input does not conform to the expected grammar.
public class ParseException extends RuntimeException {
    
    private final Token token;
    private final Diagnostic diagnostic;

    /// Constructs a new ParseException with the specified error message and the token where the error occurred.
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    public ParseException(String message, Token token) {

        super(formatMessage(message, token));
        this.token = token;
        this.diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, message, token);
    }

    /// Constructs a new ParseException with a specific source location but no Token object.
    /// Useful for errors detected outside the parser where only a line and column are available.
    /// @param message The error message describing the error.
    /// @param line    The source line where the error occurred.
    /// @param column  The source column where the error occurred.
    public ParseException(String message, int line, int column) {

        super(String.format("Parse error at line %d, column %d: %s", line, column, message));
        this.token = null;
        this.diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, message, line, column);
    }

    /// Formats the error message to include the line and column information from the token, if available.
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    /// @return A formatted error message that includes the line and column information from the token, if available; otherwise, returns the original message.
    private static String formatMessage(String message, Token token) {

        if (token != null)
            return String.format("Parse error at line %d, column %d: %s (near '%s')", token.getLine(), token.getColumn(), message, token);
        return message;
    }

    /// Returns the Token object associated with this ParseException, which represents the location in the source code where the parsing error was detected.
    /// @return The Token object associated with this ParseException, or null if no specific token is associated with this exception.
    public Token getToken() { return token; }

    /// Returns the structured diagnostic represented by this parse exception.
    /// @return The diagnostic for this parse error.
    public Diagnostic getDiagnostic() { return diagnostic; }
}
