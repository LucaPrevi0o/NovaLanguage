package semantic.type;

import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for resolving parsed type syntax into semantic type symbols.
public class SemanticTypeResolverTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private SemanticScope scope(List<StatementNode> statements) {
        return new SemanticScopeBuilder().build(statements);
    }

    @Test
    void testResolvesPrimitiveArrayTypeSymbolFromDeclaredTypeSyntax() {

        var ast = parse("int[3][] values;");
        var rootScope = scope(ast);
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var resolution = new TypeResolver().resolve(declaration.getDeclaredType(), declaration, rootScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var arrayType = assertInstanceOf(ArrayTypeSymbol.class, resolution.type());
        assertEquals(2, arrayType.getDimensions());
        assertInstanceOf(PrimitiveTypeSymbol.class, arrayType.getElementType());
        assertEquals("int", arrayType.getElementType().getName());
    }

    @Test
    void testResolvesClassTypeSymbolFromForwardDeclaration() {

        var ast = parse("Later value; public class Later { }");
        var rootScope = scope(ast);
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var resolution = new TypeResolver().resolve(declaration.getDeclaredType(), declaration, rootScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var classType = assertInstanceOf(ClassTypeSymbol.class, resolution.type());
        assertEquals("Later", classType.getName());
        assertNotNull(classType.declaration());
    }

    @Test
    void testReportsUnknownTypeSymbolForMissingClass() {

        var ast = parse("Missing value;");
        var rootScope = scope(ast);
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var resolution = new TypeResolver().resolve(declaration.getDeclaredType(), declaration, rootScope);

        assertInstanceOf(UnknownTypeSymbol.class, resolution.type());
        assertEquals(1, resolution.diagnostics().size());
        assertTrue(resolution.diagnostics().getFirst().message().contains("Undefined type 'Missing'"));
    }

    @Test
    void testResolvesGenericParameterSymbolFromClassScopeField() {

        var ast = parse("public class Box[T] { public T value; }");
        var rootScope = scope(ast);
        var classDeclaration = assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
        var classScope = rootScope.childOwnedBy(classDeclaration).orElseThrow();
        var field = classDeclaration.getFields()[0];

        var resolution = new TypeResolver().resolve(field.getDeclaredType(), field, classScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var genericType = assertInstanceOf(GenericParameterSymbol.class, resolution.type());
        assertEquals("T", genericType.getName());
    }
}
