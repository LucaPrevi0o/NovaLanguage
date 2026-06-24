package semantic.declaration;

import java.util.ArrayList;
import java.util.List;

/// Immutable result of collecting declarations from a parsed AST.
public final class DeclarationCollection {

    private final List<SemanticDeclaration> declarations;

    public DeclarationCollection(List<SemanticDeclaration> declarations) {
        this.declarations = List.copyOf(declarations);
    }

    public List<SemanticDeclaration> all() { return declarations; }

    public List<SemanticDeclaration> byName(String name) {

        var matches = new ArrayList<SemanticDeclaration>();
        for (var declaration : declarations)
            if (declaration.getName().equals(name)) matches.add(declaration);
        return List.copyOf(matches);
    }

    public List<SemanticDeclaration> byKind(DeclarationKind kind) {

        var matches = new ArrayList<SemanticDeclaration>();
        for (var declaration : declarations)
            if (declaration.getKind() == kind) matches.add(declaration);
        return List.copyOf(matches);
    }
}
