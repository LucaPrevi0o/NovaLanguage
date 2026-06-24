package error.diagnostic;

import lexer.Token;
import lexer.token.TokenClass;

/// Immutable compiler diagnostic with optional source position and token context.
/// @param severity The severity of the diagnostic (e.g., error, warning, info).
/// @param phase The phase of the compiler in which the diagnostic was generated.
/// @param message The diagnostic message.
/// @param line The line number of the source position (or `-1` if not applicable).
/// @param column The column number of the source position (or `-1` if not applicable).
/// @param spanLength The length of the lexeme associated with the diagnostic (or `-1` if not applicable).
/// @param expectedToken The expected token class for the source token (or `null` if not applicable).
/// @param actualToken The actual token class for the source token (or `null` if not applicable).
/// @param lexeme The lexeme associated with the diagnostic (or `null` if not applicable).
public record Diagnostic(DiagnosticSeverity severity, DiagnosticPhase phase, String message, int line, int column,
int spanLength, TokenClass expectedToken, TokenClass actualToken, String lexeme) {

    /// Creates a new diagnostic error with the given compiler phase and diagnostics message.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    /// @return A new diagnostic error with the specified phase and message.
    public static Diagnostic error(DiagnosticPhase phase, String message) {
        return new Diagnostic(DiagnosticSeverity.ERROR, phase, message, -1, -1, -1, null, null, null);
    }

    /// Creates a new diagnostic error with the given compiler phase, diagnostics message, and source position.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    /// @param line The line number of the source position.
    /// @param column The column number of the source position.
    /// @return A new diagnostic error with the specified phase, message, and source position.
    public static Diagnostic error(DiagnosticPhase phase, String message, int line, int column) {
        return new Diagnostic(DiagnosticSeverity.ERROR, phase, message, line, column, -1, null, null, null);
    }

    /// Creates a new diagnostic error with the given compiler phase, diagnostics message, and source token.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    /// @param token The source token associated with the diagnostic.
    /// @return A new diagnostic error with the specified phase, message, and source token.
    public static Diagnostic error(DiagnosticPhase phase, String message, Token token) {
        return fromToken(phase, message, token, null);
    }

    /// Creates a new diagnostic error with the given compiler phase, diagnostics message, source token, and expected token class.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    /// @param token The source token associated with the diagnostic.
    /// @param expectedToken The expected token class for the source token.
    /// @return A new diagnostic error with the specified phase, message, source token, and expected token class.
    public static Diagnostic error(DiagnosticPhase phase, String message, Token token, TokenClass expectedToken) {
        return fromToken(phase, message, token, expectedToken);
    }

    /// Creates a new diagnostic with the given compiler phase, diagnostics message, source token, and expected token class.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    /// @param token The source token associated with the diagnostic.
    /// @param expectedToken The expected token class for the source token.
    /// @return A new diagnostic with the specified phase, message, source token, and expected token class.
    private static Diagnostic fromToken(DiagnosticPhase phase, String message, Token token, TokenClass expectedToken) {

        if (token == null) return new Diagnostic(DiagnosticSeverity.ERROR, phase, message, -1, -1, -1, expectedToken, null, null);
        return new Diagnostic(
                DiagnosticSeverity.ERROR,
                phase,
                message,
                token.getLine(),
                token.getColumn(),
                tokenLength(token),
                expectedToken,
                token.getType(),
                tokenLexeme(token)
        );
    }

    /// Returns the length of the lexeme associated with the given token, or -1 if the token has no lexeme.
    /// @param token The token to get the lexeme length for.
    /// @return The length of the lexeme associated with the given token, or `-1` if the token has no lexeme.
    private static int tokenLength(Token token) {

        var lexeme = tokenLexeme(token);
        return lexeme != null ? lexeme.length() : -1;
    }

    /// Returns the lexeme associated with the given token, or `null` if the token has no lexeme.
    /// @param token The token to get the lexeme for.
    /// @return The lexeme associated with the given token, or `null` if the token has no lexeme.
    private static String tokenLexeme(Token token) {

        if (token.getLexeme() != null) return token.getLexeme();
        if (token.getType() != null) return token.getType().token();
        return null;
    }

    /// Returns whether this diagnostic has a source position (line and column).
    /// @return `true` if this diagnostic has a source position, `false` otherwise.
    public boolean hasLocation() { return line >= 0 && column >= 0; }

    /// Returns whether this diagnostic has a span length (length of the lexeme).
    /// @return `true` if this diagnostic has a span length, `false` otherwise.
    public boolean hasSpan() { return spanLength >= 0; }

    /// Returns a formatted string representation of this diagnostic.
    /// @return A formatted string representation of this diagnostic.
    public String format() {

        var prefix = "[" + phase + " " + severity + "]";
        if (hasLocation()) return String.format("%s line %d, col %d: %s", prefix, line, column, message);
        return prefix + " " + message;
    }
}
