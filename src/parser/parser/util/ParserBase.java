package src.parser.parser.util;

import src.lexer.Token;
import src.parser.ast.SymbolTable;
import src.token.TokenFamily;

import java.util.ArrayList;

/**
 * Base class for parsers that use shared ParserState and SymbolTable.
 * Provides convenient access to state methods and scope management.
 */
public abstract class ParserBase {
    
    protected final ParserState state;
    protected SymbolTable symbolTable;  // Current scope

    /**
     * Constructor for root parser (creates global scope).
     */
    protected ParserBase(ParserState state) { 
        this.state = state;
        this.symbolTable = new SymbolTable(null, new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * Constructor for child parsers (shares symbol table).
     */
    protected ParserBase(ParserState state, SymbolTable symbolTable) { 
        this.state = state;
        this.symbolTable = symbolTable;
    }

    public ParserState getState() { return state; }
    public SymbolTable getSymbolTable() { return symbolTable; }
    
    /**
     * Enter a new scope by creating a child symbol table.
     * @return The new child scope.
     */
    protected SymbolTable enterScope() {
        SymbolTable newScope = symbolTable.createChildScope();
        return newScope;
    }
    
    /**
     * Exit the current scope by returning to parent.
     * @param scope The scope to exit from.
     * @return The parent scope.
     */
    protected SymbolTable exitScope(SymbolTable scope) {
        SymbolTable parent = scope.getParent();
        if (parent != null) {
            return parent;
        }
        return scope;  // Already at global scope
    }

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
