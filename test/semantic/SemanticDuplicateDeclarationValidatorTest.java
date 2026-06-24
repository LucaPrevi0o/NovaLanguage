package semantic;

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
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().getPhase());
        assertEquals(2, diagnostics.getFirst().getLine());
        assertTrue(diagnostics.getFirst().getMessage().contains("Duplicate declaration 'x'"));
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
        assertEquals(3, diagnostics.getFirst().getLine());
        assertTrue(diagnostics.getFirst().getMessage().contains("Duplicate declaration 'value'"));
    }

    @Test
    void testAllowsMultipleConstructorsUntilSignatureValidationExists() {

        var diagnostics = validate(parse("""
            public class Box {
                public Box() { }
                public Box(int value) { }
            }
            """));

        assertTrue(diagnostics.isEmpty());
    }
}
