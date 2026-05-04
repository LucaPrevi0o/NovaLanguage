package src.test.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import src.lexer.Lexer;
import src.parser.Parser;
import src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import src.token.TypeRegistry;
import static org.junit.jupiter.api.Assertions.*;

/// Test suite for the Parser class
public class ParserTest {

    @BeforeEach
    void setUp() {

        // Reset TypeRegistry before each test
        TypeRegistry.reset();
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
        var funcDecl = (src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement) func;
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
    void testUndefinedVariableThrows() {

        var source = "unknownVar + 1;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertThrows(Exception.class, parser::parse, "Should throw ParseException for undefined variable");
    }

    @Test
    void testDuplicateSymbolThrows() {

        var source = "int x; int x;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertThrows(RuntimeException.class, parser::parse, "Should throw RuntimeException for duplicate symbol");
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
        assertThrows(Exception.class, parser2::parse, "Should throw ParseException - class from first parse is not available");
    }
}


