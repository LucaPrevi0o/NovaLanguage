package parser.parser.util;

import lexer.Token;

/// Represents an exception that occurs during parsing when the input does not conform to the expected grammar.
public class ParseException extends RuntimeException {
    
    private final Token token;
    private final Diagnostic diagnostic;

    /// Constructs a new ParseException with the specified error message and the token where the error occurred.
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    public ParseException(String message, Token token) { this(DiagnosticCode.SYNTAX_ERROR, message, token); }

    /// Constructs a ParseException with an explicit diagnostic code.
    /// @param code The DiagnosticCode representing the type of diagnostic (e.g., syntax error).
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    public ParseException(DiagnosticCode code, String message, Token token) {

        super(formatMessage(code, message, token));
        this.token = token;
        this.diagnostic = buildDiagnostic(code, message, token);
    }

    /// Constructs a new ParseException with a specific source location but no Token object.
    /// Useful for errors detected outside the parser where only a line and column are available.
    /// @param message The error message describing the error.
    /// @param line    The source line where the error occurred.
    /// @param column  The source column where the error occurred.
    public ParseException(String message, int line, int column) {

        super(String.format("%s at line %d, column %d: %s%n  %s", DiagnosticCode.SYNTAX_ERROR, line, column, message, new SourceSpan(line, column, column).toCaretLine()));
        this.token = null;
        this.diagnostic = new Diagnostic(DiagnosticCode.SYNTAX_ERROR, message, new SourceSpan(line, column, column), null);
    }

    /// Formats the error message to include the line and column information from the token, if available.
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    /// @return A formatted error message that includes the line and column information from the token, if available; otherwise, returns the original message.
    private static String formatMessage(DiagnosticCode code, String message, Token token) {

        if (token != null) {

            var span = new SourceSpan(token.getLine(), token.getColumn(), token.getColumn());
            return String.format("%s at line %d, column %d: %s (near '%s')%n  %s",
                code, token.getLine(), token.getColumn(), message, token, span.toCaretLine());
        }
        return code + ": " + message;
    }

    /// Builds a Diagnostic object based on the provided diagnostic code, message, and token information.
    /// @param code The DiagnosticCode representing the type of diagnostic (e.g., syntax error).
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    /// @return A Diagnostic object containing the diagnostic code, message, source span, and any relevant token information for this parse exception.
    private static Diagnostic buildDiagnostic(DiagnosticCode code, String message, Token token) {

        if (token == null) return new Diagnostic(code, message, new SourceSpan(0, 1, 1), null);
        return new Diagnostic(code, message, new SourceSpan(token.getLine(), token.getColumn(), token.getColumn()), token.toString());
    }

    /// Returns the Token object associated with this ParseException, which represents the location in the source code where the parsing error was detected.
    /// @return The Token object associated with this ParseException, or null if no specific token is associated with this exception.
    public Token getToken() { return token; }

    /// Returns a structured diagnostic representation of this parse exception.
    /// @return A Diagnostic object containing the diagnostic code, message, source span, and any relevant token information for this parse exception.
    public Diagnostic getDiagnostic() { return diagnostic; }
}
