package semantic.type;

import lexer.Lexer;
import lexer.token.ReturnType;
import lexer.token.family.NonPrimitiveType;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.type.NamedTypeSyntax;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;
import semantic.type.symbol.*;

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
    void testBuiltinValueTypesExposeSemanticDomains() {

        var boolType = ValueTypeSymbol.builtin("bool");
        var doubleType = ValueTypeSymbol.builtin("double");
        var stringType = ValueTypeSymbol.builtin("string");
        var voidType = ValueTypeSymbol.builtin("void");

        assertEquals(TypeKind.VALUE, boolType.getKind());
        assertEquals(ValueTypeDomain.BOOLEAN, boolType.domain());
        assertFalse(boolType.isNumeric());

        assertEquals(ValueTypeDomain.FLOATING_POINT, doubleType.domain());
        assertTrue(doubleType.isNumeric());

        assertEquals(ValueTypeDomain.TEXT, stringType.domain());
        assertEquals(ValueTypeDomain.VOID, voidType.domain());
    }

    @Test
    void testResolvesValueArrayTypeSymbolFromDeclaredTypeSyntax() {

        var ast = parse("int[3][] values;");
        var rootScope = scope(ast);
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var resolution = new TypeResolver().resolve(declaration.getDeclaredType(), declaration, rootScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var arrayType = assertInstanceOf(ArrayTypeSymbol.class, resolution.type());
        assertEquals(TypeKind.ARRAY, arrayType.getKind());
        assertEquals(2, arrayType.getDimensions());
        var elementType = assertInstanceOf(ValueTypeSymbol.class, arrayType.elementType());
        assertEquals(TypeKind.VALUE, elementType.getKind());
        assertEquals(ValueTypeDomain.INTEGRAL, elementType.domain());
        assertTrue(elementType.isNumeric());
        assertEquals("int", elementType.getName());
    }

    @Test
    void testResolvesClassTypeSymbolFromForwardDeclaration() {

        var ast = parse("Later value; public class Later { }");
        var rootScope = scope(ast);
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var resolution = new TypeResolver().resolve(declaration.getDeclaredType(), declaration, rootScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var classType = assertInstanceOf(ClassTypeSymbol.class, resolution.type());
        assertEquals(TypeKind.CLASS, classType.getKind());
        assertFalse(classType.isValueType());
        assertTrue(classType.isClassType());
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
        assertEquals(TypeKind.UNKNOWN, resolution.type().getKind());
        assertFalse(resolution.type().isResolved());
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
        assertEquals(TypeKind.GENERIC_PARAMETER, genericType.getKind());
        assertEquals("T", genericType.getName());
    }

    @Test
    void testResolvesGenericArrayElementFromClassScopeTypeSyntax() {

        var ast = parse("public class Box[T] { public T[] values; }");
        var rootScope = scope(ast);
        var classDeclaration = assertInstanceOf(ClassDeclarationStatement.class, ast.getFirst());
        var classScope = rootScope.childOwnedBy(classDeclaration).orElseThrow();
        var field = classDeclaration.getFields()[0];

        var resolution = new TypeResolver().resolve(field.getDeclaredType(), field, classScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var arrayType = assertInstanceOf(ArrayTypeSymbol.class, resolution.type());
        var elementType = assertInstanceOf(GenericParameterSymbol.class, arrayType.elementType());
        assertEquals("T", elementType.getName());
    }

    @Test
    void testReturnTypeAdapterResolvesSourceSyntaxBeforeLegacyTokenMetadata() {

        var ast = parse("public class Actual { }");
        var rootScope = scope(ast);
        var syntax = new NamedTypeSyntax(1, 1, "Actual", false);
        var adapter = new ReturnType(
            new NonPrimitiveType("Stale"),
            new ExpressionNode[0],
            null,
            null,
            syntax
        );

        var resolution = new TypeResolver().resolve(adapter, ast.getFirst(), rootScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var classType = assertInstanceOf(ClassTypeSymbol.class, resolution.type());
        assertEquals("Actual", classType.getName());
    }
}
