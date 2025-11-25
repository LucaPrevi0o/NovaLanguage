package src.parser.parser.util;

import src.lexer.Token;
import src.parser.ast.SymbolTable;
import src.token.TokenFamily;

import java.util.ArrayList;

/**
 * Base class for parsers that use shared ParserState.
 * Provides convenient access to state methods.
 */
public abstract class ParserBase {
    
    protected final ParserState state;
    protected SymbolTable symbolTable;

    protected ParserBase(ParserState state) { 
        
        this.state = state;
        this.symbolTable = new SymbolTable(null, new ArrayList<>(), new ArrayList<>());
    }

    public ParserState getState() { return state; }
    public SymbolTable getSymbolTable() { return symbolTable; }

    // ========== Convenience Delegates to State ==========
    
    protected Token peek() { return state.peek(); }
    protected Token previous() { return state.previous(); }
    protected boolean isAtEnd() { return state.isAtEnd(); }
    protected Token advance() { return state.advance(); }
    protected boolean check(TokenFamily type) { return state.check(type); }
    protected boolean match(TokenFamily... types) { return state.match(types); }
    protected Token consume(TokenFamily type, String message) { return state.consume(type, message); }
    protected boolean isTypeToken(Token token) { return state.isTypeToken(token); }
    protected String getLiteralValue(Token token) { return state.getLiteralValue(token); }
}
