package semantic.scope;

import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.ast.nodes.StatementNode;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for semantic lexical scope construction.
public class SemanticScopeBuilderTest {

    private List<StatementNode> parse(String source) {

        var tokens = new Lexer(source).tokenize();
        return new Parser(tokens).parse();
    }

    private SemanticScope build(String source) {
        return new SemanticScopeBuilder().build(parse(source));
    }

    @Test
    void testBuildsGlobalFunctionAndBlockScopes() {

        var root = build("int global; void main(int arg) { int local; { int nested; } }");

        assertNames(root.findLocal("global"), "global");
        var main = only(root.findLocal("main"), DeclarationKind.FUNCTION);
        var mainScope = root.childOwnedBy(main.getNode()).orElseThrow();

        assertNames(mainScope.findLocal("arg"), "arg");
        assertNames(mainScope.findLocal("local"), "local");
        assertTrue(root.findLocal("local").isEmpty());
        assertTrue(mainScope.findLocal("nested").isEmpty());

        var blockScope = mainScope.getChildren().getFirst();
        assertEquals("block", blockScope.getName());
        assertNames(blockScope.findLocal("nested"), "nested");
    }

    @Test
    void testBuildsClassMemberScopes() {

        var root = build("""
            public class Box {
                private int value;
                public Box(int value) { int constructed; }
                public int get(int fallback) { int local; return value; }
            }
            """);

        var box = only(root.findLocal("Box"), DeclarationKind.CLASS);
        var boxScope = root.childOwnedBy(box.getNode()).orElseThrow();

        assertNames(boxScope.findLocal("value"), "value");
        assertNames(boxScope.findLocal("get"), "get");
        assertNames(boxScope.findLocal("Box"), "Box");

        var method = only(boxScope.findLocal("get"), DeclarationKind.METHOD);
        var methodScope = boxScope.childOwnedBy(method.getNode()).orElseThrow();
        assertNames(methodScope.findLocal("fallback"), "fallback");
        assertNames(methodScope.findLocal("local"), "local");

        var constructor = only(boxScope.findLocal("Box"), DeclarationKind.CONSTRUCTOR);
        var constructorScope = boxScope.childOwnedBy(constructor.getNode()).orElseThrow();
        assertNames(constructorScope.findLocal("value"), "value");
        assertNames(constructorScope.findLocal("constructed"), "constructed");
    }

    @Test
    void testBuildsLoopScopes() {

        var root = build("for (int i = 0; i < 3; i++) { int body; }");

        assertTrue(root.findLocal("i").isEmpty());
        var forScope = root.getChildren().getFirst();
        assertEquals("for", forScope.getName());
        assertNames(forScope.findLocal("i"), "i");

        var bodyScope = forScope.getChildren().getFirst();
        assertEquals("block", bodyScope.getName());
        assertNames(bodyScope.findLocal("body"), "body");
    }

    private void assertNames(List<SemanticDeclaration> declarations, String... names) {
        assertEquals(List.of(names), declarations.stream().map(SemanticDeclaration::getName).toList());
    }

    private SemanticDeclaration only(List<SemanticDeclaration> declarations, DeclarationKind kind) {
        return declarations.stream()
            .filter(declaration -> declaration.getKind() == kind)
            .findFirst()
            .orElseThrow();
    }
}
