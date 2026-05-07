package parser.parser.util;

import lexer.Token;
import lexer.token.LiteralToken;
import token.family.Literal;
import lexer.token.TypeToken;
import token.TokenFamily;
import token.family.Special;

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
    ///       the token only needs to be the same sub-class of {@code Literal}.</li>
    /// </ul>
    ///
    /// @param type The token family to check against the current token.
    /// @return True if the current token matches the specified token family, false otherwise.
    public boolean check(TokenFamily type) {

        if (isAtEnd()) return false;
        var currentType = peek().getType();

        if (type instanceof Literal litType && currentType instanceof Literal litCurrent) {
            var typeVal = litType.get();
            if (typeVal != null) return typeVal.equals(litCurrent.get());  // specific-value match (e.g. TRUE vs FALSE)
            return type.getClass() == currentType.getClass();              // wildcard class match
        }
        return currentType == type;
    }

    /// Checks if the current token matches any of the specified token families. If a match is found, advances the position.
    /// @param types The token families to check against the current token.
    /// @return True if the current token matches any of the specified token families, false otherwise.
    public boolean match(TokenFamily... types) {

        for (var type : types) if (check(type)) {

            advance();
            return true;
        }
        return false;
    }

    /// Consumes the current token if it matches the specified token family; otherwise, throws a ParseException with the provided message.
    /// @param type The token family to check against the current token.
    /// @param message The error message to include in the ParseException if the current token does not match the specified token family.
    /// @return The token that was consumed if it matches the specified token family.
    /// @throws ParseException If the current token does not match the specified token family.
    public Token consume(TokenFamily type, String message) {

        if (check(type)) return advance();
        throw new ParseException(message, peek());
    }
    
    // ========== Helper Methods ==========

    /// Checks if the current token is a literal token.
    /// @param token The token to check.
    /// @return True if the token is an instance of LiteralToken, false otherwise.
    public boolean isTypeToken(Token token) { return token instanceof TypeToken; }

    /// Checks if the current token is a literal token.
    /// @param token The token to check.
    /// @return The literal value of the token if it is an instance of LiteralToken.
    /// @throws ParseException If the token is not an instance of LiteralToken.
    public String getLiteralValue(Token token) {

        if (token instanceof LiteralToken lit) return lit.getValue();
        throw new ParseException("Expected literal token", token);
    }

    /// Returns the current token stream position.
    /// Used to save and restore the position for backtracking during ambiguous parses (e.g. for vs for-each).
    /// @return The current position index.
    public int getCurrentPosition() { return current; }

    /// Restores a previously saved token stream position for backtracking.
    /// @param position The position to restore to.
    public void setCurrentPosition(int position) { current = position; }
}
