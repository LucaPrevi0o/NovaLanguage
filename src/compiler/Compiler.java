package compiler;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import error.diagnostic.ParseErrorsException;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import semantic.analysis.DuplicateDeclarationValidator;
import semantic.analysis.LValueChecker;
import semantic.analysis.LoopControlChecker;
import semantic.analysis.NameResolver;
import semantic.analysis.ReturnChecker;
import semantic.analysis.TypeChecker;
import semantic.declaration.DeclarationCollector;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/// Project-level compiler front-end orchestrator.
///
/// The compiler coordinates existing lexer, parser, and semantic-analysis
/// layers across multiple source files. It does not implement grammar rules or
/// semantic checks itself.
public final class Compiler {

    /// Compiles source files through the project front-end pipeline.
    /// @param sourceFiles Source files to compile.
    /// @return The project context containing units, declarations, scopes, and diagnostics.
    public ProjectContext compile(SourceFile... sourceFiles) {
        return compile(sourceFiles != null ? Arrays.asList(sourceFiles) : null);
    }

    /// Compiles source files through the project front-end pipeline.
    /// @param sourceFiles Source files to compile.
    /// @return The project context containing units, declarations, scopes, and diagnostics.
    public ProjectContext compile(List<SourceFile> sourceFiles) {

        var diagnostics = new ArrayList<SourceDiagnostic>();
        var sources = normalizeSources(sourceFiles, diagnostics);
        validateUniqueIdentities(sources, diagnostics);

        var lexedSources = lexAll(sources);
        var units = parseAll(lexedSources);
        for (var unit : units) diagnostics.addAll(unit.diagnostics());

        var syntaxContext = ProjectContext.syntaxOnly(units, diagnostics);
        if (syntaxContext.hasErrors()) return syntaxContext;

        var statements = syntaxContext.statements();
        var declarations = new DeclarationCollector().collect(statements);
        var rootScope = new SemanticScopeBuilder().build(statements);

        runSemanticPasses(statements, rootScope, units, diagnostics);
        return new ProjectContext(units, diagnostics, declarations, rootScope);
    }

    /// Returns source files that can enter the pipeline and reports invalid inputs.
    /// @param sourceFiles The requested source files.
    /// @param diagnostics The diagnostics being aggregated.
    /// @return Non-null source files in input order.
    private List<SourceFile> normalizeSources(List<SourceFile> sourceFiles, List<SourceDiagnostic> diagnostics) {

        if (sourceFiles == null) {

            reportProjectError(diagnostics, "Source file list must not be null");
            return List.of();
        }

        var sources = new ArrayList<SourceFile>();
        for (var sourceFile : sourceFiles)
            if (sourceFile == null) reportProjectError(diagnostics, "Source file must not be null");
            else sources.add(sourceFile);
        return List.copyOf(sources);
    }

    /// Reports duplicate source identities before semantic analysis.
    /// @param sourceFiles Source files to validate.
    /// @param diagnostics The diagnostics being aggregated.
    private void validateUniqueIdentities(List<SourceFile> sourceFiles, List<SourceDiagnostic> diagnostics) {

        var seen = new LinkedHashSet<String>();
        for (var sourceFile : sourceFiles)
            if (!seen.add(sourceFile.identity()))
                reportProjectError(diagnostics, "Duplicate source file identity '" + sourceFile.identity() + "'");
    }

    /// Lexes every source file before parsing begins.
    /// @param sourceFiles Source files to lex.
    /// @return Lexed source results in input order.
    private List<LexedSource> lexAll(List<SourceFile> sourceFiles) {

        var lexedSources = new ArrayList<LexedSource>();
        for (var sourceFile : sourceFiles) {

            var lexer = new Lexer(sourceFile.source());
            var tokens = lexer.tokenize();
            lexedSources.add(new LexedSource(sourceFile, tokens, lexer.getDiagnostics()));
        }
        return List.copyOf(lexedSources);
    }

    /// Parses every token stream into a compilation unit.
    /// @param lexedSources Lexed source results.
    /// @return Compilation units in input order.
    private List<CompilationUnit> parseAll(List<LexedSource> lexedSources) {

        var units = new ArrayList<CompilationUnit>();
        for (var lexedSource : lexedSources) units.add(parse(lexedSource));
        return List.copyOf(units);
    }

