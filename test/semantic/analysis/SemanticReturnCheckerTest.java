package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import lexer.token.ReturnType;
import lexer.token.family.PrimitiveType;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.expression.literal.number.IntegerLiteralExpression;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ReturnStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;

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
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().phase());
        assertTrue(diagnostics.getFirst().message().contains("outside function"));
    }

    @Test
    void testReportsValueReturnedFromVoidFunction() {

        var diagnostics = check("void f() { return 1; }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("cannot return a value"));
    }

    @Test
    void testReportsMissingValueFromNonVoidFunction() {

        var diagnostics = check("int f() { return; }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("must return a value"));
    }

    @Test
    void testReportsMissingReturnInNonVoidFunction() {

        var diagnostics = check("int f() { int x; }");

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("Missing return"));
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
        assertTrue(diagnostics.getFirst().message().contains("Constructor cannot return a value"));
    }

    @Test
    void testSyntaxlessVoidReturnTypeFallbackUsesTypeSyntaxBridge() {

        var body = new BlockStatement(1, 1, new StatementNode[] {
            new ReturnStatement(1, 1, new IntegerLiteralExpression(1, 1, 1))
        });
        var function = new FunctionDeclarationStatement(
            1,
            1,
            new ReturnType(PrimitiveType.VOID),
            "legacy",
            new FunctionParameter[0],
            body
        );

        var diagnostics = new ReturnChecker().check(List.of(function));

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().message().contains("cannot return a value"));
    }
}
