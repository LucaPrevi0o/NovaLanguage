package parser;

import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.expression.AssignmentExpression;
import parser.ast.nodes.expression.BinaryExpression;
import parser.ast.nodes.expression.ObjectCreationExpression;
import parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.ExpressionStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.type.NamedTypeSyntax;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Regression tests for the parser/semantic boundary.
///
/// These sources are syntactically valid but semantically invalid. The parser should preserve
/// their AST shape and leave meaning diagnostics to semantic passes.
public class ParserSemanticBoundaryTest {

    private Parser parser(String source) {
        return new Parser(new Lexer(source).tokenize());
    }

    private List<StatementNode> parseWithoutParserDiagnostics(String source) {

        var parser = parser(source);
        var ast = assertDoesNotThrow(parser::parse);
        assertTrue(parser.getDiagnostics().isEmpty(), "Syntactically valid source should not produce parser diagnostics");
        return ast;
    }

    @Test
    void testParserPreservesSemanticallyInvalidExpressionAst() {

        var ast = parseWithoutParserDiagnostics("""
            unknownVar + new MissingClass();
            1 = 2;
            """);

        assertEquals(2, ast.size());

        var unresolvedStatement = assertInstanceOf(ExpressionStatement.class, ast.get(0));
        var unresolvedExpression = assertInstanceOf(BinaryExpression.class, unresolvedStatement.getExpression());
        var identifier = assertInstanceOf(IdentifierLiteralExpression.class, unresolvedExpression.getLeft());
        var construction = assertInstanceOf(ObjectCreationExpression.class, unresolvedExpression.getRight());
        assertEquals("unknownVar", identifier.getName());
        assertEquals("MissingClass", construction.getClassName());

        var invalidAssignmentStatement = assertInstanceOf(ExpressionStatement.class, ast.get(1));
        var invalidAssignment = assertInstanceOf(AssignmentExpression.class, invalidAssignmentStatement.getExpression());
        assertInstanceOf(NumberLiteralExpression.class, invalidAssignment.getTarget());
        assertInstanceOf(NumberLiteralExpression.class, invalidAssignment.getValue());
    }

    @Test
    void testParserPreservesSemanticallyInvalidDeclarationAst() {

        var ast = parseWithoutParserDiagnostics("""
            int duplicate;
            int duplicate;
            MissingType value;
            public class Child :: MissingBase {
              public MissingType field;
            }
            """);

        assertEquals(4, ast.size());

        var firstDuplicate = assertInstanceOf(VariableDeclarationStatement.class, ast.get(0));
        var secondDuplicate = assertInstanceOf(VariableDeclarationStatement.class, ast.get(1));
        assertEquals("duplicate", firstDuplicate.getName());
        assertEquals("duplicate", secondDuplicate.getName());

        var missingTypedVariable = assertInstanceOf(VariableDeclarationStatement.class, ast.get(2));
        var missingTypeSyntax = assertInstanceOf(NamedTypeSyntax.class, missingTypedVariable.getDeclaredTypeSyntax());
        assertEquals("MissingType", missingTypeSyntax.getName());
        assertFalse(missingTypeSyntax.isPrimitive());

        var child = assertInstanceOf(ClassDeclarationStatement.class, ast.get(3));
        var superclassSyntax = assertInstanceOf(NamedTypeSyntax.class, child.getSuperClassSyntaxes()[0]);
        assertEquals("MissingBase", superclassSyntax.getName());
        assertFalse(superclassSyntax.isPrimitive());

        var fieldTypeSyntax = assertInstanceOf(NamedTypeSyntax.class, child.getFields()[0].getDeclaredTypeSyntax());
        assertEquals("MissingType", fieldTypeSyntax.getName());
        assertFalse(fieldTypeSyntax.isPrimitive());
    }
}
