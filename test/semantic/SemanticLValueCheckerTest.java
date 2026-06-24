package semantic;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic l-value checking.
public class SemanticLValueCheckerTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> check(String source) {
        return new LValueChecker().check(parse(source));
    }

    @Test
    void testReportsAssignmentToLiteral() {

        var diagnostics = check("1 = 2;");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().getPhase());
        assertTrue(diagnostics.getFirst().getMessage().contains("Invalid assignment target"));
    }

    @Test
    void testReportsPrefixUpdateOfLiteral() {

        var diagnostics = check("++1;");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Invalid assignment target"));
    }

    @Test
    void testReportsPostfixUpdateOfCallResult() {

        var diagnostics = check("readLine()++;");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Invalid assignment target"));
    }

    @Test
    void testAcceptsIdentifierMemberAndArrayTargets() {

        var diagnostics = check("""
            int x;
            int arr;
            public class Box { public int value; }
            Box box;
            x = 1;
            box.value = 2;
            arr[0] = 3;
            x++;
            --box.value;
            """);

        assertTrue(diagnostics.isEmpty());
    }
}
