package src.parser.parser.util;

import src.lexer.Token;

/**
 * Exception thrown when a parsing error occurs.
 */
public class ParseException extends RuntimeException {
    
    private final Token token;
    
    public ParseException(String message, Token token) {
        super(formatMessage(message, token));
        this.token = token;
    }
    
    public ParseException(String message) {
        super(message);
        this.token = null;
    }
    
    private static String formatMessage(String message, Token token) {
        if (token != null) {
            return String.format("Parse error at line %d, column %d: %s (near '%s')",
                token.getLine(), token.getColumn(), message, token);
        }
        return message;
    }
    
    public Token getToken() {
        return token;
    }
}
