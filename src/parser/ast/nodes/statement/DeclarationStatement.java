package parser.ast.nodes.statement;

import parser.ast.nodes.Symbol;
import lexer.token.ReturnType;

/// Represents a declaration statement in the abstract syntax tree (AST), which can be a variable declaration, function declaration, or object declaration.
public abstract class DeclarationStatement extends Symbol {

    private final ReturnType type;

    /// Constructs a new DeclarationStatement.
    /// @param line The line number where the declaration occurs.
    /// @param column The column number where the declaration starts.
    /// @param type The return type of the declaration.
    /// @param name The name of the declaration.
    public DeclarationStatement(int line, int column, ReturnType type, String name) {
        
        super(line, column, name);
        this.type = type;
    }

    /// Returns the return type of the declaration.
    /// @return The return type of the declaration.
    public ReturnType getDeclaredType() { return type; }
}
