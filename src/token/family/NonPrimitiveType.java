package src.token.family;

import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.token.TokenFamily;

/**
 * Represents a custom class type (user-defined).
 * Only stores the class name - the full ClassDeclarationStatement 
 * can be retrieved from TypeRegistry if needed.
 */
public class NonPrimitiveType implements TokenFamily {

    private final ClassDeclarationStatement classDecl;

    /**
     * Create a non-primitive type with a class name.
     */
    public NonPrimitiveType(ClassDeclarationStatement className) {
        this.classDecl = className;
    }

    @Override
    public String get() { return classDecl.getName(); }

    public ClassDeclarationStatement getClassDeclaration() { return classDecl; }
}
