package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import src.token.ReturnType;
import src.token.family.AccessModifier;
import src.token.family.GenericParameterType;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.Symbol;
import src.parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;

public class ClassDeclarationStatement extends Symbol {

    private final ClassMethodDeclaration[] methods;
    private final ClassFieldDeclaration[] fields;
    private final ReturnType[] superClasses;
    private final ReturnType genericClassParameter;
    private final ClassDeclarationStatement[] innerClasses;
    private final AccessModifier accessModifier;
    private final ClassConstructorDeclaration[] constructors;

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

    public ClassMethodDeclaration[] getMethods() { return methods; }
    public ClassFieldDeclaration[] getFields() { return fields; }
    public AccessModifier getAccessModifier() { return accessModifier; }
    public ReturnType[] getSuperClasses() { return superClasses; }
    public ReturnType getGenericClassParameter() { return genericClassParameter; }
    public ClassDeclarationStatement[] getInnerClasses() { return innerClasses; }
    public ClassConstructorDeclaration[] getConstructors() { return constructors; }

    public ReturnType getReturnType() { return new ReturnType(new GenericParameterType(getName()), new ExpressionNode[0], superClasses, genericClassParameter); }
}
