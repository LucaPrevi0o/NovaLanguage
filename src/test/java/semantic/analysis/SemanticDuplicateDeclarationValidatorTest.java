package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic duplicate declaration validation.
public class SemanticDuplicateDeclarationValidatorTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> validate(List<StatementNode> ast) {
        return new DuplicateDeclarationValidator().validate(ast);
    }

    @Test
    void testReportsDuplicateVariablesInSameScope() {

        var diagnostics = validate(parse("int x;\nint x;"));

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().phase());
        assertEquals(2, diagnostics.getFirst().line());
        assertTrue(diagnostics.getFirst().message().contains("Duplicate declaration 'x'"));
    }

    @Test
    void testAllowsNestedShadowing() {

        var diagnostics = validate(parse("int x; { int x; }"));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsDuplicateFieldsInClassScope() {

        var diagnostics = validate(parse("""
            public class Box {
                private int value;
                private int value;
            }
            """));

        assertEquals(1, diagnostics.size());
        assertEquals(3, diagnostics.getFirst().line());
        assertTrue(diagnostics.getFirst().message().contains("Duplicate declaration 'value'"));
    }

    @Test
    void testAllowsFunctionOverloadsWithDifferentParameterTypes() {

        var diagnostics = validate(parse("""
            int choose(int value) { return value; }
            bool choose(bool value) { return value; }
            """));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsDuplicateFunctionSignature() {

        var diagnostics = validate(parse("""
            int choose(int value) { return value; }
            bool choose(int other) { return true; }
            """));

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().line());
        assertTrue(diagnostics.getFirst().message().contains("Duplicate declaration 'choose'"));
    }

    @Test
    void testAllowsMethodOverloadsWithDifferentParameterTypes() {

        var diagnostics = validate(parse("""
            public class Box {
                public int get(int value) { return value; }
                public bool get(bool value) { return value; }
            }
            """));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsDuplicateMethodSignature() {

        var diagnostics = validate(parse("""
            public class Box {
                public int get(int value) { return value; }
                public bool get(int other) { return true; }
            }
            """));

        assertEquals(1, diagnostics.size());
        assertEquals(3, diagnostics.getFirst().line());
        assertTrue(diagnostics.getFirst().message().contains("Duplicate declaration 'get'"));
    }

    @Test
    void testAllowsConstructorOverloadsWithDifferentParameterTypes() {

        var diagnostics = validate(parse("""
            public class Box {
                public Box() { }
                public Box(int value) { }
            }
            """));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsDuplicateConstructorSignature() {

        var diagnostics = validate(parse("""
            public class Box {
                public Box(int value) { }
                public Box(int other) { }
            }
            """));

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("Duplicate declaration 'Box'"));
    }
}
