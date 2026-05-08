package parser.ast.nodes.statement.declaration.object;

import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import lexer.token.ReturnType;
import lexer.token.family.AccessModifier;
import parser.ast.nodes.StatementNode;

import java.util.ArrayList;
import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a method declaration within a class, including its access modifier.
public class ClassMethodDeclaration extends FunctionDeclarationStatement {

    private final AccessModifier accessModifier;

    /// Constructs a new ClassMethodDeclaration.
    /// @param line The line number where the method is declared.
    /// @param column The column number where the method is declared.
    /// @param returnType The return type of the method.
    /// @param name The name of the method.
    /// @param parameters The parameters of the method.
    /// @param body The body of the method.
    /// @param accessModifier The access modifier of the method (e.g., public, private).
    public ClassMethodDeclaration(int line, int column, ReturnType returnType, String name, FunctionParameter[] parameters, StatementNode body, AccessModifier accessModifier) {

        super(line, column, returnType, name, parameters, body);
        this.accessModifier = accessModifier;
    }

    /// Returns the access modifier of the method.
    /// @return The access modifier of the method (e.g., public, private).
    public AccessModifier getAccessModifier() { return accessModifier; }

    @Override
    public String toPrintString() { return "ClassMethodDeclaration [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Info("Access Modifier: " + accessModifier));
        entries.add(new PrintEntry.Info("Type: " + buildTypeStringWithSizes(getDeclaredType()) + (getDeclaredType().isGeneric() ? " (Generic Type)" : "")));
        var params = getParameters();
        entries.add(new PrintEntry.Info("Parameters: " + params.length));
        appendParameterEntries(entries, params);
        entries.add(new PrintEntry.Info("Name: " + getName()));
        entries.add(new PrintEntry.Child("Body", getBody()));
        return entries;
    }
}
