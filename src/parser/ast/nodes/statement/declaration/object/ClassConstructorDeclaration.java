package parser.ast.nodes.statement.declaration.object;

import parser.ast.Printable;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import lexer.token.family.AccessModifier;

import java.util.ArrayList;
import java.util.List;

/// Represents a constructor declaration within a class.
public class ClassConstructorDeclaration extends StatementNode implements Printable {

    private final FunctionParameter[] parameters;
    private final StatementNode body;
    private final AccessModifier accessModifier;

    /// Constructs a new ClassConstructorDeclaration.
    ///
    /// @param line The line number where the constructor is declared.
    /// @param column The column number where the constructor is declared.
    /// @param parameters The parameters of the constructor.
    /// @param body The body of the constructor.
    /// @param accessModifier The access modifier of the constructor (e.g., public, private).
    public ClassConstructorDeclaration(int line, int column, FunctionParameter[] parameters, StatementNode body, AccessModifier accessModifier) {

        super(line, column);
        this.parameters = parameters;
        this.body = body;
        this.accessModifier = accessModifier;
    }

    /// Returns the parameters of the constructor.
    /// @return An array of FunctionParameter objects representing the constructor's parameters.
    public FunctionParameter[] getParameters() { return parameters; }

    /// Returns the body of the constructor.
    /// @return A StatementNode representing the body of the constructor.
    public StatementNode getBody() { return body; }

    /// Returns the access modifier of the constructor.
    /// @return The access modifier of the constructor (e.g., public, private).
    public AccessModifier getAccessModifier() { return accessModifier; }

    @Override
    public String toPrintString() { return "ClassConstructorDeclaration [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Info("Access Modifier: " + accessModifier));
        entries.add(new PrintEntry.Children("Parameters", parameters));
        entries.add(new PrintEntry.Child("Body", body));
        return entries;
    }
}
