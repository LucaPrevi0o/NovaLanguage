package compiler;

import error.diagnostic.DiagnosticSeverity;
import parser.ast.nodes.StatementNode;
import semantic.declaration.DeclarationCollection;
import semantic.scope.SemanticScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Immutable project-level compiler front-end result.
///
/// A project context owns the compilation units produced for every source file,
/// file-aware diagnostics aggregated by the project pipeline, the declaration
/// collection built across all units, and the project root semantic scope when
/// semantic analysis has run.
public final class ProjectContext {

    private final List<CompilationUnit> units;
    private final List<SourceDiagnostic> diagnostics;
    private final DeclarationCollection declarations;
    private final SemanticScope rootScope;

    /// Creates a project context.
    /// @param units The ordered compilation units in source input order.
    /// @param diagnostics Aggregated file-aware diagnostics.
    /// @param declarations Declarations collected across all units, or empty when unavailable.
    /// @param rootScope Project root semantic scope, or `null` when semantic analysis did not run.
    public ProjectContext(List<CompilationUnit> units, List<SourceDiagnostic> diagnostics, DeclarationCollection declarations,
    SemanticScope rootScope) {

        this.units = immutableUnits(units);
        this.diagnostics = immutableDiagnostics(diagnostics);
        this.declarations = declarations != null ? declarations : new DeclarationCollection(List.of());
        this.rootScope = rootScope;
    }

    /// Creates a syntax-only project context before semantic analysis has run.
    /// @param units The ordered compilation units.
    /// @param diagnostics Aggregated file-aware diagnostics.
    /// @return A syntax-only project context.
    public static ProjectContext syntaxOnly(List<CompilationUnit> units, List<SourceDiagnostic> diagnostics) {
        return new ProjectContext(units, diagnostics, new DeclarationCollection(List.of()), null);
    }

    /// Returns the ordered compilation units in source input order.
    /// @return The compilation units.
    public List<CompilationUnit> units() { return units; }

    /// Returns aggregated diagnostics from project, syntax, and semantic phases.
    /// @return The aggregated file-aware diagnostics.
    public List<SourceDiagnostic> diagnostics() { return diagnostics; }

    /// Returns declarations collected across all units.
    /// @return The project declaration collection.
    public DeclarationCollection declarations() { return declarations; }

    /// Returns the project root semantic scope, or `null` when semantic analysis did not run.
    /// @return The project root semantic scope.
    public SemanticScope rootScope() { return rootScope; }

    /// Returns all top-level statements from all units in source input order.
    /// @return A flattened immutable list of top-level AST statements.
    public List<StatementNode> statements() {

        var statements = new ArrayList<StatementNode>();
        for (var unit : units) statements.addAll(unit.statements());
        return List.copyOf(statements);
    }

    /// Returns all package placeholder segments from all units in source input order.
    /// @return Flattened package placeholder segments.
    public List<String> packageSegments() {

        var packageSegments = new ArrayList<String>();
        for (var unit : units) packageSegments.addAll(unit.packageSegments());
        return List.copyOf(packageSegments);
    }

    /// Returns all import placeholder spellings from all units in source input order.
    /// @return Flattened import placeholder spellings.
    public List<String> imports() {

        var imports = new ArrayList<String>();
        for (var unit : units) imports.addAll(unit.imports());
        return List.copyOf(imports);
    }

    /// Returns whether any diagnostic in the project context is an error.
    /// @return `true` when any diagnostic has severity ERROR.
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.diagnostic().severity() == DiagnosticSeverity.ERROR);
    }

    /// Returns whether any compilation unit has lexer or parser errors.
    /// @return `true` when syntax-phase errors are present.
    public boolean hasSyntaxErrors() {
        return units.stream().anyMatch(CompilationUnit::hasSyntaxErrors);
    }

    /// Returns an immutable copy of compilation units, rejecting null entries.
    /// @param units The units to copy.
    /// @return An immutable list of units.
    private static List<CompilationUnit> immutableUnits(List<CompilationUnit> units) {

        if (units == null) return List.of();
        var copy = new ArrayList<CompilationUnit>();
        for (var unit : units) copy.add(Objects.requireNonNull(unit, "Compilation unit must not be null"));
        return List.copyOf(copy);
    }

    /// Returns an immutable copy of diagnostics, rejecting null entries.
    /// @param diagnostics The diagnostics to copy.
    /// @return An immutable list of diagnostics.
    private static List<SourceDiagnostic> immutableDiagnostics(List<SourceDiagnostic> diagnostics) {

        if (diagnostics == null) return List.of();
        var copy = new ArrayList<SourceDiagnostic>();
        for (var diagnostic : diagnostics) copy.add(Objects.requireNonNull(diagnostic, "Source diagnostic must not be null"));
        return List.copyOf(copy);
    }
}
