package parser.ast;

import java.util.ArrayList;
import java.util.List;
import parser.ast.nodes.Symbol;

/// Represents a symbol table for managing variable and function scopes in the abstract syntax tree (AST).
///
/// A symbol table is a hierarchical structure that allows for the registration and lookup of symbols
/// (such as variables and functions) within different scopes.
/// Each symbol table can have a parent symbol table, which represents the enclosing scope, and a list of child symbol
/// tables, which represent nested scopes.
public record SymbolTable(SymbolTable parent, List<Symbol> symbols, List<SymbolTable> children) {

    /// Constructs a new SymbolTable with the specified parent, symbols, and children.
    /// @param parent The parent symbol table representing the enclosing scope, or null if this is the global scope.
    private SymbolTable(SymbolTable parent) { this(parent, new ArrayList<>(), new ArrayList<>()); }

    /// Creates a new child symbol table that is a child of the current symbol table.
    /// @return A new SymbolTable instance that is a child of the current symbol table.
    public SymbolTable createChildScope() {

        var child = new SymbolTable(this);
        this.children.add(child);
        return child;
    }

    /// Registers a new symbol in the current symbol table.
    /// @param symbol The symbol to register in the current symbol table.
    public void register(Symbol symbol) {

        for (var sym : symbols) if (sym.getName().equals(symbol.getName()))
            throw new RuntimeException("Symbol '" + symbol.getName() + "' is already defined in this scope.");

        symbols.add(symbol);
    }

    /// Looks up a symbol by name in the current symbol table and its parent symbol tables.
    /// @param name The name of the symbol to look up.
    /// @return The Symbol instance with the specified name if found, or null if not found in the current symbol table
    /// or any of its parent symbol tables.
    public Symbol lookup(String name) {

        for (var symbol : symbols)
            if (symbol.getName().equals(name)) return symbol;

        if (parent != null) return parent.lookup(name);
        return null;
    }
}
