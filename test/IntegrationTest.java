import org.junit.jupiter.api.Test;
import lexer.Lexer;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.parser.util.ParseErrorsException;
import token.TypeRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Integration tests that parse representative multi-construct programs.
/// These tests combine multiple language features in realistic scenarios
/// to verify that the parser handles complex, real-world-like programs correctly.
public class IntegrationTest {

    private List<StatementNode> parse(String source) {

        TypeRegistry.reset();
        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    // ─── Multi-class programs ─────────────────────────────────────────────────

    @Test
    void testTwoClasses() {

        var ast = parse(
            "public class Node { " +
            "  public int value; " +
            "  public Node(int v) { value = v; } " +
            "} " +
            "public class LinkedList { " +
            "  private Node head; " +
            "}"
        );
        assertEquals(2, ast.size());
        assertInstanceOf(ClassDeclarationStatement.class, ast.get(0));
        assertInstanceOf(ClassDeclarationStatement.class, ast.get(1));
    }

    @Test
    void testClassWithMultipleMethodsAndFields() {

        var ast = parse(
            "public class Rectangle { " +
            "  public int width; " +
            "  public int height; " +
            "  public Rectangle(int w, int h) { width = w; height = h; } " +
            "  public int area() { return width * height; } " +
            "  public int perimeter() { return 2 * (width + height); } " +
            "}"
        );
        var rect = (ClassDeclarationStatement) ast.getFirst();
        assertEquals(2, rect.getFields().length);
        assertEquals(2, rect.getMethods().length);
        assertEquals(1, rect.getConstructors().length);
    }

    // ─── Nested scopes ────────────────────────────────────────────────────────

    @Test
    void testNestedScopesInFunction() {

        assertDoesNotThrow(() ->
            parse("void outerFn() { " +
                  "  int x; " +
                  "  { " +
                  "    int y; " +
                  "    { " +
                  "      int z; " +
                  "      x = z; " +
                  "    } " +
                  "  } " +
                  "}")
        );
    }

    @Test
    void testForLoopWithConditionalInBody() {

        assertDoesNotThrow(() ->
            parse("int total; " +
                  "for (int i = 0; i < 10; i++) { " +
                  "  if (i > 5) { " +
                  "    total += i; " +
                  "  } " +
                  "}")
        );
    }

    @Test
    void testWhileLoopWithSwitch() {

        assertDoesNotThrow(() -> {
            TypeRegistry.reset();
            var tokens = new Lexer(
                "int state; " +
                "while (state < 3) { " +
                "  switch (state) { " +
                "    case 0 -> state = 1; " +
                "    case 1 -> state = 2; " +
                "    default -> state = 3; " +
                "  } " +
                "}"
            ).tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Complex expressions ──────────────────────────────────────────────────

    @Test
    void testTernaryInVariableInitializer() {

        assertDoesNotThrow(() -> {
            TypeRegistry.reset();
            var tokens = new Lexer("int x; int y; int max = x > y ? x : y;").tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testPostfixInForLoop() {

        assertDoesNotThrow(() ->
            parse("for (int i = 0; i < 5; i++) { print(\"x\"); }")
        );
    }

    @Test
    void testComplexBooleanExpression() {

        assertDoesNotThrow(() ->
            parse("boolean a; boolean b; boolean c; boolean r; " +
                  "r = (a && b) || (!c && a);")
        );
    }

    // ─── Stdlib integration ───────────────────────────────────────────────────

    @Test
    void testStdlibPrintInLoop() {

        assertDoesNotThrow(() ->
            parse("for (int i = 0; i < 3; i++) { print(\"iteration\"); }")
        );
    }

    @Test
    void testStdlibReadAndParse() {

        assertDoesNotThrow(() -> {
            TypeRegistry.reset();
            var tokens = new Lexer("string s; s = readLine(); int n; n = parseInt(s);").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Generic class instantiation ──────────────────────────────────────────

    @Test
    void testGenericClassCanBeInstantiated() {

        assertDoesNotThrow(() -> {
            TypeRegistry.reset();
            var tokens = new Lexer("public class Stack[T] { } Stack s; s = new Stack();").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Error recovery across multiple top-level errors ─────────────────────

    @Test
    void testMultipleTopLevelErrors() {

        var tokens = new Lexer("badA + 1; badB + 2; badC + 3;").tokenize();
        var parser = new Parser(tokens);
        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(3, ex.getErrors().size(), "All three errors should be reported");
    }

    @Test
    void testValidDeclarationAfterError() {

        // First statement has an error; second is valid.
        // Error count should be 1 (only the bad statement).
        var tokens = new Lexer("undeclaredVar; int x;").tokenize();
        var parser = new Parser(tokens);
        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(1, ex.getErrors().size(), "Only 1 error expected — the second statement is valid");
    }

    // ─── Inheritance with method calls ────────────────────────────────────────

    @Test
    void testInheritanceAndMethodCall() {

        assertDoesNotThrow(() -> {
            TypeRegistry.reset();
            var tokens = new Lexer(
                "public class Animal { " +
                "  public void speak() { print(\"...\"); } " +
                "} " +
                "public class Dog :: Animal { " +
                "  public void speak() { print(\"Woof\"); } " +
                "}"
            ).tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Function with complex body ───────────────────────────────────────────

    @Test
    void testFunctionWithComplexBody() {

        assertDoesNotThrow(() ->
            parse("int fibonacci(int n) { " +
                  "  if (n <= 1) return n; " +
                  "  int a = 0; " +
                  "  int b = 1; " +
                  "  int temp; " +
                  "  for (int i = 2; i <= n; i++) { " +
                  "    temp = a + b; " +
                  "    a = b; " +
                  "    b = temp; " +
                  "  } " +
                  "  return b; " +
                  "}")
        );
    }

    @Test
    void testClassWithInnerClassAndMethod() {

        assertDoesNotThrow(() -> {
            TypeRegistry.reset();
            var tokens = new Lexer(
                "public class Outer { " +
                "  public class Inner { " +
                "    public int value; " +
                "  } " +
                "  public int compute() { return 42; } " +
                "}"
            ).tokenize();
            new Parser(tokens).parse();
        });
    }
}
