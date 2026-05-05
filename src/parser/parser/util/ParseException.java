package parser.parser.util;

import lexer.Token;

/// Represents an exception that occurs during parsing when the input does not conform to the expected grammar.
public class ParseException extends RuntimeException {
    
    private final Token token;

    /// Constructs a new ParseException with the specified error message and the token where the error occurred.
    /// @param message The error message describing the parsing error.
    /// @param token The Token object representing the location in the source code where the parsing error was detected.
    public ParseException(String message, Token token) {

        super(formatMessage(message, token));
        this.token = token;
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
}
