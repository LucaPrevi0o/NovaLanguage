package parser.project;

import lexer.Lexer;
import lexer.Token;
import lexer.token.LiteralToken;
import parser.Parser;
import parser.ast.SymbolTable;
import parser.ast.nodes.StatementNode;
import parser.parser.util.ParseErrorsException;
import parser.parser.util.ParseException;
import token.TypeRegistry;
import token.family.Keyword;
import token.family.Literal;
import semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Multi-file orchestration parser for Nova projects.
/// Performs:
/// 1) file discovery,
/// 2) class-name collection pass for forward references,
/// 3) definition pass with shared symbol/type context.
public class ProjectParser {

    private final StdLibLoader stdLibLoader;
    private final boolean runSemanticAnalysis;

    public ProjectParser() { this(new FileSystemStdLibLoader(Path.of("stdlib")), true); }

    public ProjectParser(StdLibLoader stdLibLoader) {
        this(stdLibLoader, true);
    }

    public ProjectParser(StdLibLoader stdLibLoader, boolean runSemanticAnalysis) {

        this.stdLibLoader = stdLibLoader;
        this.runSemanticAnalysis = runSemanticAnalysis;
    }

    public ProjectParseResult parse(Path rootOrFile) throws IOException {

        var userFiles = discoverNovaFiles(rootOrFile);
        var stdlibFiles = stdLibLoader.loadStdLibFiles();
        return parseFiles(stdlibFiles, userFiles);
    }

    public ProjectParseResult parseFiles(List<Path> stdlibFiles, List<Path> userFiles) throws IOException {

        var globalSymbolTable = new SymbolTable(null, new ArrayList<>(), new ArrayList<>());
        var typeRegistry = new TypeRegistry();
        var astByFile = new LinkedHashMap<Path, List<StatementNode>>();
        var errors = new ArrayList<ParseException>();

        var orderedAll = new ArrayList<Path>();
        orderedAll.addAll(stdlibFiles);
        orderedAll.addAll(userFiles);

        var tokensByFile = new LinkedHashMap<Path, List<Token>>();
        for (var file : orderedAll) {

            var source = Files.readString(file);
            var tokens = new Lexer(source).tokenize();
            tokensByFile.put(file, tokens);
            collectClassNames(tokens, typeRegistry);
        }

        var stdlibInitialized = false;
        for (var entry : tokensByFile.entrySet()) try {

            var parser = new Parser(entry.getValue(), globalSymbolTable, typeRegistry, !stdlibInitialized && stdlibFiles.isEmpty());
            stdlibInitialized = true;
            var ast = parser.parse();
            astByFile.put(entry.getKey(), ast);
        } catch (ParseErrorsException e) { errors.addAll(e.getErrors()); }
        catch (ParseException e) { errors.add(e); }

        if (!errors.isEmpty()) throw new ProjectParseException(errors);
        if (runSemanticAnalysis) {

            var analyzer = new SemanticAnalyzer();
            var diagnostics = new ArrayList<parser.parser.util.Diagnostic>();
            for (var ast : astByFile.values()) diagnostics.addAll(analyzer.analyze(ast));
            if (!diagnostics.isEmpty()) throw new ProjectAnalysisException(diagnostics);
        }
        return new ProjectParseResult(astByFile, globalSymbolTable, typeRegistry);
    }

    private static List<Path> discoverNovaFiles(Path rootOrFile) throws IOException {

        if (Files.isRegularFile(rootOrFile)) return List.of(rootOrFile);
        try (var stream = Files.walk(rootOrFile)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".nv"))
                .sorted(Comparator.naturalOrder())
                .toList();
        }
    }

    private static void collectClassNames(List<Token> tokens, TypeRegistry typeRegistry) {

        for (var i = 0; i < tokens.size() - 1; i++) {

            var token = tokens.get(i);
            if (token.getType() != Keyword.CLASS) continue;

            var nameToken = tokens.get(i + 1);
            if (nameToken instanceof LiteralToken literal && literal.getType() instanceof Literal.IdentifierLiteral)
                typeRegistry.registerClassName(literal.getValue());
        }
    }
}
