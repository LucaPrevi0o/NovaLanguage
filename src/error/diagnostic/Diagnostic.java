package error.diagnostic;

import lexer.Token;
import lexer.token.TokenClass;

/// Immutable compiler diagnostic with optional source position and token context.
public final class Diagnostic {

    private final DiagnosticSeverity severity;
    private final DiagnosticPhase phase;
    private final String message;
    private final int line;
    private final int column;
    private final int spanLength;
    private final TokenClass expectedToken;
    private final TokenClass actualToken;
    private final String lexeme;

    public Diagnostic(
            DiagnosticSeverity severity,
            DiagnosticPhase phase,
            String message,
            int line,
            int column,
            int spanLength,
            TokenClass expectedToken,
            TokenClass actualToken,
            String lexeme
    ) {

        this.severity = severity;
        this.phase = phase;
        this.message = message;
        this.line = line;
        this.column = column;
        this.spanLength = spanLength;
        this.expectedToken = expectedToken;
        this.actualToken = actualToken;
        this.lexeme = lexeme;
    }

    public static Diagnostic error(DiagnosticPhase phase, String message) {
        return new Diagnostic(DiagnosticSeverity.ERROR, phase, message, -1, -1, -1, null, null, null);
    }

    public static Diagnostic error(DiagnosticPhase phase, String message, int line, int column) {
        return new Diagnostic(DiagnosticSeverity.ERROR, phase, message, line, column, -1, null, null, null);
    }

    public static Diagnostic error(DiagnosticPhase phase, String message, Token token) {
        return fromToken(DiagnosticSeverity.ERROR, phase, message, token, null);
    }

    public static Diagnostic error(DiagnosticPhase phase, String message, Token token, TokenClass expectedToken) {
        return fromToken(DiagnosticSeverity.ERROR, phase, message, token, expectedToken);
    }

    public static Diagnostic fromError(error.Error error, DiagnosticPhase phase) {
        return error(phase, error != null ? error.getMessage() : "<unknown diagnostic>");
    }

    public static Diagnostic fromError(error.Error error, DiagnosticPhase phase, Token token) {
        return error(phase, error != null ? error.getMessage() : "<unknown diagnostic>", token);
    }

    private static Diagnostic fromToken(
            DiagnosticSeverity severity,
            DiagnosticPhase phase,
            String message,
            Token token,
            TokenClass expectedToken
    ) {

        if (token == null) return new Diagnostic(severity, phase, message, -1, -1, -1, expectedToken, null, null);
        return new Diagnostic(
                severity,
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

    private static int tokenLength(Token token) {

        var lexeme = tokenLexeme(token);
        return lexeme != null ? lexeme.length() : -1;
    }

    private static String tokenLexeme(Token token) {

        if (token.getLexeme() != null) return token.getLexeme();
        if (token.getType() != null) return token.getType().token();
        return null;
    }

    public DiagnosticSeverity getSeverity() { return severity; }

    public DiagnosticPhase getPhase() { return phase; }

    public String getMessage() { return message; }

    public int getLine() { return line; }

    public int getColumn() { return column; }

    public int getSpanLength() { return spanLength; }

    public TokenClass getExpectedToken() { return expectedToken; }

    public TokenClass getActualToken() { return actualToken; }

    public String getLexeme() { return lexeme; }

    public boolean hasLocation() { return line >= 0 && column >= 0; }

    public boolean hasSpan() { return spanLength >= 0; }

    public String format() {

        var prefix = "[" + phase + " " + severity + "]";
        if (hasLocation()) return String.format("%s line %d, col %d: %s", prefix, line, column, message);
        return prefix + " " + message;
    }
}
