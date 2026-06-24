package semantic.scope;

import parser.ast.AstNode;
import semantic.declaration.SemanticDeclaration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Lexical semantic scope built from the parsed AST.
public final class SemanticScope {

    private final String name;
    private final AstNode owner;
    private final SemanticScope parent;
    private final List<SemanticScope> children = new ArrayList<>();
    private final List<SemanticDeclaration> declarations = new ArrayList<>();
    private final Map<String, List<SemanticDeclaration>> declarationsByName = new LinkedHashMap<>();

    /// Creates a new semantic scope with the given name, owner, and parent scope.
    /// @param name The name of the scope.
    /// @param owner The AST node that owns the scope, or `null` if it has no owner.
    /// @param parent The parent scope, or `null` if it has no parent.
    public SemanticScope(String name, AstNode owner, SemanticScope parent) {

        this.name = name;
        this.owner = owner;
        this.parent = parent;
    }

    /// Creates a new child scope with the given name and owner, and adds it to the list of children.
    /// @param name The name of the child scope.
    /// @param owner The AST node that owns the child scope, or `null` if it has no owner.
    /// @return The newly created child scope.
    public SemanticScope createChild(String name, AstNode owner) {

        var child = new SemanticScope(name, owner, this);
        children.add(child);
        return child;
    }

    /// Declares a new semantic declaration in the current scope.
    /// @param declaration The semantic declaration to declare in the current scope.
    public void declare(SemanticDeclaration declaration) {

        if (declaration == null) return;
        declarations.add(declaration);
        declarationsByName.computeIfAbsent(declaration.getName(), ignored -> new ArrayList<>()).add(declaration);
    }

    /// Returns the name of the scope.
    /// @return The name of the scope.
    public String getName() { return name; }

    /// Returns the AST node that owns the scope, or `null` if it has no owner.
    /// @return The AST node that owns the scope, or `null` if it has no owner.
    public AstNode getOwner() { return owner; }

    /// Returns the parent scope, or `null` if it has no parent.
    /// @return The parent scope, or `null` if it has no parent.
    public SemanticScope getParent() { return parent; }

    /// Returns the list of child scopes.
    /// @return The list of child scopes.
    public List<SemanticScope> getChildren() { return List.copyOf(children); }

    /// Returns the list of semantic declarations declared in the current scope.
    /// @return The list of semantic declarations declared in the current scope.
    public List<SemanticDeclaration> getLocalDeclarations() { return List.copyOf(declarations); }

    /// Returns the list of semantic declarations declared in the current scope with the given name.
    /// @param name The name of the declarations to return.
    /// @return The list of semantic declarations declared in the current scope with the given name.
    public List<SemanticDeclaration> findLocal(String name) {

        var matches = declarationsByName.get(name);
        return matches != null ? List.copyOf(matches) : List.of();
    }

    /// Returns the child scope owned by the given AST node, if any.
    /// @param owner The AST node that owns the child scope.
    /// @return An optional containing the child scope owned by the given AST node, or empty if no such child scope exists.
    public Optional<SemanticScope> childOwnedBy(AstNode owner) {

        for (var child : children)
            if (child.getOwner() == owner) return Optional.of(child);
        return Optional.empty();
    }
}