    /// Parses one lexed source into a compilation unit.
    /// @param lexedSource The lexed source to parse.
    /// @return A compilation unit with tokens, parsed statements, and syntax diagnostics.
    private CompilationUnit parse(LexedSource lexedSource) {

        var parser = new Parser(lexedSource.tokens());
        List<StatementNode> statements;
        List<Diagnostic> parserDiagnostics;

        try {

            statements = parser.parse();
            parserDiagnostics = List.of();
        } catch (ParseErrorsException e) {

            statements = parser.getParsedStatements();
            parserDiagnostics = e.getDiagnostics();
        }

        return new CompilationUnit(
            lexedSource.sourceFile(),
            lexedSource.tokens(),
            statements,
            lexedSource.lexerDiagnostics(),
            parserDiagnostics
        );
    }

    /// Runs project-level semantic passes over the flattened project AST.
    /// @param statements All top-level statements across all units.
    /// @param rootScope The project root semantic scope.
    /// @param units Compilation units used for best-effort diagnostic source mapping.
    /// @param diagnostics The diagnostics being aggregated.
    private void runSemanticPasses(List<StatementNode> statements, SemanticScope rootScope, List<CompilationUnit> units,
    List<SourceDiagnostic> diagnostics) {

        reportSemanticDiagnostics(new NameResolver().resolve(statements), units, diagnostics);
        reportSemanticDiagnostics(new DuplicateDeclarationValidator().validate(rootScope), units, diagnostics);
        reportSemanticDiagnostics(new TypeChecker().check(statements), units, diagnostics);
        reportSemanticDiagnostics(new LValueChecker().check(statements), units, diagnostics);
        reportSemanticDiagnostics(new ReturnChecker().check(statements), units, diagnostics);
        reportSemanticDiagnostics(new LoopControlChecker().check(statements), units, diagnostics);
    }

    /// Adds semantic diagnostics with source identity when one unit can be identified.
    /// @param semanticDiagnostics Plain semantic diagnostics.
    /// @param units Compilation units used for location matching.
    /// @param diagnostics The diagnostics being aggregated.
    private void reportSemanticDiagnostics(List<Diagnostic> semanticDiagnostics, List<CompilationUnit> units,
    List<SourceDiagnostic> diagnostics) {

        for (var diagnostic : semanticDiagnostics) diagnostics.add(withSource(diagnostic, units));
    }

    /// Wraps a diagnostic with source identity when the diagnostic location uniquely identifies one unit.
    /// @param diagnostic The diagnostic to wrap.
    /// @param units Compilation units used for location matching.
    /// @return A source-aware diagnostic or a project-level diagnostic when no unique source can be found.
    private SourceDiagnostic withSource(Diagnostic diagnostic, List<CompilationUnit> units) {

        if (!diagnostic.hasLocation()) return SourceDiagnostic.project(diagnostic);
        if (units.size() == 1) return SourceDiagnostic.from(units.getFirst().sourceFile(), diagnostic);

        SourceFile match = null;
        for (var unit : units) if (unitHasTokenAt(unit, diagnostic.line(), diagnostic.column())) {

            if (match != null) return SourceDiagnostic.project(diagnostic);
            match = unit.sourceFile();
        }
        return match != null ? SourceDiagnostic.from(match, diagnostic) : SourceDiagnostic.project(diagnostic);
    }

    /// Returns whether a unit has a token at the given source location.
    /// @param unit The unit to inspect.
    /// @param line The source line.
    /// @param column The source column.
    /// @return `true` when a token starts at the given location.
    private boolean unitHasTokenAt(CompilationUnit unit, int line, int column) {

        for (var token : unit.tokens())
            if (token.getLine() == line && token.getColumn() == column) return true;
        return false;
    }

    /// Reports a project-level compiler diagnostic.
    /// @param diagnostics The diagnostics being aggregated.
    /// @param message The diagnostic message.
    private void reportProjectError(List<SourceDiagnostic> diagnostics, String message) {
        diagnostics.add(SourceDiagnostic.project(Diagnostic.error(DiagnosticPhase.COMPILER, message)));
    }

    /// Tokenized source-file result produced before parsing begins.
    /// @param sourceFile The source file.
    /// @param tokens The token stream.
    /// @param lexerDiagnostics Lexer diagnostics for the source file.
    private record LexedSource(SourceFile sourceFile, List<Token> tokens, List<Diagnostic> lexerDiagnostics) {

        private LexedSource {

            tokens = tokens != null ? List.copyOf(tokens) : List.of();
            lexerDiagnostics = lexerDiagnostics != null ? List.copyOf(lexerDiagnostics) : List.of();
        }
    }
}
