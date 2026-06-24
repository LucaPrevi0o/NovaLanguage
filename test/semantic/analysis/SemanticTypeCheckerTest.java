package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the initial semantic type checker.
public class SemanticTypeCheckerTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> check(String source) {
        return new TypeChecker().check(parse(source));
    }

    @Test
    void testReportsVariableInitializerMismatch() {

        var diagnostics = check("int x = true;");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().getPhase());
        assertTrue(diagnostics.getFirst().getMessage().contains("cannot assign bool to int"));
    }

    @Test
    void testReportsAssignmentMismatch() {

        var diagnostics = check("int x; x = true;");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("cannot assign bool to int"));
    }

    @Test
    void testAcceptsMatchingPrimitiveAssignments() {

        var diagnostics = check("""
            int x = 1;
            bool ok = true;
            string s = "hello";
            x = x + 1;
            ok = x > 0;
            """);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testAcceptsMatchingConstructedClassAssignment() {

        var diagnostics = check("""
            public class Box { }
            Box box;
            box = new Box();
            """);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsConstructedClassAssignmentMismatch() {

        var diagnostics = check("""
            public class Box { }
            public class Other { }
            Box box;
            box = new Other();
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("cannot assign Other to Box"));
    }

    @Test
    void testAcceptsMatchingFunctionCallArguments() {

        var diagnostics = check("""
            int choose(int value, bool ok) { return value; }
            int result = choose(1, true);
            """);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsFunctionCallArityMismatch() {

        var diagnostics = check("""
            int choose(int value, bool ok) { return value; }
            int result = choose(1);
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("expects 2 arguments but got 1"));
    }

    @Test
    void testReportsFunctionCallArgumentTypeMismatch() {

        var diagnostics = check("""
            int choose(int value, bool ok) { return value; }
            int result = choose(true, true);
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Argument 1"));
        assertTrue(diagnostics.getFirst().getMessage().contains("expects int but got bool"));
    }

    @Test
    void testUsesFunctionCallReturnTypeInInitializerChecking() {

        var diagnostics = check("""
            int choose(int value, bool ok) { return value; }
            bool result = choose(1, true);
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("cannot assign int to bool"));
    }

    @Test
    void testAcceptsArrayElementAssignmentToMatchingType() {

        var diagnostics = check("""
            int[3] values;
            int value = values[0];
            """);

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsArrayIndexTypeMismatch() {

        var diagnostics = check("""
            int[3] values;
            int value = values[true];
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Array index must be int"));
    }

    @Test
    void testReportsIndexingNonArrayValue() {

        var diagnostics = check("""
            int value;
            int first = value[0];
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Cannot index non-array type int"));
    }

    @Test
    void testUsesArrayElementTypeInInitializerChecking() {

        var diagnostics = check("""
            int[3] values;
            bool value = values[0];
            """);

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("cannot assign int to bool"));
    }
}
