package parser.ast.nodes.statement;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.NamedStatementNode;
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import lexer.token.ReturnType;
import lexer.token.family.AccessModifier;
import lexer.token.family.NonPrimitiveType;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import parser.ast.nodes.type.NamedTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;
import parser.support.TypeSyntaxAdapter;

import java.util.ArrayList;
import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a class declaration statement, including its methods, fields, superclasses, generic parameters, inner classes, access modifier, and constructors.
public class ClassDeclarationStatement extends NamedStatementNode implements Printable {

    private final AccessModifier accessModifier;
    private ClassMethodDeclaration[] methods;
    private ClassFieldDeclaration[] fields;
    private ClassConstructorDeclaration[] constructors;
    private ClassDeclarationStatement[] innerClasses;
    private final ReturnType[] compatibilitySuperClasses;
    private final ReturnType[] compatibilityGenericClassParameters;
    private final TypeSyntax[] superClassSyntaxes;
    private final TypeSyntax[] genericClassParameterSyntaxes;

    /// Constructs a new ClassDeclarationStatement.
    ///
    /// @param line The line number where the class is declared.
    /// @param column The column number where the class is declared.
    /// @param name The name of the class.
    /// @param methods The methods declared within the class.
    /// @param fields The fields declared within the class.
    /// @param superClasses The parsed superclass type syntax nodes that the class extends or implements.
    /// @param genericClassParameters The parsed generic parameter type syntaxes of the class, if any.
    /// @param innerClasses The inner classes declared within the class.
    /// @param accessModifier The access modifier of the class (e.g., public, private).
    /// @param constructors The constructors declared within the class.
    public ClassDeclarationStatement(int line, int column, String name, ClassMethodDeclaration[] methods, ClassFieldDeclaration[] fields, TypeSyntax[] superClasses, TypeSyntax[] genericClassParameters, ClassDeclarationStatement[] innerClasses, AccessModifier accessModifier, ClassConstructorDeclaration[] constructors) {

        super(line, column, name);
        this.methods = methods;
        this.fields = fields;
        this.superClassSyntaxes = superClasses != null ? superClasses.clone() : new TypeSyntax[0];
        this.genericClassParameterSyntaxes = genericClassParameters;
        this.compatibilitySuperClasses = null;
        this.compatibilityGenericClassParameters = null;
        this.innerClasses = innerClasses;
        this.accessModifier = accessModifier;
        this.constructors = constructors;
    }

    /// Constructs a compatibility ClassDeclarationStatement from legacy ReturnType adapters.
    ///
    /// @param line The line number where the class is declared.
    /// @param column The column number where the class is declared.
    /// @param name The name of the class.
    /// @param methods The methods declared within the class.
    /// @param fields The fields declared within the class.
    /// @param superClasses The temporary ReturnType superclass adapters for older callers.
    /// @param genericClassParameters The temporary ReturnType generic parameter adapters, if any.
    /// @param innerClasses The inner classes declared within the class.
    /// @param accessModifier The access modifier of the class (e.g., public, private).
    /// @param constructors The constructors declared within the class.
    public ClassDeclarationStatement(int line, int column, String name, ClassMethodDeclaration[] methods, ClassFieldDeclaration[] fields, ReturnType[] superClasses, ReturnType[] genericClassParameters, ClassDeclarationStatement[] innerClasses, AccessModifier accessModifier, ClassConstructorDeclaration[] constructors) {

        super(line, column, name);
        this.methods = methods;
        this.fields = fields;
        this.compatibilitySuperClasses = superClasses;
        this.compatibilityGenericClassParameters = genericClassParameters;
        this.superClassSyntaxes = syntaxFromReturnTypes(superClasses);

        var arr = new TypeSyntax[genericClassParameters != null ? genericClassParameters.length : 0];
        for (int i = 0; i < arr.length; i++) arr[i] = genericClassParameters[i] != null ? genericClassParameters[i].getSyntax() : null;
        this.genericClassParameterSyntaxes = arr;

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
    public ReturnType[] getSuperClasses() {
        return compatibilitySuperClasses != null ? compatibilitySuperClasses : adaptersFromSyntaxes(superClassSyntaxes);
    }

    /// Returns the parsed source type syntax for each declared superclass.
    /// @return An array of parsed superclass type syntax nodes, with {@code null} for compatibility-only entries.
    public TypeSyntax[] getSuperClassSyntaxes() {

        return superClassSyntaxes.clone();
    }

    /// Returns the generic parameters of the class, if any.
    /// @return ReturnType adapters representing the generic parameters of the class, or an empty array if none are declared.
    public ReturnType[] getGenericClassParameters() {
        return compatibilityGenericClassParameters != null ? compatibilityGenericClassParameters : adaptersFromSyntaxes(genericClassParameterSyntaxes);
    }

    /// Returns the parsed source type syntax for this class's generic parameters, when available.
    /// @return The generic parameter syntax nodes, or an empty array if none are declared.
    public TypeSyntax[] getGenericClassParameterSyntaxes() {
        return genericClassParameterSyntaxes;
    }

    /// Returns the inner classes declared within the class.
    /// @return An array of ClassDeclarationStatement objects representing the inner classes declared within the class.
    public ClassDeclarationStatement[] getInnerClasses() { return innerClasses; }

    /// Returns the type of this class, which is a NonPrimitiveType wrapping this class declaration.
    /// @return A source-level type syntax node representing this class name.
    public TypeSyntax getTypeSyntax() {
        return new NamedTypeSyntax(getLine(), getColumn(), getName(), false);
    }

    /// Returns the type of this class, which is a NonPrimitiveType wrapping this class declaration.
    /// This ReturnType remains a compatibility adapter for older AST/printer callers.
    /// @return A ReturnType representing the type of this class.
    public ReturnType getReturnType() {

        return new ReturnType(
            new NonPrimitiveType(this.getName()),
            new ExpressionNode[0],
            getSuperClasses(),
           compatibilityGenericClassParameters == null ? null : getGenericClassParameters()[0],
            getTypeSyntax()
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
        var genericClassParameters = getGenericClassParameters();
        if (genericClassParameters != null && genericClassParameters.length > 0) {

            var superNames = new StringBuilder();
            for (var i = 0; i < genericClassParameters.length; i++) {

                if (i > 0) superNames.append(", ");
                superNames.append(buildTypeStringWithSizes(genericClassParameters[i]));
            }
            entries.add(new PrintEntry.Info("Generic types: " + superNames));
        }

        var superClasses = getSuperClasses();
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

    private static TypeSyntax[] syntaxFromReturnTypes(ReturnType[] types) {

        if (types == null) return new TypeSyntax[0];
        var syntaxes = new TypeSyntax[types.length];
        for (var i = 0; i < types.length; i++)
            syntaxes[i] = types[i] != null ? types[i].getSyntax() : null;
        return syntaxes;
    }

    private static ReturnType[] adaptersFromSyntaxes(TypeSyntax[] syntaxes) {

        if (syntaxes == null) return new ReturnType[0];
        var adapters = new ReturnType[syntaxes.length];
        for (var i = 0; i < syntaxes.length; i++)
            adapters[i] = TypeSyntaxAdapter.toReturnType(syntaxes[i]);
        return adapters;
    }
}
