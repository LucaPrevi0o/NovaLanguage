package src.parser.ast.nodes.statement.declaration;

import src.parser.ast.nodes.statement.DeclarationStatement;
import src.token.ReturnType;
import src.parser.ast.nodes.StatementNode;

/// Represents a function declaration statement in the abstract syntax tree (AST).
public class FunctionDeclarationStatement extends DeclarationStatement {

    private final FunctionParameter[] parameters;
    private final StatementNode body;

    /// Constructs a new FunctionDeclarationStatement.
    /// @param line The line number where the function is declared.
    /// @param column The column number where the function is declared.
    /// @param returnType The return type of the function.
    /// @param name The name of the function.
    /// @param parameters The parameters of the function.
    /// @param body The body of the function.
    public FunctionDeclarationStatement(int line, int column, ReturnType returnType, String name, FunctionParameter[] parameters, StatementNode body) {

        super(line, column, returnType, name);
        this.parameters = parameters;
        this.body = body;
    }

    /// Returns the parameters of the function.
    /// @return An array of FunctionParameter objects representing the function's parameters.
    public FunctionParameter[] getParameters() { return parameters; }

    /// Returns the body of the function.
    /// @return A StatementNode representing the body of the function.
    public StatementNode getBody() { return body; }
}
