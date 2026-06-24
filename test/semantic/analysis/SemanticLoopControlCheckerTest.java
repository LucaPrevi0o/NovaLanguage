package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic break/continue context checking.
public class SemanticLoopControlCheckerTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> check(String source) {
        return new LoopControlChecker().check(parse(source));
    }

    @Test
    void testReportsTopLevelBreak() {

        var diagnostics = check("break;");

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().getPhase());
        assertTrue(diagnostics.getFirst().getMessage().contains("outside loop or switch"));
    }

    @Test
    void testReportsTopLevelContinue() {

        var diagnostics = check("continue;");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("outside loop"));
    }

    @Test
    void testAcceptsBreakAndContinueInsideLoop() {

        var diagnostics = check("for (;;) { if (true) { continue; } break; }");

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testAcceptsBreakInsideSwitch() {

        var diagnostics = check("int x; switch (x) { case 1 : { break; } default : { } }");

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsContinueInsideSwitchOutsideLoop() {

        var diagnostics = check("int x; switch (x) { case 1 : { continue; } }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("outside loop"));
    }
}
