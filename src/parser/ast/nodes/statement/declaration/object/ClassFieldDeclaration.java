package src.parser.ast.nodes.statement.declaration.object;

import src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import src.token.ReturnType;
import src.token.family.AccessModifier;
import src.parser.ast.nodes.ExpressionNode;

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
}
