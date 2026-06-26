package parser.ast.nodes.statement.declaration;

import parser.ast.Printable;
import parser.ast.nodes.*;
import parser.ast.nodes.statement.DeclarationStatement;
import lexer.token.ReturnType;
import parser.ast.nodes.type.TypeSyntax;

import java.util.ArrayList;
import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a variable declaration statement in the abstract syntax tree (AST).
public class VariableDeclarationStatement extends DeclarationStatement implements Printable {

    private final ExpressionNode initialValue;

    /// Constructs a new VariableDeclarationStatement.
    /// @param line The line number where the variable is declared.
    /// @param column The column number where the variable is declared.
    /// @param variableType The return type of the variable.
    /// @param variableName The name of the variable.
    /// @param initialValue The initial value of the variable, if any.
    public VariableDeclarationStatement(int line, int column, TypeSyntax variableType, String variableName, ExpressionNode initialValue) {

        super(line, column, variableType, variableName);
        this.initialValue = initialValue;
    }

    /// Constructs a compatibility VariableDeclarationStatement from a legacy ReturnType adapter.
    /// @param line The line number where the variable is declared.
    /// @param column The column number where the variable is declared.
    /// @param variableType The temporary ReturnType adapter for older callers.
    /// @param variableName The name of the variable.
    /// @param initialValue The initial value of the variable, if any.
    public VariableDeclarationStatement(int line, int column, ReturnType variableType, String variableName, ExpressionNode initialValue) {

        super(line, column, variableType, variableName);
        this.initialValue = initialValue;
    }

    /// Returns the initial value of the variable, if any.
    /// @return An ExpressionNode representing the initial value of the variable, or null if no initial value is provided.
    public ExpressionNode getInitialValue() { return initialValue; }

    @Override
    public String toPrintString() { return "VariableDeclarationStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Info("Name: " + getName()));
        entries.add(new PrintEntry.Info("Type: " + buildTypeStringWithSizes(getDeclaredType())));
        if (initialValue != null) entries.add(new PrintEntry.Child("Initializer", initialValue));
        return entries;
    }
}
