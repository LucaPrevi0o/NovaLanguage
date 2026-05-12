import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import lexer.Lexer;
import parser.Parser;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import error.ErrorCollector;
import static org.junit.jupiter.api.Assertions.*;

/// Test suite for the Parser class
public class ParserTest {

    @BeforeEach
    void clearErrors() {

        ErrorCollector.clear();
    }

    @Test
    void testMinimalVariableDeclaration() {

        var source = "int x;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testFunctionDeclarationWithBody() {

        var source = "int foo() { return 42; }";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        var ast = assertDoesNotThrow(parser::parse);
        var func = ast.getFirst();

        // Check that function has a non-null body
        assertInstanceOf(FunctionDeclarationStatement.class, func);
        var funcDecl = (FunctionDeclarationStatement) func;
        assertNotNull(funcDecl.getBody());
    }

    @Test
    void testClassDeclarationWithMembers() {

        var source = "public class MyClass { int field; int method() { return 0; } }";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        var ast = assertDoesNotThrow(parser::parse);
        assertEquals(1, ast.size());
    }

    @Test
    void testClassDeclarationMembersPopulated() {

        // Verify that the ClassDeclarationStatement in the AST (and the one registered
        // in the symbol table) is the same object with all members populated, not the empty placeholder.
        var source = "public class Counter { " +
                     "  private int count; " +
                     "  public Counter() { count = 0; } " +
                     "  public int token() { return count; } " +
                     "}";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        var ast    = assertDoesNotThrow(parser::parse);

        assertEquals(1, ast.size());
        assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
        var cls = (ClassDeclarationStatement) ast.getFirst();

        assertEquals("Counter", cls.getName());
        assertEquals(1, cls.getFields().length,       "Expected 1 field");
        assertEquals(1, cls.getMethods().length,      "Expected 1 method");
        assertEquals(1, cls.getConstructors().length, "Expected 1 constructor");

        // The symbol in the symbol table must be the same populated object (not the placeholder)
        var sym = parser.getSymbolTable().lookup("Counter");
        assertInstanceOf(ClassDeclarationStatement.class, sym);
        var registered = (ClassDeclarationStatement) sym;
        assertEquals(1, registered.getFields().length,  "Registered symbol should have 1 field");
        assertEquals(1, registered.getMethods().length, "Registered symbol should have 1 method");
        assertSame(cls, registered, "AST node and registered symbol must be the same object");
    }

    @Test
    void testUndefinedVariableThrows() {

        var source = "unknownVar + 1;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
        assertFalse(ErrorCollector.hasErrors(), "Undefined variable is not a syntax error and should not be collected here");
    }

    @Test
    void testDuplicateSymbolThrows() {

        var source = "int x; int x;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
        assertTrue(ErrorCollector.hasErrors(), "Duplicate symbol should be collected as parse error");
    }

    @Test
    void testTypeRegistryResetBetweenParses() {

        // First parse defines a class
        var source1 = "public class First { }";
        var lexer1 = new Lexer(source1);
        var tokens1 = lexer1.tokenize();
        var parser1 = new Parser(tokens1);
        parser1.parse();

        // Second parse should start fresh (should fail if class First is still registered)
        var source2 = "int x; First y;"; // This should fail since First is not in scope of second parse
        var lexer2 = new Lexer(source2);
        var tokens2 = lexer2.tokenize();
        var parser2 = new Parser(tokens2);
        assertDoesNotThrow(parser2::parse);
        assertTrue(ErrorCollector.hasErrors(), "Using an unregistered type in a declaration should produce collected parse errors");
    }

    // ─── Error recovery tests ──────────────────────────────────────────────────

    @Test
    void testMultipleErrorsReported() {

        // Two malformed expressions on different top-level statements.
        // The parser should report multiple syntax errors rather than stopping at the first.
        var source = "1 + ; 2 + ;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        assertDoesNotThrow(parser::parse);
        assertTrue(ErrorCollector.getErrors().size() >= 2, "Both malformed statements should produce collected syntax errors");
    }

    @Test
    void testErrorMessageContainsLocation() {

        var source = "1 + ;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        assertDoesNotThrow(parser::parse);
        assertFalse(ErrorCollector.getErrors().isEmpty(), "At least one syntax error should be collected");
        // The error message should contain line/column info
        var msg = ErrorCollector.getErrors().getFirst().getMessage();
        assertTrue(msg.contains("line"), "Error message should contain 'line'");
    }

    @Test
    void testValidCodeAfterErrorStillParsed() {

        // The second statement is valid — it should still be parsed even if the first one has a syntax error.
        var source = "1 + ; int y;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        assertDoesNotThrow(parser::parse);
        assertFalse(ErrorCollector.getErrors().isEmpty(), "The first statement should produce at least one syntax error");
    }

    // ─── Stdlib tests ─────────────────────────────────────────────────────────

    @Test
    void testStdlibFunctionsAvailable() {

        // All stdlib functions should be callable without prior declaration
        var source = "print(\"hello\"); println(\"world\");";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testExtendedStdlibFunctionsAvailable() {

        // readLine, parseInt, parseFloat should be pre-registered as well
        var source = "string s; s = readLine();";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
    }
}

