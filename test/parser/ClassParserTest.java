package parser;

import org.junit.jupiter.api.Test;
import lexer.Lexer;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for class declarations, including methods, fields, constructors,
/// inner classes, generics, super-classes, and access modifiers.
public class ClassParserTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    // ─── Basic class ──────────────────────────────────────────────────────────

    @Test
    void testEmptyClass() {

        var ast = parse("public class Empty { }");
        assertEquals(1, ast.size());
        assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
    }

    @Test
    void testClassWithSingleField() {

        var ast = parse("public class Box { public int value; }");
        var cls = (ClassDeclarationStatement) ast.getFirst();
        assertEquals(1, cls.getFields().length);
        assertEquals("value", cls.getFields()[0].getName());
    }

    @Test
    void testClassWithSingleMethod() {

        var ast = parse("public class Adder { public int add(int a, int b) { return 0; } }");
        var cls = (ClassDeclarationStatement) ast.getFirst();
        assertEquals(1, cls.getMethods().length);
        assertEquals("add", cls.getMethods()[0].getName());
    }

    @Test
    void testClassWithConstructor() {

        var ast = parse("public class Foo { public Foo() { } }");
        var cls = (ClassDeclarationStatement) ast.getFirst();
        assertEquals(1, cls.getConstructors().length);
    }

    @Test
    void testClassWithMixedMembers() {

        var ast = parse(
            "public class Counter { " +
            "  private int count; " +
            "  public Counter() { count = 0; } " +
            "  public int token() { return count; } " +
            "}"
        );
        var cls = (ClassDeclarationStatement) ast.getFirst();
        assertEquals(1, cls.getFields().length);
        assertEquals(1, cls.getMethods().length);
        assertEquals(1, cls.getConstructors().length);
    }

    // ─── Access modifiers ─────────────────────────────────────────────────────

    @Test
    void testPrivateClass() {

        var ast = parse("private class Hidden { }");
        assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
    }

    @Test
    void testProtectedClass() {

        var ast = parse("protected class Semi { }");
        assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
    }

    @Test
    void testClassWithoutAccessModifierThrows() {

        var tokens = new Lexer("class Bare { }").tokenize();
        assertThrows(RuntimeException.class, () -> new Parser(tokens).parse());
    }

    @Test
    void testClassFieldAccessModifiers() {

        assertDoesNotThrow(() ->
            parse("public class Mods { " +
                  "  public int pub; " +
                  "  private int priv; " +
                  "  protected int prot; " +
                  "}"));
    }

    // ─── Class AST consistency ────────────────────────────────────────────────

    @Test
    void testClassReturnedInAst() {

        var ast = parse("public class MyClass { }");
        var cls = assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
        assertEquals("MyClass", cls.getName());
    }

    @Test
    void testClassAstNodeKeepsParsedMembers() {

        var ast = parse("public class Linked { private int val; public int token() { return val; } }");

        var cls = assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
        assertEquals("Linked", cls.getName());
        assertEquals(1, cls.getFields().length);
        assertEquals(1, cls.getMethods().length);
    }

    @Test
    void testParsedClassHasPopulatedMembers() {

        var ast = parse(
            "public class Populated { " +
            "  public int x; " +
            "  public int foo() { return x; } " +
            "}"
        );

        var cls = assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
        assertEquals(1, cls.getFields().length,  "class AST should have 1 field");
        assertEquals(1, cls.getMethods().length, "class AST should have 1 method");
    }

    // ─── Inheritance ──────────────────────────────────────────────────────────

    @Test
    void testClassWithSuperclass() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer(
                "public class Base { } " +
                "public class Child :: Base { }"
            ).tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testClassWithMultipleSuperclasses() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer(
                "public class A { } " +
                "public class B { } " +
                "public class C :: A, B { }"
            ).tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testClassWithUndefinedSuperclassParsesSyntactically() {

        var ast = parse("public class Orphan :: NoParent { }");
        var cls = assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());

        assertEquals(1, cls.getSuperClasses().length);
        assertEquals("NoParent", cls.getSuperClasses()[0].getTokenClass().token());
    }

    // ─── Generics ─────────────────────────────────────────────────────────────

    @Test
    void testGenericClass() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer("public class Container[T] { }").tokenize();
            new Parser(tokens).parse();
        });
    }

    // ─── Inner classes ────────────────────────────────────────────────────────

    @Test
    void testInnerClass() {

        assertDoesNotThrow(() -> {
            var tokens = new Lexer(
                "public class Outer { " +
                "  public class Inner { } " +
                "}"
            ).tokenize();
            new Parser(tokens).parse();
        });
    }

    @Test
    void testInnerClassPopulated() {

        var tokens = new Lexer(
            "public class Outer { " +
            "  public class Inner { } " +
            "}"
        ).tokenize();
        var ast = new Parser(tokens).parse();
        var outer = (ClassDeclarationStatement) ast.getFirst();
        assertEquals(1, outer.getInnerClasses().length, "Outer should have 1 inner class");
    }

    // ─── Constructor parameters ───────────────────────────────────────────────

    @Test
    void testConstructorWithParameters() {

        assertDoesNotThrow(() ->
            parse("public class Point { " +
                  "  private int x; " +
                  "  private int y; " +
                  "  public Point(int a, int b) { x = a; y = b; } " +
                  "}"));
    }

    // ─── Method recursive call ────────────────────────────────────────────────

    @Test
    void testMethodRecursiveCall() {

        assertDoesNotThrow(() ->
            parse("public class Fact { " +
                  "  public int factorial(int n) { " +
                  "    if (n == 0) return 1; " +
                  "    return factorial(n); " +  // recursive call
                  "  } " +
                  "}"));
    }

    // ─── Field initializer ────────────────────────────────────────────────────

    @Test
    void testFieldWithInitializer() {

        assertDoesNotThrow(() ->
            parse("public class Config { " +
                  "  public int maxSize = 100; " +
                  "}"));
    }

    // ─── Duplicate class members ──────────────────────────────────────────────

    @Test
    void testDuplicateFieldParsesSyntactically() {

        assertDoesNotThrow(() -> parse("public class Dup { public int x; public int x; }"));
    }
}
