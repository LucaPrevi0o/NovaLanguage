package parser.support;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import error.diagnostic.ParseException;
import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.TypeRegistry;
import lexer.token.family.AccessModifier;

/// Base class for all parsers, providing common token-state helpers.
public abstract class ParserBase {

    protected final ParserState state;
    protected final TypeRegistry typeRegistry;

    /// Constructor for parser components that do not need parse-session type metadata.
    /// @param state The parser state to use for token management.
    protected ParserBase(ParserState state) {
        this(state, null);
    }

    /// Constructor for parser components.
    /// @param state        The parser state to use for token management.
    /// @param typeRegistry The per-session type registry to use, or {@code null} for parsers that do not parse type syntax.
    protected ParserBase(ParserState state, TypeRegistry typeRegistry) {

        this.state = state;
        this.typeRegistry = typeRegistry;
    }

    /// Returns the current parser state.
    /// @return The current parser state.
    public ParserState getState() { return state; }

    /// Returns the type registry for this parse session.
    /// @return The type registry, or {@code null} when this parser component does not parse type syntax.
    public TypeRegistry getTypeRegistry() { return typeRegistry; }

    // ========== Convenience Delegates to State ==========

    /// Returns the current token without consuming it.
    /// @return The current token.
    protected Token getCurrentToken() { return state.getCurrentToken(); }

    /// Returns the getPreviousToken token that was consumed.
    /// @return The getPreviousToken token.
    protected Token getPreviousToken() { return state.getPreviousToken(); }

    /// Checks if the parser has not reached the end of the token stream.
    /// @return `true` if the parser has not reached the end of the token stream; otherwise, `false`.
    protected boolean isNotAtEnd() { return !state.isAtEnd(); }

    /// Advances the parser to the next token and returns the consumed token.
    protected void getNextToken() { state.getNextToken(); }

    /// Returns the current token without consuming it.
    /// @return The current token.
    protected Token peek() { return state.peek(); }

    /// Returns the previously consumed token.
    /// @return The previous token.
    protected Token previous() { return state.previous(); }

    /// Advances by one token and returns the consumed token.
    /// @return The consumed token.
    protected Token advance() { return state.advance(); }

    /// Checks if the current token matches the specified type.
    /// @param type The token family to checkCurrentTokenType against the current token.
    /// @return `true` if the current token matches the specified type; otherwise, `false`.
    protected boolean checkCurrentTokenType(TokenClass type) { return state.checkCurrentTokenType(type); }

    /// Checks if the current token matches the specified type without consuming it.
    /// @param type The token family to check against the current token.
    /// @return `true` if the current token matches the specified type.
    protected boolean check(TokenClass type) { return state.check(type); }

    /// Checks if the current token matches any of the specified types.
    /// @param types The token families to checkCurrentTokenType against the current token.
    /// @return `true` if the current token matches any of the specified types; otherwise, `false`.
    protected boolean checkCurrentTokenType(TokenClass... types) { return state.checkCurrentTokenType(types); }

    /// Checks if the current token matches any of the specified types without consuming it.
    /// @param types The token families to check against the current token.
    /// @return `true` if the current token matches any of the specified types.
    protected boolean check(TokenClass... types) { return state.check(types); }

    /// Checks the current token kind without triggering parser diagnostics.
    /// This is intended for recovery guards that must safely inspect unknown tokens.
    /// @param type The token family to compare against the current token.
    /// @return `true` when the current token has the given token family.
    protected boolean currentTokenIs(TokenClass type) { return isNotAtEnd() && peek().getType() == type; }

    /// Consumes the current token if it matches any of the specified types.
    /// @param types The token families to match.
    /// @return `true` when a token was consumed.
    protected boolean match(TokenClass... types) { return state.match(types); }

    /// Consumes the current token if it matches the expected type; otherwise throws a parse exception.
    /// @param expectedType The token family that the current token is expected to match.
    /// @param message The error message to report when the token is missing.
    /// @return The consumed token.
    protected Token consume(TokenClass expectedType, String message) { return state.consume(expectedType, message); }

    /// Compatibility alias for parser code still being migrated to {@link #consume(TokenClass, String)}.
    protected Token getNextToken(TokenClass expectedType, String message) { return consume(expectedType, message); }

    /// Retrieves the literal value associated with the specified token, if applicable.
    /// @param token The token for which to retrieve the literal value.
    /// @return The literal value associated with the specified token, or `null` if the token does not have a literal value.
    protected String getLiteralValue(Token token) { return state.getLiteralValue(token); }

    /// Builds a parse exception backed by a structured parser diagnostic.
    /// @param message The parser diagnostic message.
    /// @param token The token where the error occurred.
    /// @return A parse exception carrying the diagnostic.
    protected ParseException parseError(String message, Token token) {

        var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, message, token);
        return new ParseException(diagnostic, token);
    }

    /// Builds a parse exception for the current token.
    /// @param message The parser diagnostic message.
    /// @return A parse exception carrying the diagnostic.
    protected ParseException parseError(String message) { return parseError(message, peek()); }

    /// Checks if the provided token is a primitive type token.
    /// @param token The token to inspect.
    /// @return `true` when the token is a type token.
    protected boolean isTypeToken(Token token) { return state.isTypeToken(token); }

    /// Parses an access modifier, which can be "public", "private", or "protected".
    /// If no access modifier is present, null is returned.
    ///
    /// Grammar rule:
    /// `accessModifier → "public" | "private" | "protected"`
    /// @return An AccessModifier enum value representing the parsed access modifier, or `null` if no access modifier is present.
    protected AccessModifier parseAccessModifier() {

        for (var modifier : AccessModifier.values()) if (match(modifier)) return modifier;
        return null;
    }
}
