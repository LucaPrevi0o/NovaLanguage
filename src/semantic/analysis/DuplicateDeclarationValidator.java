package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import parser.ast.nodes.StatementNode;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Reports duplicate declarations inside each semantic scope.
public final class DuplicateDeclarationValidator {

    private final DiagnosticBag diagnostics = new DiagnosticBag();

    /// Validates a list of statements for duplicate declarations.
    /// @param statements The list of statements to validate.
    /// @return A list of diagnostics for any duplicate declarations found.
    public List<Diagnostic> validate(List<StatementNode> statements) {
        return validate(new SemanticScopeBuilder().build(statements));
    }

    /// Validates a semantic scope for duplicate declarations.
    /// @param rootScope The root semantic scope to validate.
    /// @return A list of diagnostics for any duplicate declarations found.
    public List<Diagnostic> validate(SemanticScope rootScope) {

        diagnostics.clear();
        validateScope(rootScope);
        return diagnostics.getDiagnostics();
    }

    /// Recursively validates a semantic scope and its children for duplicate declarations.
    /// @param scope The semantic scope to validate.
    private void validateScope(SemanticScope scope) {

        if (scope == null) return;

        var groups = groupByName(scope.getLocalDeclarations());
        for (var declarations : groups.values()) {

            if (declarations.size() < 2) continue;
            for (var i = 1; i < declarations.size(); i++) reportDuplicate(declarations.get(i));
        }

        for (var child : scope.getChildren()) validateScope(child);
    }

    /// Groups a list of semantic declarations by their names, ignoring constructors.
    /// @param declarations The list of semantic declarations to group.
    /// @return A map where the keys are declaration names and the values are lists of declarations with that name.
    private Map<String, List<SemanticDeclaration>> groupByName(List<SemanticDeclaration> declarations) {

        var groups = new LinkedHashMap<String, List<SemanticDeclaration>>();
        for (var declaration : declarations) {

            if (declaration.getKind() == DeclarationKind.CONSTRUCTOR) continue;
            groups.computeIfAbsent(declaration.getName(), ignored -> new ArrayList<>()).add(declaration);
        }
        return groups;
    }

    /// Reports a diagnostic for a duplicate declaration.
    /// @param declaration The duplicate semantic declaration to report.
    private void reportDuplicate(SemanticDeclaration declaration) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Duplicate declaration '" + declaration.getName() + "'",
            declaration.getLine(),
            declaration.getColumn()
        ));
    }
}
