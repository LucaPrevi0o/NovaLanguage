package src.parser.ast;

import java.util.List;
import src.parser.ast.nodes.Symbol;

public class SymbolTable {
    
    private SymbolTable parent;
    private List<Symbol> symbols;
    private List<SymbolTable> children;

    public SymbolTable(SymbolTable parent, List<Symbol> symbols, List<SymbolTable> children) {

        this.parent = parent;
        this.symbols = symbols;
        this.children = children;
    }

    public SymbolTable getParent() { return parent; }
    public List<Symbol> getSymbols() { return symbols; }
    public List<SymbolTable> getChildren() { return children; }
    
    /**
     * Create a new child scope for this symbol table.
     * @return A new SymbolTable with this as parent.
     */
    public SymbolTable createChildScope() {
        SymbolTable child = new SymbolTable(this, new java.util.ArrayList<>(), new java.util.ArrayList<>());
        this.children.add(child);
        return child;
    }

    /**
     * Register a new symbol in the current symbol table.
     * @param symbol The symbol to register.
     */
    public void register(Symbol symbol) { 
        
        // Check for duplicate only in the CURRENT scope (not parent)
        for (var sym : symbols) {
            if (sym.getName().equals(symbol.getName())) 
                throw new RuntimeException("Symbol '" + symbol.getName() + "' is already defined in this scope.");
        }
        
        // Allow shadowing - variables can hide parent scope variables
        symbols.add(symbol);
    }

    /**
     * Lookup a symbol by name in the current symbol table and its parents.
     * @param name The name of the symbol to lookup.
     * @return The found symbol, or null if not found.
     */
    public Symbol lookup(String name) { 

        for (var symbol : symbols) {
            if (symbol.getName().equals(name)) {
                return symbol;
            }
        }
        
        if (parent != null) {
            return parent.lookup(name);
        }

        return null;
    }
}
