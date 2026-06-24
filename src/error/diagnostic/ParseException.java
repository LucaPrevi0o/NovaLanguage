package error.diagnostic;

import lexer.Token;

/// Represents an exception that occurs during parsing when the input does not conform to the expected grammar.
public class ParseException extends RuntimeException {
    
    private final Token token;
    private final Diagnostic diagnostic;

    /// Constructs a new ParseException with the specified error message and the token where the error occurred.
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    public ParseException(String message, Token token) { this(Diagnostic.error(DiagnosticPhase.PARSER, message, token), token); }

    /// Constructs a new ParseException with a specific source location but no Token object.
    /// Useful for errors detected outside the parser where only a line and column are available.
    /// @param message The error message describing the error.
    /// @param line    The source line where the error occurred.
    /// @param column  The source column where the error occurred.
    public ParseException(String message, int line, int column) { this(Diagnostic.error(DiagnosticPhase.PARSER, message, line, column), null); }

    /// Constructs a new ParseException from a prebuilt structured diagnostic.
    /// @param diagnostic The diagnostic represented by this parse exception.
    /// @param token The token where the error occurred, if available.
    public ParseException(Diagnostic diagnostic, Token token) {

        super(formatMessage(diagnostic, token));
        this.token = token;
        this.diagnostic = diagnostic;
    }

    /// Formats the error message to include the line and column information from the token, if available.
    /// @param diagnostic The diagnostic describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    /// @return A formatted error message that includes the line and column information from the token, if available; otherwise, returns the original message.
    private static String formatMessage(Diagnostic diagnostic, Token token) {

        var message = diagnostic != null ? diagnostic.getMessage() : "<unknown parse error>";

        if (token != null)
            return String.format("Parse error at line %d, column %d: %s (near '%s')", token.getLine(), token.getColumn(), message, token);

        if (diagnostic != null && diagnostic.hasLocation())
            return String.format("Parse error at line %d, column %d: %s", diagnostic.getLine(), diagnostic.getColumn(), message);

        return message;
    }

    /// Returns the Token object associated with this ParseException, which represents the location in the source code where the parsing error was detected.
    /// @return The Token object associated with this ParseException, or null if no specific token is associated with this exception.
    public Token getToken() { return token; }

    /// Returns the structured diagnostic represented by this parse exception.
    /// @return The diagnostic for this parse error.
    public Diagnostic getDiagnostic() { return diagnostic; }
}
