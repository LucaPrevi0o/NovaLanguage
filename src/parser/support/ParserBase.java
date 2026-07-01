package parser.support;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import error.diagnostic.ParseException;
import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.AccessModifier;

/// Base class for all parsers, providing common token-state helpers.
public abstract class ParserBase {

    /// The parser state to use for token management.
    protected final ParserState state;

    /// Constructor for parser components.
    /// @param state The parser state to use for token management.
    protected ParserBase(ParserState state) { this.state = state; }

    /// Returns the current parser state.
    /// @return The current parser state.
    public ParserState getState() { return state; }

    // ========== Convenience Delegates to State ==========

    /// Checks the current token kind without triggering parser diagnostics.
    /// This is intended for recovery guards that must safely inspect unknown tokens.
    /// @param type The token family to compare against the current token.
    /// @return `true` when the current token has the given token family.
    protected boolean currentTokenIs(TokenClass type) { return !state.isAtEnd() && state.peek().getType() == type; }

    /// Builds a parse exception backed by a structured parser diagnostic.
    /// @param message The parser diagnostic message.
    /// @param token The token where the error occurred.
    /// @return A parse exception carrying the diagnostic.
    protected ParseException parseError(String message, Token token) {

        var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, message, token);
        return new ParseException(diagnostic, token);
    }

    /// Parses an access modifier, which can be "public", "private", or "protected".
    /// If no access modifier is present, null is returned.
    ///
    /// Grammar rule:
    /// `accessModifier → "public" | "private" | "protected"`
    /// @return An AccessModifier enum value representing the parsed access modifier, or `null` if no access modifier is present.
    protected AccessModifier parseAccessModifier() {

        for (var modifier : AccessModifier.values()) if (state.match(modifier)) return modifier;
        return null;
    }
}
