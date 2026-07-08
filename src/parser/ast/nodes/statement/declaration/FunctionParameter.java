package parser.ast.nodes.statement.declaration;

import parser.ast.Printable;
import parser.ast.nodes.NamedStatementNode;
import parser.ast.nodes.type.TypeSyntax;
import printer.AstPrinter;

import java.util.List;

/// Represents a parameter in a function declaration, including its type and name.
public class FunctionParameter extends NamedStatementNode implements Printable {

    private final TypeSyntax typeSyntax;

    /// Constructs a new FunctionParameter.
    /// @param line The line number where the parameter is declared.
    /// @param column The column number where the parameter is declared.
    /// @param name The name of the parameter.
    /// @param type The parsed source type syntax of the parameter.
    public FunctionParameter(int line, int column, String name, TypeSyntax type) {

        super(line, column, name);
        this.typeSyntax = type;
    }

    /// Returns the parsed source type syntax for this parameter.
    /// @return The parsed type syntax.
    public TypeSyntax getTypeSyntax() { return typeSyntax; }

    @Override
    public String toPrintString() { return "FunctionParameter [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {
        return List.of(new PrintEntry.Info("Name: " + getName()), new PrintEntry.Info("Type: " + AstPrinter.buildTypeStringWithSizes(getTypeSyntax())));
    }
}
