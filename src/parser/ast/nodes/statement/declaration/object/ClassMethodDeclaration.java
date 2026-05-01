package src.parser.ast.nodes.statement.declaration.object;

import src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.token.ReturnType;
import src.token.family.AccessModifier;
import src.parser.ast.nodes.StatementNode;

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
}
