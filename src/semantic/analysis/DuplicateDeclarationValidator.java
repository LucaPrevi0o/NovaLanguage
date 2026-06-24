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

    public List<Diagnostic> validate(List<StatementNode> statements) {
        return validate(new SemanticScopeBuilder().build(statements));
    }

    public List<Diagnostic> validate(SemanticScope rootScope) {

        diagnostics.clear();
        validateScope(rootScope);
        return diagnostics.getDiagnostics();
    }

    private void validateScope(SemanticScope scope) {

        if (scope == null) return;

        var groups = groupByName(scope.getLocalDeclarations());
        for (var declarations : groups.values()) {

            if (declarations.size() < 2) continue;
            for (var i = 1; i < declarations.size(); i++) reportDuplicate(declarations.get(i));
        }

        for (var child : scope.getChildren()) validateScope(child);
    }

    private Map<String, List<SemanticDeclaration>> groupByName(List<SemanticDeclaration> declarations) {

        var groups = new LinkedHashMap<String, List<SemanticDeclaration>>();
        for (var declaration : declarations) {

            if (declaration.getKind() == DeclarationKind.CONSTRUCTOR) continue;
            groups.computeIfAbsent(declaration.getName(), ignored -> new ArrayList<>()).add(declaration);
        }
        return groups;
    }

    private void reportDuplicate(SemanticDeclaration declaration) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Duplicate declaration '" + declaration.getName() + "'",
            declaration.getLine(),
            declaration.getColumn()
        ));
    }
}
