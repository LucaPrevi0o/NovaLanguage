package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import src.token.ReturnType;
import src.token.family.AccessModifier;
import src.parser.ast.nodes.Symbol;
import src.parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;

public class ClassDeclarationStatement extends Symbol {

    private final ClassMethodDeclaration[] methods;
    private final ClassFieldDeclaration[] fields;
    private final ClassDeclarationStatement superClass;
    private final ReturnType genericClassParameter;
    private final ClassDeclarationStatement[] innerClasses;
    private final AccessModifier accessModifier;
    private final ClassConstructorDeclaration[] constructors;

    public ClassDeclarationStatement(int line, int column, String name, ClassMethodDeclaration[] methods, ClassFieldDeclaration[] fields, ClassDeclarationStatement superClass, ReturnType genericClassParameter, ClassDeclarationStatement[] innerClasses, AccessModifier accessModifier, ClassConstructorDeclaration[] constructors) {

        super(line, column, name);
        this.methods = methods;
        this.fields = fields;
        this.superClass = superClass;
        this.genericClassParameter = genericClassParameter;
        this.innerClasses = innerClasses;
        this.accessModifier = accessModifier;
        this.constructors = constructors;
    }

    public ClassMethodDeclaration[] getMethods() { return methods; }
    public ClassFieldDeclaration[] getFields() { return fields; }
    public AccessModifier getAccessModifier() { return accessModifier; }
    public ClassDeclarationStatement getSuperClass() { return superClass; }
    public ReturnType getGenericClassParameter() { return genericClassParameter; }
    public ClassDeclarationStatement[] getInnerClasses() { return innerClasses; }
    public ClassConstructorDeclaration[] getConstructors() { return constructors; }
}
