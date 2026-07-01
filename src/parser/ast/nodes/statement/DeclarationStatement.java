package parser.ast.nodes.statement;

import parser.ast.nodes.NamedStatementNode;
import lexer.token.ReturnType;
import parser.ast.nodes.type.TypeSyntax;
import parser.support.TypeSyntaxAdapter;

/// Represents a declaration statement in the abstract syntax tree (AST), which can be a variable declaration, function declaration, or object declaration.
public abstract class DeclarationStatement extends NamedStatementNode {

    private final ReturnType compatibilityType;
    private final TypeSyntax typeSyntax;

    /// Constructs a new DeclarationStatement.
    /// @param line The line number where the declaration occurs.
    /// @param column The column number where the declaration starts.
    /// @param typeSyntax The parsed source type syntax of the declaration.
    /// @param name The name of the declaration.
    public DeclarationStatement(int line, int column, TypeSyntax typeSyntax, String name) {

        super(line, column, name);
        this.typeSyntax = typeSyntax;
        this.compatibilityType = null;
    }

    /// Constructs a compatibility DeclarationStatement from a legacy ReturnType adapter.
    /// @param line The line number where the declaration occurs.
    /// @param column The column number where the declaration starts.
    /// @param type The temporary ReturnType adapter for older callers.
    /// @param name The name of the declaration.
    public DeclarationStatement(int line, int column, ReturnType type, String name) {

        super(line, column, name);
        this.compatibilityType = type;
        this.typeSyntax = type != null ? type.getSyntax() : null;
    }

    /// Returns the temporary ReturnType adapter for compatibility callers.
    /// @return The compatibility return type of the declaration.
    public ReturnType getDeclaredType() {
        return compatibilityType != null ? compatibilityType : TypeSyntaxAdapter.toReturnType(typeSyntax);
    }

    /// Returns the parsed source type syntax for this declaration, when available.
    /// @return The parsed type syntax, or {@code null} for compatibility-only declarations.
    public TypeSyntax getDeclaredTypeSyntax() { return typeSyntax; }
}
