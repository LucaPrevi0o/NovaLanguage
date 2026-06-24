package parser;

import org.junit.jupiter.api.Test;
import lexer.Lexer;
import lexer.token.family.Delimiter;
import lexer.token.family.PrimitiveType;
import parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.ExpressionStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import error.diagnostic.ParseErrorsException;
import semantic.analysis.NameResolver;
import static org.junit.jupiter.api.Assertions.*;

/// Test suite for the Parser class
public class ParserTest {

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

        // Verify that the ClassDeclarationStatement in the AST is fully populated,
        // not the empty type placeholder used while parsing members.
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

        assertSame(cls, ast.getFirst(), "Parsed class node should be the populated AST object");
    }

    @Test
    void testUndefinedVariableParsesSyntactically() {

        var source = "unknownVar + 1;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse, "Unresolved identifiers should be handled by semantic analysis");
    }

    @Test
    void testDuplicateSymbolParsesSyntactically() {

        var source = "int x; int x;";
        var lexer = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ast = assertDoesNotThrow(parser::parse, "Duplicate declarations should be handled by semantic analysis");
        assertEquals(2, ast.size());
    }

    @Test
    void testTypeRegistryResetBetweenParses() {

        // First parse defines a class
        var source1 = "public class First { }";
        var lexer1 = new Lexer(source1);
        var tokens1 = lexer1.tokenize();
        var parser1 = new Parser(tokens1);
        parser1.parse();

        // Second parse should start fresh: First parses as a type name, then semantic resolution rejects it.
        var source2 = "int x; First y;";
        var lexer2 = new Lexer(source2);
        var tokens2 = lexer2.tokenize();
        var parser2 = new Parser(tokens2);
        var ast2 = assertDoesNotThrow(parser2::parse);

        var diagnostics = new NameResolver().resolve(ast2);
        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.getFirst().getMessage().contains("Undefined type 'First'"));
    }

    // ─── Error recovery tests ──────────────────────────────────────────────────

    @Test
    void testMultipleErrorsReported() {

        // Two separate syntax errors on different top-level statements.
        // The parser should report both errors rather than stopping at the first.
        var source = "1 + ; 2 + ;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse,
                "Should throw ParseErrorsException with multiple errors");
        assertEquals(2, ex.getErrors().size(), "Both syntax errors should be reported");
    }

    @Test
    void testErrorMessageContainsLocation() {

        var source = "1 + ;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertFalse(ex.getErrors().isEmpty());
        // The error message should contain line/column info
        var msg = ex.getErrors().getFirst().getMessage();
        assertTrue(msg.contains("line"), "Error message should contain 'line'");
    }

    @Test
    void testIdentifierExpressionStoresIdentifierName() {

        var source = "badVar;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ast = assertDoesNotThrow(parser::parse);
        var statement = assertInstanceOf(ExpressionStatement.class, ast.getFirst());
        var expression = assertInstanceOf(IdentifierLiteralExpression.class, statement.getExpression());

        assertEquals("badVar", expression.getName());
    }

    @Test
    void testValidCodeAfterErrorStillParsed() {

        // The second statement is valid — its AST node should still be produced even though
        // the first statement contains an error.  The parse() method throws, but we verify the
        // error count is exactly 1 (not 2), showing the second statement didn't add another error.
        var source = "1 + ; int y;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(1, ex.getErrors().size(), "Only the first (invalid) statement should produce an error");

        var parsed = parser.getParsedStatements();
        assertEquals(1, parsed.size(), "The valid declaration after the error should still be retained");
        assertInstanceOf(VariableDeclarationStatement.class, parsed.getFirst());
    }

    @Test
    void testBlockRecoveryKeepsValidStatementsAfterError() {

        var source = "void f() { bad + ; int y; } int z;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(1, ex.getErrors().size(), "Only the invalid block statement should produce an error");

        var parsed = parser.getParsedStatements();
        assertEquals(2, parsed.size(), "Top-level parsing should continue after the recovered function body");

        var fn = assertInstanceOf(FunctionDeclarationStatement.class, parsed.getFirst());
        var body = assertInstanceOf(BlockStatement.class, fn.getBody());
        assertEquals(1, body.getStatements().length, "The valid declaration after the error should remain in the function body");
        assertInstanceOf(VariableDeclarationStatement.class, parsed.get(1));
    }

    @Test
    void testBlockRecoveryHandlesUnknownTokenBeforeStatement() {

        var source = "void f() { int before; @ int after; } int top;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(1, ex.getErrors().size(), "Only the unknown token inside the block should produce an error");

        var parsed = parser.getParsedStatements();
        assertEquals(2, parsed.size(), "Top-level parsing should continue after the recovered function body");

        var fn = assertInstanceOf(FunctionDeclarationStatement.class, parsed.getFirst());
        var body = assertInstanceOf(BlockStatement.class, fn.getBody());
        assertEquals(2, body.getStatements().length, "Valid block statements around the unknown token should be retained");
        assertInstanceOf(VariableDeclarationStatement.class, body.getStatements()[0]);
        assertInstanceOf(VariableDeclarationStatement.class, body.getStatements()[1]);
        assertInstanceOf(VariableDeclarationStatement.class, parsed.get(1));
    }

    @Test
    void testUnknownTokenReportsUnrecognizedTokenError() {

        var source = "@; int y;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(1, ex.getErrors().size(), "Only the unknown token should produce an error");

        var msg = ex.getErrors().getFirst().getMessage();
        assertTrue(msg.contains("Unrecognized token"), "Unknown token should use the unrecognized-token diagnostic");
        assertTrue(msg.contains("@"), "Unknown token diagnostic should include the raw lexeme");

        assertEquals(1, ex.getDiagnostics().size(), "Aggregate exception should expose structured diagnostics");
        assertEquals(1, parser.getDiagnostics().size(), "Parser should retain diagnostics from the run");
        assertEquals("@", parser.getDiagnostics().getFirst().getLexeme());
    }

    @Test
    void testMissingRequiredTokenDiagnosticStoresExpectedAndActualTokens() {

        var source = "int x int y;";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);

        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(1, ex.getDiagnostics().size());

        var diagnostic = ex.getDiagnostics().getFirst();
        assertEquals(Delimiter.SEMICOLON, diagnostic.getExpectedToken());
        assertEquals(PrimitiveType.INT, diagnostic.getActualToken());
    }

    // ─── Builtin-like call syntax ─────────────────────────────────────────────

    @Test
    void testBuiltinLikeCallsParseSyntactically() {

        var source = "print(\"hello\"); println(\"world\");";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testBuiltinLikeCallAssignmentParsesSyntactically() {

        var source = "string s; s = readLine();";
        var lexer  = new Lexer(source);
        var tokens = lexer.tokenize();
        var parser = new Parser(tokens);
        assertDoesNotThrow(parser::parse);
    }
}
