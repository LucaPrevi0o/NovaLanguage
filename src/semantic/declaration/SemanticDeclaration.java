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

    public SemanticDeclaration(DeclarationKind kind, String name, ReturnType declaredType, AstNode node, AstNode owner) {

        this.kind = kind;
        this.name = name;
        this.declaredType = declaredType;
        this.node = node;
        this.owner = owner;
    }

    public DeclarationKind getKind() { return kind; }

    public String getName() { return name; }

    public ReturnType getDeclaredType() { return declaredType; }

    public AstNode getNode() { return node; }

    public AstNode getOwner() { return owner; }

    public int getLine() { return node != null ? node.getLine() : -1; }

    public int getColumn() { return node != null ? node.getColumn() : -1; }
}
