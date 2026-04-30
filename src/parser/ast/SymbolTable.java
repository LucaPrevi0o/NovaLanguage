package src.parser.ast;

import java.util.ArrayList;
import java.util.List;
import src.parser.ast.nodes.Symbol;

public record SymbolTable(SymbolTable parent, List<Symbol> symbols, List<SymbolTable> children) {

    private SymbolTable(SymbolTable parent) { this(parent, new ArrayList<>(), new ArrayList<>()); }

    /**
     * Create a new child scope for this symbol table.
     *
     * @return A new SymbolTable with this as parent.
     */
    public SymbolTable createChildScope() {

        var child = new SymbolTable(this);
        this.children.add(child);
        return child;
    }

    /**
     * Register a new symbol in the current symbol table.
     *
     * @param symbol The symbol to register.
     */
    public void register(Symbol symbol) {

        for (var sym : symbols) if (sym.getName().equals(symbol.getName()))
            throw new RuntimeException("Symbol '" + symbol.getName() + "' is already defined in this scope.");

        symbols.add(symbol);
    }

    /**
     * Lookup a symbol by name in the current symbol table and its parents.
     *
     * @param name The name of the symbol to lookup.
     * @return The found symbol, or null if not found.
     */
    public Symbol lookup(String name) {

        for (var symbol : symbols)
            if (symbol.getName().equals(name)) return symbol;

        if (parent != null) return parent.lookup(name);
        return null;
    }
}
