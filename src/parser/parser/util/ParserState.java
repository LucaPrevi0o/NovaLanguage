package parser.parser.util;

import error.ErrorCollector;
import error.syntax.SyntaxError;
import lexer.Token;
import lexer.token.TokenClass;
import lexer.token.type.LiteralToken;
import lexer.token.family.Literal;
import lexer.token.family.Special;
import error.Error;

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
    public Token getCurrentToken() { return tokens.get(current); }

    /// Returns the getPreviousToken token that was consumed.
    /// @return The getPreviousToken token.
    public Token getPreviousToken() { return tokens.get(current - 1); }

    /// Checks if the parser has reached the end of the token list.
    /// @return True if the current token is the end-of-file token, false otherwise.
    public boolean isAtEnd() { return getCurrentToken().getType() == Special.EOF; }

    /// Advances the current position and returns the token that was just consumed.
    /// @return The token that was consumed by advancing the position.
    public Token getNextToken() {

        if (!isAtEnd()) current++;
        return getPreviousToken();
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
    /// @param type The token family to checkCurrentTokenType against the current token.
    /// @return True if the current token matches the specified token family, false otherwise.
    public boolean checkCurrentTokenType(TokenClass type) {

        if (isAtEnd()) return false;
        var currentType = getCurrentToken().getType();

        if (type instanceof Literal litType && currentType instanceof Literal litCurrent) {

            var typeVal = litType.token();
            if (typeVal != null) return typeVal.equals(litCurrent.token());  // specific-value checkCurrentTokenType (e.g. TRUE vs FALSE)
            return type.getClass() == currentType.getClass();              // wildcard class checkCurrentTokenType
        }
        return currentType == type;
    }

    /// Checks if the current token matches any of the specified token families.
    /// @param types The token families to checkCurrentTokenType against the current token.
    /// @return True if the current token matches any of the specified token families, false otherwise.
    public boolean checkCurrentTokenType(TokenClass... types) {

        for (var type : types)
            if (checkCurrentTokenType(type)) return true;
        return false;
    }

    /// Consumes the current token if it matches the specified token family, advancing the position.
    /// If the current token does not match, records a syntax error and returns null.
    /// @param expectedType The token family to checkCurrentTokenType against the current token.
    /// @param errorType The type of syntax error to record if the current token does not match the specified token family.
    /// @return The token that was consumed if it matches the specified token family, or null if it does not match (with a syntax error recorded).
    public Token getNextToken(TokenClass expectedType, @SyntaxError Error errorType) {

        if (checkCurrentTokenType(expectedType)) return getNextToken();
        ErrorCollector.add(errorType);
        return null;
    }
    
    // ========== Helper Methods ==========

    /// Checks if the current token is a literal token.
    /// @param token The token to checkCurrentTokenType.
    /// @return The literal value of the token if it is an instance of LiteralToken.
    /// @throws ParseException If the token is not an instance of LiteralToken.
    public String getLiteralValue(LiteralToken token) {
        return token.getType().token();
    }

    /// Returns the current token stream position.
    /// Used to save and restore the position for backtracking during ambiguous parses (e.g. for vs for-each).
    /// @return The current position index.
    public int getCurrentPosition() { return current; }

    /// Restores a previously saved token stream position for backtracking.
    /// @param position The position to restore to.
    public void setCurrentPosition(int position) { current = position; }
}
