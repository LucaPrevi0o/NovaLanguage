package src.parser.ast.nodes.statement.declaration.object;

import src.parser.ast.nodes.StatementNode;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.token.family.AccessModifier;

public class ClassConstructorDeclaration extends StatementNode {

    private final FunctionParameter[] parameters;
    private final StatementNode body;
    private final AccessModifier accessModifier;

    public ClassConstructorDeclaration(int line, int column, FunctionParameter[] parameters, StatementNode body, AccessModifier accessModifier) {

        super(line, column);
        this.parameters = parameters;
        this.body = body;
        this.accessModifier = accessModifier;
    }

    public FunctionParameter[] getParameters() { return parameters; }
    public StatementNode getBody() { return body; }
    public AccessModifier getAccessModifier() { return accessModifier; }
}