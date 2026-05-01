package src.parser.ast.nodes.statement.declaration;

import src.parser.ast.nodes.*;
import src.parser.ast.nodes.statement.DeclarationStatement;
import src.token.ReturnType;

/// Represents a variable declaration statement in the abstract syntax tree (AST).
public class VariableDeclarationStatement extends DeclarationStatement {

    private final ExpressionNode initialValue;

    /// Constructs a new VariableDeclarationStatement.
    /// @param line The line number where the variable is declared.
    /// @param column The column number where the variable is declared.
    /// @param variableType The return type of the variable.
    /// @param variableName The name of the variable.
    /// @param initialValue The initial value of the variable, if any.
    public VariableDeclarationStatement(int line, int column, ReturnType variableType, String variableName, ExpressionNode initialValue) {

        super(line, column, variableType, variableName);
        this.initialValue = initialValue;
    }

    /// Returns the initial value of the variable, if any.
    /// @return An ExpressionNode representing the initial value of the variable, or null if no initial value is provided.
    public ExpressionNode getInitialValue() { return initialValue; }
}
