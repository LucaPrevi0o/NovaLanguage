package parser.ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import parser.ast.nodes.Symbol;
import parser.parser.util.ParseException;

/// Represents a symbol table for managing variable and function scopes in the abstract syntax tree (AST).
///
/// A symbol table is a hierarchical structure that allows for the registration and lookup of symbols
/// (such as variables and functions) within different scopes.
/// Each symbol table can have a parent symbol table, which represents the enclosing scope, and a list of child symbol
/// tables, which represent nested scopes.
public record SymbolTable(SymbolTable parent, List<Symbol> symbols, List<SymbolTable> children, Map<Symbol, SymbolTable> ownedScopes) {

    /// Constructs a new SymbolTable with the specified parent, symbols, and children.
    /// @param parent The parent symbol table representing the enclosing scope, or null if this is the global scope.
    public SymbolTable(SymbolTable parent) { this(parent, new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>()); }

    /// Creates a new child symbol table that is a child of the current symbol table.
    /// @return A new SymbolTable instance that is a child of the current symbol table.
    public SymbolTable createChildScope() {

        var child = new SymbolTable(this);
        this.children.add(child);
        return child;
    }

    /// Creates a new child symbol table that is a child of the current symbol table and associates it with the given owner symbol.
    /// @param owner The symbol that owns the new child scope (e.g., a function or class declaration).
    /// @return A new SymbolTable instance that is a child of the current symbol table and is associated with the given owner symbol.
    public SymbolTable createChildScope(Symbol owner) {

        var child = new SymbolTable(this);
        this.children.add(child);
        this.ownedScopes.put(owner, child);
        return child;
    }

    /// Registers a new symbol in the current symbol table.
    /// Throws a {@link ParseException} with source location if the name is already declared in this scope.
    /// @param symbol The symbol to register in the current symbol table.
    public void register(Symbol symbol) {

        for (var sym : symbols) if (sym.getName().equals(symbol.getName()))
            throw new ParseException(
                "Symbol '" + symbol.getName() + "' is already defined in this scope.",
                symbol.getLine(),
                symbol.getColumn()
            );

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

    /// Retrieves the child symbol table that is owned by the specified symbol, if it exists.
    /// @param symbol The symbol for which to retrieve the owned child symbol table.
    /// @return The child SymbolTable instance that is owned by the specified symbol, or {@code null} if no such child symbol table exists for the given symbol.
    public SymbolTable getOwnedScope(Symbol symbol) {
        return ownedScopes.get(symbol);
    }
}
