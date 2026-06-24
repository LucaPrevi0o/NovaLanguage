package semantic.declaration;

import lexer.token.ReturnType;
import parser.ast.AstNode;

/// A declaration discovered from the AST before name resolution or duplicate validation.
public final class SemanticDeclaration {

    private final DeclarationKind kind;
    private final String name;
    private final ReturnType declaredType;
    private final AstNode node;
    private final AstNode owner;

    /// Creates a new semantic declaration with the given properties.
    /// @param kind The kind of declaration (e.g., class, method, variable).
    /// @param name The name of the declaration.
    /// @param declaredType The return type of the declaration, or null if not applicable.
    /// @param node The AST node that represents the declaration.
    /// @param owner The AST node that owns the declaration, or `null` if it has no owner.
    public SemanticDeclaration(DeclarationKind kind, String name, ReturnType declaredType, AstNode node, AstNode owner) {

        this.kind = kind;
        this.name = name;
        this.declaredType = declaredType;
        this.node = node;
        this.owner = owner;
    }

    /// Returns the kind of declaration (e.g., class, method, variable).
    /// @return The kind of declaration.
    public DeclarationKind getKind() { return kind; }

    /// Returns the name of the declaration.
    /// @return The name of the declaration.
    public String getName() { return name; }

    /// Returns the return type of the declaration, or `null` if not applicable.
    /// @return The return type of the declaration, or `null` if not applicable.
    public ReturnType getDeclaredType() { return declaredType; }

    /// Returns the AST node that represents the declaration.
    /// @return The AST node that represents the declaration.
    public AstNode getNode() { return node; }

    /// Returns the AST node that owns the declaration, or `null` if it has no owner.
    /// @return The AST node that owns the declaration, or `null` if it has no owner.
    public AstNode getOwner() { return owner; }

    /// Returns the line number of the declaration in the source code, or `-1` if not available.
    /// @return The line number of the declaration in the source code, or `-1` if not available.
    public int getLine() { return node != null ? node.getLine() : -1; }

    /// Returns the column number of the declaration in the source code, or `-1` if not available.
    /// @return The column number of the declaration in the source code, or `-1` if not available.
    public int getColumn() { return node != null ? node.getColumn() : -1; }
}
