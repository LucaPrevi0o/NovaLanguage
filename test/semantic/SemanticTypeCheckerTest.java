package semantic;

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
}
