import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Lexer;
import lexer.token.ReturnType;
import lexer.token.family.AccessModifier;
import lexer.token.family.PrimitiveType;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import semantic.DuplicateDeclarationValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic duplicate declaration validation.
public class SemanticDuplicateDeclarationValidatorTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private List<Diagnostic> validate(List<StatementNode> ast) {
        return new DuplicateDeclarationValidator().validate(ast);
    }

    @Test
    void testReportsDuplicateVariablesInSameScope() {

        var intType = new ReturnType(PrimitiveType.INT);
        var ast = List.<StatementNode>of(
            new VariableDeclarationStatement(1, 1, intType, "x", null),
            new VariableDeclarationStatement(2, 1, intType, "x", null)
        );

        var diagnostics = validate(ast);

        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticPhase.SEMANTIC, diagnostics.getFirst().getPhase());
        assertEquals(2, diagnostics.getFirst().getLine());
        assertTrue(diagnostics.getFirst().getMessage().contains("Duplicate declaration 'x'"));
    }

    @Test
    void testAllowsNestedShadowing() {

        var diagnostics = validate(parse("int x; { int x; }"));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void testReportsDuplicateFieldsInClassScope() {

        var intType = new ReturnType(PrimitiveType.INT);
        var ast = List.<StatementNode>of(new ClassDeclarationStatement(
            1,
            1,
            "Box",
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[] {
                new ClassFieldDeclaration(2, 1, intType, "value", null, AccessModifier.PRIVATE),
                new ClassFieldDeclaration(3, 1, intType, "value", null, AccessModifier.PRIVATE)
            },
            new ReturnType[0],
            null,
            new ClassDeclarationStatement[0],
            AccessModifier.PUBLIC,
            new ClassConstructorDeclaration[0]
        ));

        var diagnostics = validate(ast);

        assertEquals(1, diagnostics.size());
        assertEquals(3, diagnostics.getFirst().getLine());
        assertTrue(diagnostics.getFirst().getMessage().contains("Duplicate declaration 'value'"));
    }

    @Test
    void testAllowsMultipleConstructorsUntilSignatureValidationExists() {

        var intType = new ReturnType(PrimitiveType.INT);
        var ast = List.<StatementNode>of(new ClassDeclarationStatement(
            1,
            1,
            "Box",
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[0],
            new ReturnType[0],
            null,
            new ClassDeclarationStatement[0],
            AccessModifier.PUBLIC,
            new ClassConstructorDeclaration[] {
                new ClassConstructorDeclaration(2, 1, new FunctionParameter[0], null, AccessModifier.PUBLIC),
                new ClassConstructorDeclaration(
                    3,
                    1,
                    new FunctionParameter[] { new FunctionParameter(3, 9, "value", intType) },
                    null,
                    AccessModifier.PUBLIC
                )
            }
        ));

        var diagnostics = validate(ast);

        assertTrue(diagnostics.isEmpty());
    }
}
