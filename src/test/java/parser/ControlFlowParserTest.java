package parser;

import org.junit.jupiter.api.Test;
import lexer.Lexer;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.*;
import parser.ast.nodes.statement.conditional.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for all control-flow statements:
/// if/else, while, for, for-each, switch (with case and default).
public class ControlFlowParserTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    // ─── if / else ────────────────────────────────────────────────────────────

    @Test
    void testIfStatement() {

        var ast = parse("bool cond; if (cond) { }");
        assertEquals(2, ast.size());
        assertInstanceOf(IfStatement.class, ast.get(1));
    }

    @Test
    void testIfElseStatement() {

        var ast = parse("bool cond; if (cond) { } else { }");
        assertEquals(2, ast.size());
        var ifStmt = (IfStatement) ast.get(1);
        assertNotNull(ifStmt.getElseBlock(), "else branch should be non-null");
    }

    @Test
    void testIfWithoutElse() {

        var ast = parse("bool cond; if (cond) { }");
        var ifStmt = (IfStatement) ast.get(1);
        assertNull(ifStmt.getElseBlock(), "no else-branch expected");
    }

    @Test
    void testNestedIf() {
        assertDoesNotThrow(() -> parse("bool a; bool b; if (a) { if (b) { } }"));
    }

    @Test
    void testIfElseChain() {
        assertDoesNotThrow(() -> parse("int x; if (x == 1) { } else if (x == 2) { } else { }"));
    }

    @Test
    void testIfWithSingleStatement() {
        assertDoesNotThrow(() -> parse("bool cond; int x; if (cond) x = 1;"));
    }

    // ─── while ────────────────────────────────────────────────────────────────

    @Test
    void testWhileStatement() {

        var ast = parse("bool cond; while (cond) { }");
        assertEquals(2, ast.size());
        assertInstanceOf(WhileStatement.class, ast.get(1));
    }

    @Test
    void testWhileWithBody() {
        assertDoesNotThrow(() -> parse("int i; while (i < 10) { i++; }"));
    }

    @Test
    void testWhileNestedLoop() {
        assertDoesNotThrow(() -> parse("int i; int j; while (i < 3) { while (j < 3) { j++; } i++; }"));
    }

    // ─── for ──────────────────────────────────────────────────────────────────

    @Test
    void testForStatement() {

        var ast = parse("for (int i = 0; i < 10; i++) { }");
        assertEquals(1, ast.size());
        assertInstanceOf(ForStatement.class, ast.getFirst());
    }

    @Test
    void testForStatementBodyExecuted() {
        assertDoesNotThrow(() -> parse("for (int i = 0; i < 5; i++) { print(\"hi\"); }"));
    }

    @Test
    void testForWithEmptyInitializer() {
        assertDoesNotThrow(() -> parse("int i; for (; i < 10; i++) { }"));
    }

    @Test
    void testForWithEmptyCondition() {
        assertDoesNotThrow(() -> parse("for (int i = 0; ; i++) { break; }"));
    }

    @Test
    void testForWithEmptyIncrement() {
        assertDoesNotThrow(() -> parse("int i; for (i = 0; i < 5;) { i++; }"));
    }

    @Test
    void testForWithAllEmpty() {
        assertDoesNotThrow(() -> parse("for (;;) { break; }"));
    }

    // ─── for-each ─────────────────────────────────────────────────────────────

    @Test
    void testForEachStatement() {

        // We need an iterable expression — use an existing variable reference
        assertDoesNotThrow(() -> parse("public class MyList { } MyList items; for (int elem : items) { }"));
    }

    @Test
    void testForEachHasCorrectNodeType() {

        var ast = parse("public class MyList { } MyList items; for (int elem : items) { }");
        // last statement is the for-each
        assertInstanceOf(ForEachStatement.class, ast.getLast());
    }

    @Test
    void testForEachElementNameAvailableInBody() {

        // elem must be visible inside the for-each body
        assertDoesNotThrow(() -> parse("public class MyList { } MyList items; for (int elem : items) { print(\"x\"); }"));
    }

    // ─── switch ───────────────────────────────────────────────────────────────

    @Test
    void testSwitchStatement() {

        var ast = parse("int x; switch (x) { case 1 : { } case 2 : { } }");
        assertFalse(ast.isEmpty());
        assertInstanceOf(SwitchStatement.class, ast.getLast());
    }

    @Test
    void testSwitchWithDefault() {

        var ast = parse("int x; switch (x) { case 1 : { } default : { } }");
        var sw = (SwitchStatement) ast.getLast();
        assertEquals(2, sw.getCases().length);
    }

    @Test
    void testSwitchDefaultCaseHasNullValue() {

        var ast = parse("int x; switch (x) { default : { } }");
        var sw = (SwitchStatement) ast.getLast();
        // Default arm has null value in the AST
        assertNull(sw.getCases()[0].getValue(), "default case should have null value");
    }

    @Test
    void testSwitchNoCases() {

        var ast = parse("int x; switch (x) { }");
        var sw = (SwitchStatement) ast.getLast();
        assertEquals(0, sw.getCases().length);
    }

    @Test
    void testSwitchMultipleCases() {

        var ast = parse("int x; switch (x) { case 1 : { } case 2 : { } case 3 : { } }");
        var sw = (SwitchStatement) ast.getLast();
        assertEquals(3, sw.getCases().length);
    }

    @Test
    void testSwitchCaseBodyIsStatement() {
        assertDoesNotThrow(() -> parse("int x; switch (x) { case 1 : print(\"one\"); case 2 : print(\"two\"); }"));
    }

    // ─── break / continue ─────────────────────────────────────────────────────

    @Test
    void testBreakStatement() {

        var ast = parse("for (;;) { break; }");
        assertNotNull(ast);
    }

    @Test
    void testContinueStatement() {

        var ast = parse("for (;;) { continue; }");
        assertNotNull(ast);
    }

    // ─── return ───────────────────────────────────────────────────────────────

    @Test
    void testReturnWithValue() {

        var ast = parse("int foo() { return 42; }");
        assertFalse(ast.isEmpty());
    }

    @Test
    void testReturnVoid() {
        assertDoesNotThrow(() -> parse("void bar() { return; }"));
    }

    // ─── block ────────────────────────────────────────────────────────────────

    @Test
    void testBlockStatement() {

        var ast = parse("{ int x; }");
        assertEquals(1, ast.size());
        assertInstanceOf(BlockStatement.class, ast.getFirst());
    }

    @Test
    void testNestedBlock() {
        assertDoesNotThrow(() -> parse("{ { int x; } }"));
    }
}
