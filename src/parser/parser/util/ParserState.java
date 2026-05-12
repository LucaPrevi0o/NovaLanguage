package parser.parser.util;

import error.ErrorCollector;
import error.syntax.MissingTokenError;
import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.family.*;
import lexer.token.type.LiteralToken;

import java.util.List;

/// Represents the current state of the parser, including the list of tokens and the current position in that list.
/// Provides methods for navigating and checking tokens during parsing.
public class ParserState {
    
    private final List<Token> tokens;
    private int current = 0;

    /// Constructs a new ParserState with the given list of tokens.
    /// @param tokens The list of tokens to be parsed.
    public ParserState(List<Token> tokens) { this.tokens = tokens; }

    // ========== Token Navigation ==========

    /// Returns the current token without advancing the position.
    /// @return The current token.
    public Token peek() { return tokens.get(current); }

    /// Returns the previous token that was consumed.
    /// @return The previous token.
    public Token previous() { return tokens.get(current - 1); }

    /// Checks if the parser has reached the end of the token list.
    /// @return True if the current token is the end-of-file token, false otherwise.
    public boolean isAtEnd() { return peek().getType() == Special.EOF; }

    /// Advances the current position and returns the token that was just consumed.
    /// @return The token that was consumed by advancing the position.
    public Token advance() {

        if (!isAtEnd()) current++;
        return previous();
    }
    
    // ========== Token Checking ==========

    /// Checks if the current token matches the specified token family.
    ///
    /// Matching rules for {@link Literal} types:
    /// <ul>
    ///   <li>If {@code type} has a non-null value (e.g. {@code BooleanLiteral.TRUE} / {@code BooleanLiteral.FALSE}),
    ///       the token's value must equal that specific value — this correctly distinguishes TRUE from FALSE.</li>
    ///   <li>If {@code type} has a null value (wildcard/placeholder, e.g. {@code new Literal.NumberLiteral()}),
    ///       the token only needs to be the same subclass of {@code Literal}.</li>
    /// </ul>
    ///
    /// @param type The token family to check against the current token.
    /// @return True if the current token matches the specified token family, false otherwise.
    public boolean check(TokenClass type) {

        if (isAtEnd()) return false;
        var currentType = peek().getType();

        if (type instanceof Literal litType && currentType instanceof Literal litCurrent) {

            var typeVal = litType.token();
            if (typeVal != null) return typeVal.equals(litCurrent.token());  // specific-value match (e.g. TRUE vs FALSE)
            return type.getClass() == currentType.getClass();              // wildcard class match
        }
        return currentType == type;
    }

    /// Checks if the current token matches any of the specified token families. If a match is found, advances the position.
    /// @param types The token families to check against the current token.
    /// @return True if the current token matches any of the specified token families, false otherwise.
    public boolean match(TokenClass... types) {

        for (var type : types) if (check(type)) {

            advance();
            return true;
        }
        return false;
    }

    /// Consumes the current token if it matches the specified token family; otherwise, records a
    /// {@link MissingTokenError} in the global {@link ErrorCollector} and attempts to recover by synchronizing to the
    /// next statement boundary.
    /// @param type The token family that the current token is expected to match.
    public Token consume(TokenClass type) {

        if (check(type)) return advance();
        var errorToken = peek();
        ErrorCollector.add(new MissingTokenError(errorToken.getLine(), errorToken.getColumn(), type));
        //throw new ParseException(message, errorToken);
        this.synchronize();  // Attempt to recover by synchronizing to the next statement/declaration
        return null;  // Return null to indicate that the expected token was not found, but allow parsing to continue
    }
    
    // ========== Helper Methods ==========

    /// Checks if the current token is a literal token.
    /// @param token The token to check.
    /// @return The literal value of the token if it is an instance of LiteralToken.
    public String getLiteralValue(LiteralToken token) { return token.getType().token(); }

    /// Returns the current token stream position.
    /// Used to save and restore the position for backtracking during ambiguous parses (e.g. for vs for-each).
    /// @return The current position index.
    public int getCurrentPosition() { return current; }

    /// Restores a previously saved token stream position for backtracking.
    /// @param position The position to restore to.
    public void setCurrentPosition(int position) { current = position; }

    /// Sync point for error recovery: advances the token stream until it finds a token that likely indicates the start
    /// of a new statement or declaration, allowing the parser to resume parsing after encountering an error.
    public void synchronize() {

        while (!isAtEnd()) {

            if (previous().getType() == Delimiter.SEMICOLON) return;  // likely end of previous statement
            if (previous().getType() == Delimiter.RBRACE) return;  // likely end of previous block

            var currentType = peek().getType();
            if (currentType instanceof AccessModifier) return;  // likely start of new declaration
            if (currentType instanceof Keyword) return;  // likely start of new statement or declaration (e.g. if, while, for, class, return)

            advance();  // Keep advancing until we find a sync point or reach the end of the token stream
        }
    }
}
