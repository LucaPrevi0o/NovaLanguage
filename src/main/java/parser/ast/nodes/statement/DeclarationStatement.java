package parser.ast.nodes.statement;

import parser.ast.nodes.NamedStatementNode;
import parser.ast.nodes.type.TypeSyntax;

/// Represents a declaration statement in the abstract syntax tree (AST), which can be a variable declaration, function declaration, or object declaration.
public abstract class DeclarationStatement extends NamedStatementNode {

    private final TypeSyntax typeSyntax;

    /// Constructs a new DeclarationStatement.
    /// @param line The line number where the declaration occurs.
    /// @param column The column number where the declaration starts.
    /// @param typeSyntax The parsed source type syntax of the declaration.
    /// @param name The name of the declaration.
    public DeclarationStatement(int line, int column, TypeSyntax typeSyntax, String name) {

        super(line, column, name);
        this.typeSyntax = typeSyntax;
    }

    /// Returns the parsed source type syntax for this declaration.
    /// @return The parsed type syntax.
    public TypeSyntax getDeclaredTypeSyntax() { return typeSyntax; }
}
