package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic return checking.
public class SemanticReturnCheckerTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> check(String source) {
        return new ReturnChecker().check(parse(source));
    }

    @Test
    void testReportsReturnOutsideFunction() {

        var diagnostics = check("return 1;");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().getPhase());
        assertTrue(diagnostics.getFirst().getMessage().contains("outside function"));
    }

    @Test
    void testReportsValueReturnedFromVoidFunction() {

        var diagnostics = check("void f() { return 1; }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("cannot return a value"));
    }

    @Test
    void testReportsMissingValueFromNonVoidFunction() {

        var diagnostics = check("int f() { return; }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("must return a value"));
    }

    @Test
    void testReportsMissingReturnInNonVoidFunction() {

        var diagnostics = check("int f() { int x; }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Missing return"));
    }

    @Test
    void testAcceptsIfElseWhenBothBranchesReturn() {

        var diagnostics = check("int f(bool b) { if (b) { return 1; } else { return 2; } }");

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsConstructorReturningValue() {

        var diagnostics = check("public class Box { public Box() { return 1; } }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Constructor cannot return a value"));
    }
}
