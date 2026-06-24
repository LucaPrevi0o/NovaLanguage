package semantic.declaration;

import java.util.ArrayList;
import java.util.List;

/// Immutable result of collecting declarations from a parsed AST.
public final class DeclarationCollection {

    private final List<SemanticDeclaration> declarations;

    /// Creates a new declaration collection from the given list of declarations.
    /// @param declarations The list of declarations to include in the collection.
    public DeclarationCollection(List<SemanticDeclaration> declarations) {
        this.declarations = List.copyOf(declarations);
    }

    /// Returns all declarations in the collection.
    /// @return A list of all declarations in the collection.
    public List<SemanticDeclaration> all() { return declarations; }

    /// Returns all declarations in the collection with the given name.
    /// @param name The name of the declarations to return.
    /// @return A list of all declarations in the collection with the given name.
    public List<SemanticDeclaration> byName(String name) {

        var matches = new ArrayList<SemanticDeclaration>();
        for (var declaration : declarations)
            if (declaration.getName().equals(name)) matches.add(declaration);
        return List.copyOf(matches);
    }

    /// Returns all declarations in the collection with the given kind.
    /// @param kind The kind of the declarations to return.
    /// @return A list of all declarations in the collection with the given kind.
    public List<SemanticDeclaration> byKind(DeclarationKind kind) {

        var matches = new ArrayList<SemanticDeclaration>();
        for (var declaration : declarations)
            if (declaration.getKind() == kind) matches.add(declaration);
        return List.copyOf(matches);
    }
}
