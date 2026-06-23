package semantic;

import parser.ast.AstNode;

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

    public SemanticScope(String name, AstNode owner, SemanticScope parent) {

        this.name = name;
        this.owner = owner;
        this.parent = parent;
    }

    public SemanticScope createChild(String name, AstNode owner) {

        var child = new SemanticScope(name, owner, this);
        children.add(child);
        return child;
    }

    public void declare(SemanticDeclaration declaration) {

        if (declaration == null) return;
        declarations.add(declaration);
        declarationsByName.computeIfAbsent(declaration.getName(), ignored -> new ArrayList<>()).add(declaration);
    }

    public String getName() { return name; }

    public AstNode getOwner() { return owner; }

    public SemanticScope getParent() { return parent; }

    public List<SemanticScope> getChildren() { return List.copyOf(children); }

    public List<SemanticDeclaration> getLocalDeclarations() { return List.copyOf(declarations); }

    public List<SemanticDeclaration> findLocal(String name) {

        var matches = declarationsByName.get(name);
        return matches != null ? List.copyOf(matches) : List.of();
    }

    public Optional<SemanticScope> childOwnedBy(AstNode owner) {

        for (var child : children)
            if (child.getOwner() == owner) return Optional.of(child);
        return Optional.empty();
    }
}
