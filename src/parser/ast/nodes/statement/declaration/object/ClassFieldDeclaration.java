package parser.ast.nodes.statement.declaration.object;

import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import lexer.token.ReturnType;
import lexer.token.family.AccessModifier;
import parser.ast.nodes.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a field declaration within a class, including its access modifier.
public class ClassFieldDeclaration extends VariableDeclarationStatement {

    private final AccessModifier accessModifier;

    /// Constructs a new ClassFieldDeclaration.
    /// @param line The line number where the field is declared.
    /// @param column The column number where the field is declared.
    /// @param variableType The return type of the field.
    /// @param variableName The name of the field.
    /// @param initialValue The initial value of the field, if any.
    /// @param accessModifier The access modifier of the field (e.g., public, private).
    public ClassFieldDeclaration(int line, int column, ReturnType variableType, String variableName, ExpressionNode initialValue, AccessModifier accessModifier) {

        super(line, column, variableType, variableName, initialValue);
        this.accessModifier = accessModifier;
    }

    /// Returns the access modifier of the field.
    /// @return The access modifier of the field (e.g., public, private).
    public AccessModifier getAccessModifier() { return accessModifier; }

    @Override
    public String toPrintString() { return "ClassFieldDeclaration [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Info("Name: " + getName()));
        entries.add(new PrintEntry.Info("Type: " + buildTypeStringWithSizes(getDeclaredType()) + (getDeclaredType().isGeneric() ? " (Generic Type)" : "")));
        entries.add(new PrintEntry.Info("Access Modifier: " + accessModifier));
        if (getInitialValue() != null) entries.add(new PrintEntry.Child("Initializer", getInitialValue()));
        return entries;
    }
}
