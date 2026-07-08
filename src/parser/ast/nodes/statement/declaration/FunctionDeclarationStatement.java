package parser.ast.nodes.statement.declaration;

import parser.ast.Printable;
import parser.ast.nodes.statement.DeclarationStatement;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.type.TypeSyntax;

import java.util.ArrayList;
import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a function declaration statement in the abstract syntax tree (AST).
public class FunctionDeclarationStatement extends DeclarationStatement implements Printable {

    private final FunctionParameter[] parameters;
    private StatementNode body;  // mutable to allow pre-registration before body is parsed

    /// Constructs a new FunctionDeclarationStatement.
    /// @param line The line number where the function is declared.
    /// @param column The column number where the function is declared.
    /// @param returnType The parsed source return type syntax of the function.
    /// @param name The name of the function.
    /// @param parameters The parameters of the function.
    /// @param body The body of the function (may be {@code null} for pre-registration before the body is parsed).
    public FunctionDeclarationStatement(int line, int column, TypeSyntax returnType, String name, FunctionParameter[] parameters, StatementNode body) {

        super(line, column, returnType, name);
        this.parameters = parameters;
        this.body = body;
    }

    /// Returns the parameters of the function.
    /// @return An array of FunctionParameter objects representing the function's parameters.
    public FunctionParameter[] getParameters() { return parameters; }

    /// Returns the body of the function.
    /// @return A StatementNode representing the body of the function, or {@code null} if not yet parsed.
    public StatementNode getBody() { return body; }

    /// Sets the body of the function after it has been parsed.
    /// This is used to support recursive calls by pre-registering the function before its body is known.
    /// @param body The parsed body statement.
    public void setBody(StatementNode body) { this.body = body; }

    @Override
    public String toPrintString() { return "FunctionDeclarationStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Info("Type: " + buildTypeStringWithSizes(getDeclaredTypeSyntax())));
        entries.add(new PrintEntry.Children("Parameters", parameters));
        entries.add(new PrintEntry.Info("Name: " + getName()));
        entries.add(new PrintEntry.Child("Body", body));
        return entries;
    }
}
