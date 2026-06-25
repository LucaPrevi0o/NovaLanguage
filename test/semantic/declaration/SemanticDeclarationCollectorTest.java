package semantic.declaration;

import lexer.Lexer;
import lexer.token.ReturnType;
import lexer.token.family.PrimitiveType;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.NamedTypeSyntax;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the first semantic declaration-collection pass.
public class SemanticDeclarationCollectorTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private DeclarationCollection collect(String source) {
        return new DeclarationCollector().collect(parse(source));
    }

    @Test
    void testCollectsTopLevelDeclarations() {

        var collection = collect("int global; void main() { } public class Box { }");

        assertNames(collection.byKind(DeclarationKind.VARIABLE), "global");
        assertNames(collection.byKind(DeclarationKind.FUNCTION), "main");
        assertNames(collection.byKind(DeclarationKind.CLASS), "Box");
    }

    @Test
    void testCollectsNestedDeclarationsAndOwners() {

        var collection = collect("""
            public class Box {
                private int value;
                public Box(int value) { int constructed; }
                public int get(int fallback) { int local; return value; }
            }
            void main(int arg) { int local; { string nested; } }
            public class MyList { }
            MyList items;
            for (int elem : items) { int inside; }
            """);

        assertEquals(16, collection.all().size());
        assertNames(collection.byKind(DeclarationKind.CLASS), "Box", "MyList");
        assertNames(collection.byKind(DeclarationKind.FIELD), "value");
        assertNames(collection.byKind(DeclarationKind.CONSTRUCTOR), "Box");
        assertNames(collection.byKind(DeclarationKind.METHOD), "get");
        assertNames(collection.byKind(DeclarationKind.FUNCTION), "main");
        assertNames(collection.byKind(DeclarationKind.PARAMETER), "fallback", "value", "arg");
        assertNames(collection.byKind(DeclarationKind.VARIABLE), "local", "constructed", "local", "nested", "items", "inside");
        assertNames(collection.byKind(DeclarationKind.FOREACH_VARIABLE), "elem");

        var box = only(collection.byKind(DeclarationKind.CLASS), "Box");
        var field = only(collection.byKind(DeclarationKind.FIELD), "value");
        var constructor = collection.byKind(DeclarationKind.CONSTRUCTOR).getFirst();
        var constructorParameter = only(collection.byKind(DeclarationKind.PARAMETER), "value");

        assertSame(box.node(), field.owner());
        assertSame(box.node(), constructor.owner());
        assertSame(constructor.node(), constructorParameter.owner());
    }

    @Test
    void testCollectorPreservesDuplicateDeclarationsForLaterValidation() {

        var intType = new ReturnType(PrimitiveType.INT);
        var ast = List.<StatementNode>of(
            new VariableDeclarationStatement(1, 1, intType, "x", null),
            new VariableDeclarationStatement(2, 1, intType, "x", null)
        );

        var collection = new DeclarationCollector().collect(ast);

        assertEquals(2, collection.byName("x").size());
        assertEquals(2, collection.byKind(DeclarationKind.VARIABLE).size());
    }

    @Test
    void testCollectorPreservesParsedTypeSyntaxOnSemanticDeclarations() {

        var collection = collect("""
            Later[3] value;
            public class Later { }
            """);

        var variable = only(collection.byKind(DeclarationKind.VARIABLE), "value");
        var arraySyntax = assertInstanceOf(ArrayTypeSyntax.class, variable.declaredTypeSyntax());
        assertEquals("Later", arraySyntax.getName());

        var classDeclaration = only(collection.byKind(DeclarationKind.CLASS), "Later");
        var classSyntax = assertInstanceOf(NamedTypeSyntax.class, classDeclaration.declaredTypeSyntax());
        assertEquals("Later", classSyntax.getName());
    }

    private void assertNames(List<SemanticDeclaration> declarations, String... names) {
        assertEquals(List.of(names), declarations.stream().map(SemanticDeclaration::name).toList());
    }

    private SemanticDeclaration only(List<SemanticDeclaration> declarations, String name) {
        return declarations.stream()
            .filter(declaration -> declaration.name().equals(name))
            .findFirst()
            .orElseThrow();
    }
}
