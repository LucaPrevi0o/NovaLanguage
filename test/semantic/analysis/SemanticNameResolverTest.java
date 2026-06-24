package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic name resolution after parsing.
public class SemanticNameResolverTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> resolve(String source) {
        return new NameResolver().resolve(parse(source));
    }

    @Test
    void testReportsUndefinedIdentifierAfterParsing() {

        var diagnostics = resolve("unknownVar + 1;");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().phase());
        assertTrue(diagnostics.getFirst().message().contains("Undefined name 'unknownVar'"));
    }

    @Test
    void testReportsIdentifierOutsideBlockScope() {

        var diagnostics = resolve("{ int x; } x;");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("Undefined name 'x'"));
    }

    @Test
    void testResolvesVisibleVariablesParametersAndClasses() {

        var diagnostics = resolve("""
            public class Box { }
            int global;
            Box box;
            void main(int arg) {
                int local = global;
                box = new Box();
                local = arg;
                { local = global; }
            }
            """);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsUndefinedConstructedClassAfterParsing() {

        var diagnostics = resolve("new MissingClass();");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().phase());
        assertTrue(diagnostics.getFirst().message().contains("Undefined class 'MissingClass'"));
    }

    @Test
    void testReportsUndefinedSuperclassAfterParsing() {

        var diagnostics = resolve("public class Orphan :: NoParent { }");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().phase());
        assertTrue(diagnostics.getFirst().message().contains("Undefined superclass 'NoParent'"));
    }

    @Test
    void testReportsUndefinedDeclaredTypeAfterParsing() {

        var diagnostics = resolve("MissingType value;");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().phase());
        assertTrue(diagnostics.getFirst().message().contains("Undefined type 'MissingType'"));
    }

    @Test
    void testReportsUndefinedParameterTypeAfterParsing() {

        var diagnostics = resolve("void f(MissingType value) { }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("Undefined type 'MissingType'"));
    }

    @Test
    void testResolvesForwardDeclaredVariableTypeAfterDeclarationCollection() {

        var diagnostics = resolve("Later value; public class Later { }");

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testResolvesForwardSuperclassAfterDeclarationCollection() {

        var diagnostics = resolve("public class Child :: Base { } public class Base { }");

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testResolvesForEachElementInsideBody() {

        var diagnostics = resolve("int items; for (int elem : items) { elem; }");

        assertTrue(diagnostics.isEmpty());
    }
}
