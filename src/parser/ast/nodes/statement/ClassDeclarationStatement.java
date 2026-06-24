package parser.ast.nodes.statement;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import lexer.token.ReturnType;
import lexer.token.family.AccessModifier;
import lexer.token.family.NonPrimitiveType;
import parser.ast.nodes.Symbol;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;

import java.util.ArrayList;
import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a class declaration statement, including its methods, fields, superclasses, generic parameters, inner classes, access modifier, and constructors.
public class ClassDeclarationStatement extends Symbol implements Printable {

    private final AccessModifier accessModifier;
    private ClassMethodDeclaration[] methods;
    private ClassFieldDeclaration[] fields;
    private ClassConstructorDeclaration[] constructors;
    private ClassDeclarationStatement[] innerClasses;
    private final ReturnType[] superClasses;
    private final ReturnType genericClassParameter;

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
    /// This ReturnType remains the parser-side metadata adapter until Phase 5 adds resolved semantic type symbols.
    /// @return A ReturnType representing the type of this class.
    public ReturnType getReturnType() {

        return new ReturnType(
            new NonPrimitiveType(this.getName()),
            new ExpressionNode[0],
            superClasses,
            genericClassParameter
        );
    }

    /// Returns the constructors declared within the class.
    /// @return An array of ClassConstructorDeclaration objects representing the constructors declared within the class.
    public ClassConstructorDeclaration[] getConstructors() { return constructors; }

    /// Sets the methods of this class after parsing is complete.
    /// @param methods The parsed method declarations.
    public void setMethods(ClassMethodDeclaration[] methods) { this.methods = methods; }

    /// Sets the fields of this class after parsing is complete.
    /// @param fields The parsed field declarations.
    public void setFields(ClassFieldDeclaration[] fields) { this.fields = fields; }

    /// Sets the constructors of this class after parsing is complete.
    /// @param constructors The parsed constructor declarations.
    public void setConstructors(ClassConstructorDeclaration[] constructors) { this.constructors = constructors; }

    /// Sets the inner classes of this class after parsing is complete.
    /// @param innerClasses The parsed inner class declarations.
    public void setInnerClasses(ClassDeclarationStatement[] innerClasses) { this.innerClasses = innerClasses; }

    @Override
    public String toPrintString() { return "ClassDeclarationStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Info("Name: " + getName()));
        entries.add(new PrintEntry.Info("Access Modifier: " + accessModifier));
        if (genericClassParameter != null) entries.add(new PrintEntry.Info("Generic Parameter: " + buildTypeStringWithSizes(genericClassParameter)));
        if (superClasses != null && superClasses.length > 0) {

            var superNames = new StringBuilder();
            for (var i = 0; i < superClasses.length; i++) {

                if (i > 0) superNames.append(", ");
                superNames.append(buildTypeStringWithSizes(superClasses[i]));
            }
            entries.add(new PrintEntry.Info("Superclasses: " + superNames));
        }
        entries.add(new PrintEntry.Children("Constructors", constructors));
        entries.add(new PrintEntry.Children("Inner Classes", innerClasses));
        entries.add(new PrintEntry.Children("Fields", fields));
        entries.add(new PrintEntry.Children("Methods", methods));
        return entries;
    }
}
