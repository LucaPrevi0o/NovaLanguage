package compiler;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticSeverity;
import lexer.Token;
import parser.ast.nodes.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Syntax result for one Nova source file.
///
/// A compilation unit groups the source file with its token stream, parser AST,
/// lexer diagnostics, parser diagnostics, and future package/import placeholder
/// metadata. It does not own semantic declarations, scopes, or resolved types.
public record CompilationUnit(
        SourceFile sourceFile,
        List<Token> tokens,
        List<StatementNode> statements,
        List<Diagnostic> lexerDiagnostics,
        List<Diagnostic> parserDiagnostics,
        List<String> packageSegments,
        List<String> imports
) {

    /// Creates a compilation unit from one file's syntax-phase data.
    /// @param sourceFile The source file represented by this unit.
    /// @param tokens Tokens produced for the source file.
    /// @param statements Parsed top-level statements.
    /// @param lexerDiagnostics Lexer diagnostics for the file.
    /// @param parserDiagnostics Parser diagnostics for the file.
    /// @param packageSegments Future package-name placeholder segments.
    /// @param imports Future import placeholder spellings.
    public CompilationUnit {

        Objects.requireNonNull(sourceFile, "Source file must not be null");
        tokens = immutable(tokens);
        statements = immutable(statements);
        lexerDiagnostics = immutable(lexerDiagnostics);
        parserDiagnostics = immutable(parserDiagnostics);
        packageSegments = immutable(packageSegments);
        imports = immutable(imports);
    }

    /// Creates a compilation unit without package/import placeholder data.
    /// @param sourceFile The source file represented by this unit.
    /// @param tokens Tokens produced for the source file.
    /// @param statements Parsed top-level statements.
    /// @param lexerDiagnostics Lexer diagnostics for the file.
    /// @param parserDiagnostics Parser diagnostics for the file.
    public CompilationUnit(
            SourceFile sourceFile,
            List<Token> tokens,
            List<StatementNode> statements,
            List<Diagnostic> lexerDiagnostics,
            List<Diagnostic> parserDiagnostics
    ) {
        this(sourceFile, tokens, statements, lexerDiagnostics, parserDiagnostics, List.of(), List.of());
    }

    /// Returns all lexer and parser diagnostics wrapped with this unit's source identity.
    /// @return File-aware diagnostics for syntax-phase diagnostics in this unit.
    public List<SourceDiagnostic> diagnostics() {

        var diagnostics = new ArrayList<SourceDiagnostic>();
        for (var diagnostic : lexerDiagnostics) diagnostics.add(SourceDiagnostic.from(sourceFile, diagnostic));
        for (var diagnostic : parserDiagnostics) diagnostics.add(SourceDiagnostic.from(sourceFile, diagnostic));
        return List.copyOf(diagnostics);
    }

    /// Returns whether this unit has lexer or parser error diagnostics.
    /// @return `true` when any syntax-phase diagnostic is an error.
    public boolean hasSyntaxErrors() {
        return hasErrors(lexerDiagnostics) || hasErrors(parserDiagnostics);
    }

    private static boolean hasErrors(List<Diagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values != null ? List.copyOf(values) : List.of();
    }
}
