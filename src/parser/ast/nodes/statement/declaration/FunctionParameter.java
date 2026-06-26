package parser.ast.nodes.statement.declaration;

import parser.ast.Printable;
import parser.ast.nodes.Symbol;
import lexer.token.ReturnType;
import parser.ast.nodes.type.TypeSyntax;
import parser.support.TypeSyntaxAdapter;
import printer.AstPrinter;

import java.util.List;

/// Represents a parameter in a function declaration, including its type and name.
public class FunctionParameter extends Symbol implements Printable {

    private final ReturnType compatibilityType;
    private final TypeSyntax typeSyntax;

    /// Constructs a new FunctionParameter.
    /// @param line The line number where the parameter is declared.
    /// @param column The column number where the parameter is declared.
    /// @param name The name of the parameter.
    /// @param type The parsed source type syntax of the parameter.
    public FunctionParameter(int line, int column, String name, TypeSyntax type) {

        super(line, column, name);
        this.typeSyntax = type;
        this.compatibilityType = null;
    }

    /// Constructs a compatibility FunctionParameter from a legacy ReturnType adapter.
    /// @param line The line number where the parameter is declared.
    /// @param column The column number where the parameter is declared.
    /// @param name The name of the parameter.
    /// @param type The temporary ReturnType adapter for older callers.
    public FunctionParameter(int line, int column, String name, ReturnType type) {

        super(line, column, name);
        this.compatibilityType = type;
        this.typeSyntax = type != null ? type.getSyntax() : null;
    }

    /// Returns the temporary ReturnType adapter for compatibility callers.
    /// @return The compatibility return type of the parameter.
    public ReturnType getType() {
        return compatibilityType != null ? compatibilityType : TypeSyntaxAdapter.toReturnType(typeSyntax);
    }

    /// Returns the parsed source type syntax for this parameter, when available.
    /// @return The parsed type syntax, or {@code null} for compatibility-only parameters.
    public TypeSyntax getTypeSyntax() { return typeSyntax; }

    @Override
    public String toPrintString() { return "FunctionParameter [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {
        return List.of(new PrintEntry.Info("Name: " + getName()), new PrintEntry.Info("Type: " + AstPrinter.buildTypeStringWithSizes(getType())));
    }
}
