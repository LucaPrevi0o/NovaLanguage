package parser.parser.util;

import lexer.Token;
import lexer.token.type.LiteralToken;
import parser.ast.SymbolTable;
import lexer.token.TokenClass;
import lexer.token.TypeRegistry;
import lexer.token.family.AccessModifier;
import parser.ast.nodes.Symbol;

/// Base class for all parsers, providing common state and symbol table management.
public abstract class ParserBase {
    
    protected final ParserState state;
    protected SymbolTable symbolTable;  // Current scope
    protected final TypeRegistry typeRegistry;

    /// Constructor for root parser (starts with global scope).
    /// @param state        The parser state to use for token management.
    /// @param typeRegistry The per-session type registry to use.
    protected ParserBase(ParserState state, TypeRegistry typeRegistry) { 

        this.state = state;
        this.typeRegistry = typeRegistry;
        this.symbolTable = new SymbolTable(null);  // Start with global scope (no parent)
    }

    /// Constructor for child parsers (inherits current scope).
    /// @param state        The parser state to use for token management.
    /// @param symbolTable  The symbol table representing the current scope to inherit.
    /// @param typeRegistry The per-session type registry to use.
    protected ParserBase(ParserState state, SymbolTable symbolTable, TypeRegistry typeRegistry) { 

        this.state = state;
        this.symbolTable = symbolTable;
        this.typeRegistry = typeRegistry;
    }

    /// Returns the current parser state.
    /// @return The current parser state.
    public ParserState getState() { return state; }

    /// Returns the current symbol table, representing the current scope.
    /// @return The current symbol table.
    public SymbolTable getSymbolTable() { return symbolTable; }

    /// Returns the type registry for this parse session.
    /// @return The type registry.
    public TypeRegistry getTypeRegistry() { return typeRegistry; }

    /// Enters a new scope by creating a child symbol table of the current scope.
    /// @return The new child symbol table representing the new scope.
    protected SymbolTable enterScope() { return symbolTable.createChildScope(); }

    /// Enters a new scope owned by the specified symbol (e.g., a function or class declaration) by creating a child symbol
    /// table of the current scope and associating it with the owner symbol.
    /// @param owner The symbol that owns the new scope (e.g., a function or class declaration).
    /// @return The new child symbol table representing the new scope owned by the specified symbol.
    protected SymbolTable enterScope(Symbol owner) { return symbolTable.createChildScope(owner); }

    /// Exits the current scope by returning the parent symbol table of the current scope.
    /// @param scope The current symbol table representing the current scope.
    /// @return The parent symbol table representing the enclosing scope, or the current symbol table if already at global scope.
    protected SymbolTable exitScope(SymbolTable scope) {

        var parent = scope.parent();
        if (parent != null) return parent;
        return scope;  // Already at global scope
    }

    // ========== Convenience Delegates to State ==========

    /// Returns the current token without consuming it.
    /// @return The current token.
    protected Token peek() { return state.peek(); }

    /// Returns the previous token that was consumed.
    /// @return The previous token.
    protected Token previous() { return state.previous(); }

    /// Checks if the parser has not reached the end of the token stream.
    /// @return True if the parser has not reached the end of the token stream; otherwise, false.
    protected boolean isNotAtEnd() { return !state.isAtEnd(); }

    /// Advances the parser to the next token and returns the consumed token.
    /// @return The token that was consumed by advancing the parser.
    protected Token advance() { return state.advance(); }

    /// Checks if the current token matches the specified type.
    /// @param type The token family to check against the current token.
    /// @return True if the current token matches the specified type; otherwise, false.
    protected boolean check(TokenClass type) { return state.check(type); }

    /// Checks if the current token matches any of the specified types.
    /// @param types The token families to check against the current token.
    /// @return True if the current token matches any of the specified types; otherwise, false.
    protected boolean match(TokenClass... types) { return state.match(types); }

    /// Consumes the current token if it matches the specified type; otherwise, throws a ParseException with the provided error message.
    /// @param type The token family to check against the current token.
    /// @param message The error message to include in the ParseException if the current token does not match the specified type.
    /// @return The token that was consumed if it matches the specified type.
    protected Token consume(TokenClass type, String message) { return state.consume(type, message); }

    /// Retrieves the literal value associated with the specified token, if applicable.
     /// @param token The token for which to retrieve the literal value.
     /// @return The literal value associated with the specified token, or null if the token does not have a literal value.
     protected String getLiteralValue(LiteralToken token) { return state.getLiteralValue(token); }

     /// Parses an access modifier, which can be "public", "private", or "protected".
     /// If no access modifier is present, returns null (indicating package-private).
     ///
     /// Grammar rule:
     /// `accessModifier → "public" | "private" | "protected"`
     /// @return An AccessModifier enum value representing the parsed access modifier, or null if no access modifier is present (indicating package-private).
     protected AccessModifier parseAccessModifier() {

         for (var modifier : AccessModifier.values()) if (match(modifier)) return modifier;
         return null;  // No access modifier (package-private)
     }
}
