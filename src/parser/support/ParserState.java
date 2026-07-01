package parser.support;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import error.diagnostic.ParseException;
import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.type.TypeToken;
import lexer.token.type.LiteralToken;
import lexer.token.family.Literal;
import lexer.token.family.Special;

import java.util.ArrayList;
import java.util.List;

/// Represents the current state of the parser, including the list of tokens and the current position in that list.
/// Provides methods for navigating and checking tokens during parsing.
public class ParserState {
    
    private final List<Token> tokens;
    private final List<ParseException> errors = new ArrayList<>();
    private final DiagnosticBag diagnosticBag;
    private int current = 0;

    /// Constructs a new ParserState with the given list of tokens.
    /// @param tokens The list of tokens to be parsed.
    public ParserState(List<Token> tokens) { this(tokens, new DiagnosticBag()); }

    /// Constructs a new ParserState with the given token list and diagnostic bag.
    /// @param tokens The list of tokens to be parsed.
    /// @param diagnosticBag The diagnostic bag used to collect parser diagnostics.
    public ParserState(List<Token> tokens, DiagnosticBag diagnosticBag) {

        this.tokens = tokens;
        this.diagnosticBag = diagnosticBag != null ? diagnosticBag : new DiagnosticBag();
    }

    /// Returns the diagnostic bag used to collect parser diagnostics.
    /// @return The diagnostic bag.
    public DiagnosticBag getDiagnosticBag() { return diagnosticBag; }

    // ========== Error Reporting ==========

    /// Records a parse error and its structured diagnostic.
    /// @param error The parse error to record.
    public void report(ParseException error) {

        if (error == null) return;
        errors.add(error);
        diagnosticBag.report(error.getDiagnostic());
    }

    /// Clears parse errors and diagnostics from a previous parse run.
    public void clearErrors() {

        errors.clear();
        diagnosticBag.clear();
    }

    /// Returns the parse errors collected during the current parse run.
    /// @return An immutable list of parse errors.
    public List<ParseException> getErrors() { return List.copyOf(errors); }

    // ========== Token Navigation ==========

    /// Returns the current token without advancing the position.
    /// @return The current token.
    public Token peek() { return tokens.get(current); }

    /// Returns the token most recently consumed by {@link #advance()}.
    /// @return The previous token.
    public Token previous() { return tokens.get(current - 1); }

    /// Checks if the parser has reached the end of the token list.
    /// @return `true` if the current token is the end-of-file token, `false` otherwise.
    public boolean isAtEnd() { return peek().getType() == Special.EOF; }

    /// Advances the parser by one token and returns the consumed token.
    /// @return The consumed token.
    public Token advance() {

        if (!isAtEnd()) current++;
        return previous();
    }
    
    // ========== Token Checking ==========

    /// Checks whether the current token matches the given token class without consuming it.
    /// @param type The expected token class.
    /// @return `true` when the current token matches.
    public boolean check(TokenClass type) {

        if (isAtEnd()) return false;
        rejectUnknownToken();

        var currentType = peek().getType();
        if (type instanceof Literal litType && currentType instanceof Literal litCurrent) {

            var typeVal = litType.token();
            if (typeVal != null) return typeVal.equals(litCurrent.token());  // specific-value checkCurrentTokenType (e.g. TRUE vs FALSE)
            return type.getClass() == currentType.getClass();              // wildcard class checkCurrentTokenType
        }
        return currentType == type;
    }

    /// Checks whether the current token matches any of the given token classes without consuming it.
    /// @param types The expected token classes.
    /// @return `true` when the current token matches any expected token class.
    public boolean check(TokenClass... types) {

        for (var type : types)
            if (check(type)) return true;
        return false;
    }

    /// Consumes the current token if it matches one of the given token classes.
    /// @param types The token classes to match.
    /// @return `true` when a token was consumed.
    public boolean match(TokenClass... types) {

        for (var type : types) if (check(type)) {

            advance();
            return true;
        }
        return false;
    }

    /// Consumes the current token if it matches the specified token family, advancing the position.
    /// If the current token does not match, throws a parse exception with expected/actual token context.
    /// @param expectedType The token family to checkCurrentTokenType against the current token.
    /// @param message The parse error message.
    /// @return The token that was consumed if it matches the specified token family.
    public Token consume(TokenClass expectedType, String message) {

        if (check(expectedType)) return advance();

        var actual = peek();
        var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, message, actual, expectedType);
        throw new ParseException(diagnostic, actual);
    }

    /// Consumes the current token if it matches the specified token family, advancing the position.
    /// > Compatibility alias for older parser code. This method is deprecated and will be removed in future versions.
    /// @param expectedType The token family to checkCurrentTokenType against the current token.
    /// @param message The parse error message.
    /// @return The token that was consumed if it matches the specified token family.
    /// @deprecated Use {@link #consume(TokenClass, String)} instead.
    @Deprecated
    public Token getNextToken(TokenClass expectedType, String message) { return consume(expectedType, message); }

    // ========== Helper Methods ==========

    /// Checks if the current token is a literal token.
    /// @param token The token to checkCurrentTokenType.
    /// @return The literal value of the token if it is an instance of LiteralToken.
    /// @throws ParseException If the token is not an instance of LiteralToken.
    public String getLiteralValue(Token token) {

        if (token instanceof LiteralToken lit) return lit.getValue();
        throw new ParseException("Expected literal token", token);
    }

    /// Checks whether a token represents a primitive type token.
    /// @param token The token to inspect.
    /// @return `true` when the token is a type token.
    public boolean isTypeToken(Token token) { return token instanceof TypeToken; }

    /// Returns the current token stream position.
    /// Used to save and restore the position for backtracking during ambiguous parses (e.g. for vs for-each).
    /// @return The current position index.
    public int getCurrentPosition() { return current; }

    /// Restores a previously saved token stream position for backtracking.
    /// @param position The position to restore to.
    public void setCurrentPosition(int position) { current = position; }

    /// Rejects the current token if it is an unknown token, throwing a parse exception with a diagnostic message.
    private void rejectUnknownToken() {

        if (peek().getType() == Special.UNKNOWN) {

            var token = peek();
            var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, "Unrecognized token: '" + tokenLexeme(token) + "'", token);
            throw new ParseException(diagnostic, peek());
        }
    }

    /// Returns a string representation of the token's lexeme for error reporting.
    /// @param token The token to get the lexeme for.
    /// @return The lexeme of the token, or `<unknown>` if the lexeme is not available.
    private String tokenLexeme(Token token) {

        if (token == null) return "<unknown>";
        if (token.getLexeme() != null) return token.getLexeme();
        if (token.getType() != null && token.getType().token() != null) return token.getType().token();
        return "<unknown>";
    }
}
