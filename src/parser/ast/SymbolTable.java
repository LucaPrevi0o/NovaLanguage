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

    public void register(Symbol symbol) { 
        
        for (var sym : symbols) {

            if (sym.getName().equals(symbol.getName())) throw new RuntimeException("Symbol '" + symbol.getName() + "' is already defined in this scope.");
            if (parent != null && parent.lookup(symbol.getName()) != null)
                throw new RuntimeException("Symbol '" + symbol.getName() + "' is already defined in an outer scope.");
        }
        symbols.add(symbol);
    }

    public Symbol lookup(String name) { 

        for (var symbol : symbols)
            if (symbol.getName().equals(name)) return symbol;
        if (parent != null) return parent.lookup(name);
        return null;
    }
}
