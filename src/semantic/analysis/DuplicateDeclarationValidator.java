package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static printer.AstPrinter.buildTypeStringWithSizes;

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
            validateNameGroup(declarations);
        }

        for (var child : scope.getChildren()) validateScope(child);
    }

    /// Groups a list of semantic declarations by their names.
    /// @param declarations The list of semantic declarations to group.
    /// @return A map where the keys are declaration names and the values are lists of declarations with that name.
    private Map<String, List<SemanticDeclaration>> groupByName(List<SemanticDeclaration> declarations) {

        var groups = new LinkedHashMap<String, List<SemanticDeclaration>>();
        for (var declaration : declarations)
            groups.computeIfAbsent(declaration.name(), ignored -> new ArrayList<>()).add(declaration);
        return groups;
    }

    /// Validates all declarations that share the same name in one semantic scope.
    /// @param declarations The same-name declarations to validate.
    private void validateNameGroup(List<SemanticDeclaration> declarations) {

        if (allOverloadable(declarations)) validateOverloadSignatures(declarations);
        else for (var i = 1; i < declarations.size(); i++) reportDuplicate(declarations.get(i));
    }

    /// Checks whether every declaration in a same-name group can participate in overloads.
    /// @param declarations The same-name declarations to inspect.
    /// @return `true` if every declaration is overloadable, `false` otherwise.
    private boolean allOverloadable(List<SemanticDeclaration> declarations) {

        for (var declaration : declarations)
            if (!isOverloadable(declaration)) return false;
        return true;
    }

    /// Checks whether a declaration kind can be overloaded by signature.
    /// @param declaration The declaration to inspect.
    /// @return `true` for functions, methods, and constructors.
    private boolean isOverloadable(SemanticDeclaration declaration) {
        return declaration.kind() == DeclarationKind.FUNCTION ||
                declaration.kind() == DeclarationKind.METHOD ||
                declaration.kind() == DeclarationKind.CONSTRUCTOR;
    }

    /// Validates function, method, and constructor declarations that share a name by parameter signature.
    /// @param declarations The overloadable declarations to validate.
    private void validateOverloadSignatures(List<SemanticDeclaration> declarations) {

        var seen = new LinkedHashMap<String, SemanticDeclaration>();
        for (var declaration : declarations) {

            var signature = overloadSignature(declaration);
            if (seen.containsKey(signature)) reportDuplicate(declaration);
            else seen.put(signature, declaration);
        }
    }

    /// Builds an overload signature from declaration name and parameter types.
    /// @param declaration The overloadable declaration.
    /// @return A signature string used for duplicate detection.
    private String overloadSignature(SemanticDeclaration declaration) {
        return declaration.name() + parameterSignature(parameters(declaration));
    }

    /// Retrieves parameters from an overloadable declaration.
    /// @param declaration The overloadable declaration.
    /// @return The declaration parameters, or an empty array if unavailable.
    private FunctionParameter[] parameters(SemanticDeclaration declaration) {

        if (declaration.node() instanceof FunctionDeclarationStatement functionDeclaration)
            return functionDeclaration.getParameters();
        if (declaration.node() instanceof ClassConstructorDeclaration constructorDeclaration)
            return constructorDeclaration.getParameters();
        return new FunctionParameter[0];
    }

    /// Builds a parameter-type signature.
    /// @param parameters The parameters to include in the signature.
    /// @return A parenthesized list of parameter type spellings.
    private String parameterSignature(FunctionParameter[] parameters) {

        var signature = new StringBuilder("(");
        for (var i = 0; i < parameters.length; i++) {

            if (i > 0) signature.append(", ");
            signature.append(buildTypeStringWithSizes(parameters[i].getType()));
        }
        return signature.append(')').toString();
    }

    /// Reports a diagnostic for a duplicate declaration.
    /// @param declaration The duplicate semantic declaration to report.
    private void reportDuplicate(SemanticDeclaration declaration) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Duplicate declaration '" + declaration.name() + "'",
            declaration.getLine(),
            declaration.getColumn()
        ));
    }
}
