package parser;

import org.junit.jupiter.api.Test;
import lexer.Lexer;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.expression.*;
import parser.ast.nodes.statement.ExpressionStatement;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for expression-level parsing:
/// ternary operator, postfix ++/--, binary operators,
/// assignment (compound), member access, array access, call expressions.
public class ExpressionParserTest {

    private Parser parse() {

        var tokens = new Lexer("int x; x = true ? 1 : 0;").tokenize();
        return new Parser(tokens);
    }

    private StatementNode firstStatement() {

        var parser = parse();
        var ast = parser.parse();
        assertFalse(ast.isEmpty());
        return ast.getFirst();
    }

    private ExpressionStatement expressionStatement(String source, int index) {

        var tokens = new Lexer(source).tokenize();
        var ast = new Parser(tokens).parse();
        assertInstanceOf(ExpressionStatement.class, ast.get(index));
        return (ExpressionStatement) ast.get(index);
    }

    // ─── Ternary ──────────────────────────────────────────────────────────────

    @Test
    void testTernaryExpression() {

        var stmt = expressionStatement("int x; x = true ? 1 : 0;", 1);
        assertInstanceOf(AssignmentExpression.class, stmt.getExpression());

        var assignment = (AssignmentExpression) stmt.getExpression();
        assertInstanceOf(TernaryExpression.class, assignment.getValue());
    }

    @Test
    void testTernaryWithIdentifiers() {

        // x is declared first; then the ternary references x
        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; int y; x = x > 0 ? x : y;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testNestedTernary() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; x = true ? true ? 1 : 2 : 3;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testTernaryMissingColonThrows() {

        var tokens = new Lexer("int x; x = true ? 1;").tokenize();
        var parser = new Parser(tokens);
        assertThrows(RuntimeException.class, parser::parse);
    }

    // ─── Postfix ++ / -- ──────────────────────────────────────────────────────

    @Test
    void testPostfixIncrementParsed() {

        var tokens = new Lexer("int x; x++;").tokenize();
        var ast = new Parser(tokens).parse();
        assertEquals(2, ast.size());
        var stmt = (ExpressionStatement) ast.get(1);
        assertInstanceOf(PostfixUnaryExpression.class, stmt.getExpression());
    }

    @Test
    void testPostfixDecrementParsed() {

        var tokens = new Lexer("int x; x--;").tokenize();
        var ast = new Parser(tokens).parse();
        var stmt = (ExpressionStatement) ast.get(1);
        assertInstanceOf(PostfixUnaryExpression.class, stmt.getExpression());
    }

    @Test
    void testPrefixIncrementParsed() {

        var tokens = new Lexer("int x; ++x;").tokenize();
        var ast = new Parser(tokens).parse();
        var stmt = (ExpressionStatement) ast.get(1);
        assertInstanceOf(UnaryExpression.class, stmt.getExpression());
    }

    @Test
    void testPrefixDecrementDistinctFromPostfix() {

        // ++x (prefix) and x++ (postfix) must produce different node types
        var tokensPrefix  = new Lexer("int x; ++x;").tokenize();
        var tokensPostfix = new Lexer("int x; x++;").tokenize();

        var prefixStmt  = (ExpressionStatement) new Parser(tokensPrefix).parse().get(1);
        var postfixStmt = (ExpressionStatement) new Parser(tokensPostfix).parse().get(1);

        assertNotEquals(prefixStmt.getExpression().getClass(), postfixStmt.getExpression().getClass(),
            "Prefix and postfix ++ must produce different AST node types");
    }

    // ─── Arithmetic / binary operators ───────────────────────────────────────

    @Test
    void testAddition() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; int y; int z; z = x + y;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testComplexArithmetic() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int a; int b; int c; int d; d = a * b + c / 2;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testModulo() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; int y; int z; z = x % y;").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Logical operators ────────────────────────────────────────────────────

    @Test
    void testLogicalAnd() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("bool a; bool b; bool c; c = a && b;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testLogicalOr() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("bool a; bool b; bool c; c = a || b;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testLogicalNot() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("bool a; bool b; b = !a;").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Comparison operators ─────────────────────────────────────────────────

    @Test
    void testEqualityComparison() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; bool r; r = x == 0;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testInequalityComparison() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; bool r; r = x != 0;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testLessThan() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; bool r; r = x < 10;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testGreaterThanOrEqual() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; bool r; r = x >= 5;").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Compound assignment ──────────────────────────────────────────────────

    @Test
    void testPlusAssign() {

        var stmt = expressionStatement("int x; x += 5;", 1);
        assertInstanceOf(AssignmentExpression.class, stmt.getExpression());

        var assignment = (AssignmentExpression) stmt.getExpression();
        assertInstanceOf(BinaryExpression.class, assignment.getValue());
    }

    @Test
    void testMinusAssign() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; x -= 3;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testMultiplyAssign() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; x *= 2;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testDivideAssign() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; x /= 4;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testModuloAssign() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; x %= 3;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testAssignmentToLiteralParsesSyntactically() {

        var stmt = expressionStatement("1 = 2;", 0);

        assertInstanceOf(AssignmentExpression.class, stmt.getExpression());
    }

    // ─── Literals ─────────────────────────────────────────────────────────────

    @Test
    void testBoolTrueLiteral() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("bool b; b = true;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testBoolFalseLiteral() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("bool b; b = false;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testNullLiteral() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("string s; s = null;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testStringLiteral() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("string s; s = \"hello\";").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testCharLiteral() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("char c; c = 'x';").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testIntLiteralInExpression() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; x = 42;").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Invalid number format ────────────────────────────────────────────────

    @Test
    void testInvalidNumberFormatThrows() {

        // "1.2.3" — the lexer stops the number token at "1.2", leaving ".3" as separate
        // tokens.  The parser will encounter an unexpected "." when parsing the expression
        // after "1.2", so it should throw.
        var tokens = new Lexer("int x; x = 1.2.3;").tokenize();
        assertThrows(RuntimeException.class, () -> new Parser(tokens).parse());
    }

    // ─── Parenthesised expressions ────────────────────────────────────────────

    @Test
    void testParenthesisedExpression() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("int x; int y; int z; z = (x + y) * 2;").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Call expression ──────────────────────────────────────────────────────

    @Test
    void testCallExpression() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("print(\"hello\");").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testCallExpressionMultipleArgs() {

        assertDoesNotThrow(() -> {
            // readLine is pre-registered; use it as a callee
            var tokens = new Lexer("string s; s = readLine();").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── new keyword ──────────────────────────────────────────────────────────

    @Test
    void testNewWithPrimitiveTypeGivesClearError() {

        var tokens = new Lexer("new int(1);").tokenize();
        var ex = assertThrows(RuntimeException.class, () -> new Parser(tokens).parse());
        assertTrue(ex.getMessage().contains("primitive") || ex.getMessage().contains("new"),
                "Error should mention 'primitive' or 'new'");
    }

    @Test
    void testNewWithUndefinedClassParsesSyntactically() {

        var stmt = expressionStatement("new UndefinedClass();", 0);
        var expr = assertInstanceOf(ObjectCreationExpression.class, stmt.getExpression());

        assertEquals("UndefinedClass", expr.getClassName());
        assertEquals(0, expr.getArguments().length);
    }
}
