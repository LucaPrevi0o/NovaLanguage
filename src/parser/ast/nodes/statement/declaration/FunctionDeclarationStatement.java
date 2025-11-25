package src.parser.ast.nodes.statement.declaration;

import src.parser.ast.nodes.statement.DeclarationStatement;
import src.token.ReturnType;
import src.parser.ast.nodes.StatementNode;

public class FunctionDeclarationStatement extends DeclarationStatement {

    private final FunctionParameter[] parameters;
    private final StatementNode body;

    public FunctionDeclarationStatement(int line, int column, ReturnType returnType, String name, FunctionParameter[] parameters, StatementNode body) {

        super(line, column, returnType, name);
        this.parameters = parameters;
        this.body = body;
    }

    public FunctionParameter[] getParameters() { return parameters; }
    public StatementNode getBody() { return body; }
}
