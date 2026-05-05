package token.family;

import parser.ast.nodes.statement.ClassDeclarationStatement;
import token.TokenFamily;

/// Represents a non-primitive type in the token family, which is associated with a class declaration.
public class NonPrimitiveType implements TokenFamily {

    private final ClassDeclarationStatement classDecl;

    /// Constructs a new NonPrimitiveType with the specified class declaration.
    /// @param className The ClassDeclarationStatement representing the class associated with this non-primitive type.
    public NonPrimitiveType(ClassDeclarationStatement className) { this.classDecl = className; }

    @Override
    public String get() { return classDecl.getName(); }

    /// Returns the ClassDeclarationStatement associated with this non-primitive type.
    /// @return The ClassDeclarationStatement representing the class associated with this non-primitive type.
    public ClassDeclarationStatement getClassDeclaration() { return classDecl; }
}
