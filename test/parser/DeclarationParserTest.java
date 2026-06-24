package parser;

import org.junit.jupiter.api.Test;
import lexer.Lexer;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import semantic.DuplicateDeclarationValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for variable and function declarations, including multi-variable declarations,
/// scope rules, duplicate detection, and parameter parsing.
public class DeclarationParserTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    // ─── Variable declarations ────────────────────────────────────────────────

    @Test
    void testSimpleVariableDeclaration() {

        var ast = parse("int x;");
        assertEquals(1, ast.size());
        assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());
    }

    @Test
    void testVariableWithInitializer() {

        var ast = parse("int x = 42;");
        var decl = (VariableDeclarationStatement) ast.getFirst();
        assertNotNull(decl.getInitialValue(), "initializer should be non-null");
    }

    @Test
    void testStringVariable() {

        assertDoesNotThrow(() -> parse("string s;"));
    }

    @Test
    void testBoolVariable() {

        assertDoesNotThrow(() -> parse("bool b;"));
    }

    @Test
    void testFloatVariable() {

        assertDoesNotThrow(() -> parse("float f;"));
    }

    @Test
    void testDoubleVariable() {

        assertDoesNotThrow(() -> parse("double d;"));
    }

    @Test
    void testLongVariable() {

        assertDoesNotThrow(() -> parse("long l;"));
    }

    @Test
    void testCharVariable() {

        assertDoesNotThrow(() -> parse("char c;"));
    }

    @Test
    void testByteVariable() {

        assertDoesNotThrow(() -> parse("byte b;"));
    }

    @Test
    void testMultiVariableDeclaration() {

        var ast = parse("int a, b, c;");
        // Multiple declarations wrapped in a block
        assertFalse(ast.isEmpty());
    }

    @Test
    void testMultiVariableWithInitializers() {

        assertDoesNotThrow(() -> parse("int a = 1, b = 2;"));
    }

    @Test
    void testFirstVariableVisibleInSecondInitializer() {

        // 'a' should be visible when the initializer for 'b' is parsed (since a is registered first)
        assertDoesNotThrow(() -> parse("int a = 1, b = a;"));
    }

    @Test
    void testDuplicateVariableParsesSyntactically() {

        assertDoesNotThrow(() -> parse("int x; int x;"));
    }

    @Test
    void testDuplicateVariableSemanticDiagnosticHasLocation() {

        var ast = parse("int x;\nint x;");
        var diagnostics = new DuplicateDeclarationValidator().validate(ast);

        assertEquals(1, diagnostics.size());
        assertEquals(2, diagnostics.getFirst().getLine());
    }

    @Test
    void testUndefinedVariableExpressionParsesSyntactically() {

        assertDoesNotThrow(() -> parse("unknownVar;"));
    }

    // ─── Function declarations ────────────────────────────────────────────────

    @Test
    void testSimpleFunctionDeclaration() {

        var ast = parse("void foo() { }");
        assertEquals(1, ast.size());
        assertInstanceOf(FunctionDeclarationStatement.class, ast.getFirst());
    }

    @Test
    void testFunctionWithReturnType() {

        var ast = parse("int getAnswer() { return 42; }");
        var fn = (FunctionDeclarationStatement) ast.getFirst();
        assertNotNull(fn.getBody(), "function body should be non-null");
    }

    @Test
    void testFunctionWithParameters() {

        var ast = parse("int add(int a, int b) { return 0; }");
        var fn = (FunctionDeclarationStatement) ast.getFirst();
        assertEquals(2, fn.getParameters().length);
    }

    @Test
    void testFunctionParameterNames() {

        var ast = parse("void greet(string name) { }");
        var fn = (FunctionDeclarationStatement) ast.getFirst();
        assertEquals("name", fn.getParameters()[0].getName());
    }

    @Test
    void testFunctionWithManyParameters() {

        assertDoesNotThrow(() -> parse("void fn(int a, int b, int c, int d) { }"));
    }

    @Test
    void testFunctionRegisteredInEnclosingScope() {

        // After parsing, the function symbol must be in the top-level symbol table
        var tokens = new Lexer("int compute() { return 1; }").tokenize();
        var parser = new Parser(tokens);
        parser.parse();
        assertNotNull(parser.getSymbolTable().lookup("compute"),
                "function should be in the global symbol table");
    }

    @Test
    void testRecursiveFunction() {

        // A function that calls itself recursively should parse without error
        assertDoesNotThrow(() ->
            parse("int factorial(int n) { if (n == 0) return 1; return factorial(n); }"));
    }

    @Test
    void testForwardReferenceCallParsesSyntactically() {

        assertDoesNotThrow(() -> parse("callMe(); void callMe() { }"));
    }

    // ─── Variable scoping ─────────────────────────────────────────────────────

    @Test
    void testOutOfScopeVariableExpressionParsesSyntactically() {

        assertDoesNotThrow(() -> parse("{ int x; } x;"));
    }

    @Test
    void testVariableVisibleInNestedScope() {

        // x declared outside is visible inside the block
        assertDoesNotThrow(() -> parse("int x; { x = 5; }"));
    }

    @Test
    void testFunctionLocalVariableExpressionParsesSyntacticallyOutsideFunction() {

        assertDoesNotThrow(() -> parse("void fn() { int inner; } inner;"));
    }
}
