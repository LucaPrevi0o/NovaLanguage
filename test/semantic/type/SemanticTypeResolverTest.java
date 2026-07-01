package semantic.type;

import lexer.Lexer;
import lexer.token.ReturnType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.GenericTypeSyntax;
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

    private VariableDeclarationStatement variable(List<StatementNode> statements, String name) {

        for (var statement : statements)
            if (statement instanceof VariableDeclarationStatement declaration && declaration.getName().equals(name))
                return declaration;
        fail("Expected variable declaration named " + name);
        return null;
    }

    private ClassDeclarationStatement classDeclaration(List<StatementNode> statements, String name) {

        for (var statement : statements)
            if (statement instanceof ClassDeclarationStatement declaration && declaration.getName().equals(name))
                return declaration;
        fail("Expected class declaration named " + name);
        return null;
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
    void testBuiltinValueTypesResolveToSemanticDomainsFromSourceSyntax() {

        assertBuiltinValueDomain("byte", ValueTypeDomain.INTEGRAL, true);
        assertBuiltinValueDomain("int", ValueTypeDomain.INTEGRAL, true);
        assertBuiltinValueDomain("long", ValueTypeDomain.INTEGRAL, true);
        assertBuiltinValueDomain("float", ValueTypeDomain.FLOATING_POINT, true);
        assertBuiltinValueDomain("double", ValueTypeDomain.FLOATING_POINT, true);
        assertBuiltinValueDomain("bool", ValueTypeDomain.BOOLEAN, false);
        assertBuiltinValueDomain("char", ValueTypeDomain.CHARACTER, false);
        assertBuiltinValueDomain("string", ValueTypeDomain.TEXT, false);
        assertBuiltinValueDomain("void", ValueTypeDomain.VOID, false);
    }

    @Test
    void testSourceTypeSyntaxResolvesEverySemanticTypeCategory() {

        var ast = parse("""
            int count;
            public class Known { }
            Known object;
            Known[] objects;
            Missing missing;
            public class Box[T] { public T value; }
            """);
        var rootScope = scope(ast);
        var resolver = new TypeResolver();

        var count = variable(ast, "count");
        var valueType = resolver.resolve(count.getDeclaredTypeSyntax(), count.getDeclaredType(), count, rootScope);
        assertTrue(valueType.diagnostics().isEmpty());
        assertEquals(TypeKind.VALUE, valueType.type().getKind());

        var object = variable(ast, "object");
        var classType = resolver.resolve(object.getDeclaredTypeSyntax(), object.getDeclaredType(), object, rootScope);
        assertTrue(classType.diagnostics().isEmpty());
        assertEquals(TypeKind.CLASS, classType.type().getKind());

        var objects = variable(ast, "objects");
        var arrayType = resolver.resolve(objects.getDeclaredTypeSyntax(), objects.getDeclaredType(), objects, rootScope);
        assertTrue(arrayType.diagnostics().isEmpty());
        var arraySymbol = assertInstanceOf(ArrayTypeSymbol.class, arrayType.type());
        assertEquals(TypeKind.ARRAY, arraySymbol.getKind());
        assertEquals(TypeKind.CLASS, arraySymbol.elementType().getKind());

        var missing = variable(ast, "missing");
        var unknownType = resolver.resolve(missing.getDeclaredTypeSyntax(), missing.getDeclaredType(), missing, rootScope);
        assertEquals(TypeKind.UNKNOWN, unknownType.type().getKind());
        assertInstanceOf(UnknownTypeSymbol.class, unknownType.type());
        assertEquals(1, unknownType.diagnostics().size());

        var box = classDeclaration(ast, "Box");
        var boxScope = rootScope.childOwnedBy(box).orElseThrow();
        var field = box.getFields()[0];
        var genericType = resolver.resolve(field.getDeclaredTypeSyntax(), field.getDeclaredType(), field, boxScope);
        assertTrue(genericType.diagnostics().isEmpty());
        assertEquals(TypeKind.GENERIC_PARAMETER, genericType.type().getKind());
    }

    @Test
    void testAstDeclarationsExposeParsedTypeSyntaxDirectly() {

        var ast = parse("int[3][] values;");
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var arraySyntax = assertInstanceOf(ArrayTypeSyntax.class, declaration.getDeclaredTypeSyntax());
        assertEquals("int", arraySyntax.getName());
        assertEquals(2, arraySyntax.getSizes().length);
        assertInstanceOf(NamedTypeSyntax.class, arraySyntax.getElementType());
    }

    @Test
    void testClassHeadersExposeGenericAndSuperclassSyntaxDirectly() {

        var ast = parse("""
            public class Base { }
            public class Box[T] :: Base { }
            """);
        var classDeclaration = assertInstanceOf(ClassDeclarationStatement.class, ast.get(1));

        var genericSyntax = assertInstanceOf(GenericTypeSyntax.class, classDeclaration.getGenericClassParameterSyntaxes()[0]);
        assertEquals("T", genericSyntax.getName());

        assertEquals(1, classDeclaration.getSuperClassSyntaxes().length);
        var superClassSyntax = assertInstanceOf(NamedTypeSyntax.class, classDeclaration.getSuperClassSyntaxes()[0]);
        assertEquals("Base", superClassSyntax.getName());
    }

    @Test
    void testResolvesValueArrayTypeSymbolFromDeclaredTypeSyntax() {

        var ast = parse("int[3][] values;");
        var rootScope = scope(ast);
        var declaration = assertInstanceOf(VariableDeclarationStatement.class, ast.getFirst());

        var resolution = new TypeResolver().resolve(declaration.getDeclaredTypeSyntax(), declaration.getDeclaredType(), declaration, rootScope);

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

        var resolution = new TypeResolver().resolve(declaration.getDeclaredTypeSyntax(), declaration.getDeclaredType(), declaration, rootScope);

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

        var resolution = new TypeResolver().resolve(declaration.getDeclaredTypeSyntax(), declaration.getDeclaredType(), declaration, rootScope);

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

        var resolution = new TypeResolver().resolve(field.getDeclaredTypeSyntax(), field.getDeclaredType(), field, classScope);

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

        var resolution = new TypeResolver().resolve(field.getDeclaredTypeSyntax(), field.getDeclaredType(), field, classScope);

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

    @Test
    void testSyntaxlessReturnTypeFallbacksResolveThroughTypeSyntaxBridge() {

        var ast = parse("public class Later { }");
        var rootScope = scope(ast);
        var resolver = new TypeResolver();

        var valueResolution = resolver.resolve(new ReturnType(PrimitiveType.INT), ast.getFirst(), rootScope);
        assertTrue(valueResolution.diagnostics().isEmpty());
        var valueType = assertInstanceOf(ValueTypeSymbol.class, valueResolution.type());
        assertEquals("int", valueType.getName());

        var classResolution = resolver.resolve(new ReturnType(new NonPrimitiveType("Later")), ast.getFirst(), rootScope);
        assertTrue(classResolution.diagnostics().isEmpty());
        var classType = assertInstanceOf(ClassTypeSymbol.class, classResolution.type());
        assertEquals("Later", classType.getName());

        var arrayAdapter = new ReturnType(PrimitiveType.INT, new ExpressionNode[] { null }, null, null);
        var arrayResolution = resolver.resolve(arrayAdapter, ast.getFirst(), rootScope);
        assertTrue(arrayResolution.diagnostics().isEmpty());
        var arrayType = assertInstanceOf(ArrayTypeSymbol.class, arrayResolution.type());
        assertEquals(1, arrayType.getDimensions());
        var elementType = assertInstanceOf(ValueTypeSymbol.class, arrayType.elementType());
        assertEquals("int", elementType.getName());
    }

    private void assertBuiltinValueDomain(String name, ValueTypeDomain expectedDomain, boolean expectedNumeric) {

        var rootScope = scope(List.of());
        var syntax = new NamedTypeSyntax(1, 1, name, true);
        var resolution = new TypeResolver().resolve(syntax, null, rootScope);

        assertTrue(resolution.diagnostics().isEmpty());
        var valueType = assertInstanceOf(ValueTypeSymbol.class, resolution.type());
        assertEquals(TypeKind.VALUE, valueType.getKind());
        assertEquals(expectedDomain, valueType.domain());
        assertEquals(expectedNumeric, valueType.isNumeric());
    }
}
