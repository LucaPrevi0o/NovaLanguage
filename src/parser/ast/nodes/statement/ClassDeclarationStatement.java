package parser.ast.nodes.statement;

import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import token.ReturnType;
import token.family.AccessModifier;
import token.family.NonPrimitiveType;
import parser.ast.nodes.Symbol;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import parser.ast.visitor.NodeVisitor;

/// Represents a class declaration statement, including its methods, fields, superclasses, generic parameters, inner classes, access modifier, and constructors.
public class ClassDeclarationStatement extends Symbol {

    private final ClassMethodDeclaration[] methods;
    private final ClassFieldDeclaration[] fields;
    private final ReturnType[] superClasses;
    private final ReturnType genericClassParameter;
    private final ClassDeclarationStatement[] innerClasses;
    private final AccessModifier accessModifier;
    private final ClassConstructorDeclaration[] constructors;

    /// Constructs a new ClassDeclarationStatement.
    ///
    /// @param line The line number where the class is declared.
    /// @param column The column number where the class is declared.
    /// @param name The name of the class.
    /// @param methods The methods declared within the class.
    /// @param fields The fields declared within the class.
    /// @param superClasses The superclasses that the class extends or implements.
    /// @param genericClassParameter The generic parameter of the class, if any.
    /// @param innerClasses The inner classes declared within the class.
    /// @param accessModifier The access modifier of the class (e.g., public, private).
    /// @param constructors The constructors declared within the class.
    public ClassDeclarationStatement(int line, int column, String name, ClassMethodDeclaration[] methods, ClassFieldDeclaration[] fields, ReturnType[] superClasses, ReturnType genericClassParameter, ClassDeclarationStatement[] innerClasses, AccessModifier accessModifier, ClassConstructorDeclaration[] constructors) {

        super(line, column, name);
        this.methods = methods;
        this.fields = fields;
        this.superClasses = superClasses;
        this.genericClassParameter = genericClassParameter;
        this.innerClasses = innerClasses;
        this.accessModifier = accessModifier;
        this.constructors = constructors;
    }

    /// Returns the methods declared within the class.
    /// @return An array of ClassMethodDeclaration objects representing the methods declared within the class.
    public ClassMethodDeclaration[] getMethods() { return methods; }

    /// Returns the fields declared within the class.
    /// @return An array of ClassFieldDeclaration objects representing the fields declared within the class.
    public ClassFieldDeclaration[] getFields() { return fields; }

    /// Returns the access modifier of the class.
    /// @return The access modifier of the class (e.g., public, private).
    public AccessModifier getAccessModifier() { return accessModifier; }

    /// Returns the superclasses that the class extends or implements.
    /// @return An array of ReturnType objects representing the superclasses that the class extends or implements.
    public ReturnType[] getSuperClasses() { return superClasses; }

    /// Returns the generic parameter of the class, if any.
    /// @return A ReturnType object representing the generic parameter of the class, or null if the class does not have a generic parameter.
    public ReturnType getGenericClassParameter() { return genericClassParameter; }

    /// Returns the inner classes declared within the class.
    /// @return An array of ClassDeclarationStatement objects representing the inner classes declared within the class.
    public ClassDeclarationStatement[] getInnerClasses() { return innerClasses; }

    /// Returns the type of this class, which is a NonPrimitiveType wrapping this class declaration.
    /// This ReturnType is consistent with how the class is registered in TypeRegistry.
    /// @return A ReturnType representing the type of this class.
    public ReturnType getReturnType() { return new ReturnType(new NonPrimitiveType(this)); }

    /// Returns the constructors declared within the class.
    /// @return An array of ClassConstructorDeclaration objects representing the constructors declared within the class.
    public ClassConstructorDeclaration[] getConstructors() { return constructors; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitClassDeclaration(this); }
}
