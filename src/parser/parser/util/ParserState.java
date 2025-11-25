package src.parser.parser.util;

import src.lexer.Token;
import src.lexer.token.LiteralToken;
import src.token.family.Literal;
import src.lexer.token.TypeToken;
import src.token.TokenFamily;
import src.token.family.Special;

import java.util.List;

/**
 * Shared state for all parsers.
 * Maintains the current position in the token stream and provides navigation methods.
 * All parser instances share this single state, eliminating the need for position synchronization.
 */
public class ParserState {
    
    private final List<Token> tokens;
    private int current = 0;

    public ParserState(List<Token> tokens) { this.tokens = tokens; }

    // ========== Token Navigation ==========
    
    public Token peek() { return tokens.get(current); }

    public Token previous() { return tokens.get(current - 1); }

    public boolean isAtEnd() { return peek().getType() == Special.EOF; }

    public Token advance() {

        if (!isAtEnd()) current++;
        return previous();
    }
    
    // ========== Token Checking ==========
    
    public boolean check(TokenFamily type) {

        if (isAtEnd()) return false;
        TokenFamily currentType = peek().getType();
        
        // For Literal subclasses, compare by class type instead of instance
        if (type instanceof Literal && currentType instanceof Literal) return type.getClass() == currentType.getClass();
        return currentType == type;
    }
    
    public boolean match(TokenFamily... types) {

        for (TokenFamily type : types) if (check(type)) {

            advance();
            return true;
        }
        return false;
    }
    
    public Token consume(TokenFamily type, String message) {

        if (check(type)) return advance();
        throw new ParseException(message, peek());
    }
    
    // ========== Helper Methods ==========
    
    public boolean isTypeToken(Token token) { return token instanceof TypeToken; }
    
    public String getLiteralValue(Token token) {

        if (token instanceof LiteralToken lit) return lit.getValue();
        throw new ParseException("Expected literal token", token);
    }
    
    // ========== Position Access (for error recovery) ==========
    
    public int getCurrentPosition() { return current; }
    public void setCurrentPosition(int position) { this.current = position; }
}
