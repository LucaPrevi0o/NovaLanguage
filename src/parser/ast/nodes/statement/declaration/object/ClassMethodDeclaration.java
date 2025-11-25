package src.parser.ast.nodes.statement.declaration.object;

import src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.token.ReturnType;
import src.token.family.AccessModifier;
import src.parser.ast.nodes.StatementNode;

public class ClassMethodDeclaration extends FunctionDeclarationStatement {

    private final AccessModifier accessModifier;

    public ClassMethodDeclaration(int line, int column, ReturnType returnType, String name, FunctionParameter[] parameters, StatementNode body, AccessModifier accessModifier) {

        super(line, column, returnType, name, parameters, body);
        this.accessModifier = accessModifier;
    }

    public AccessModifier getAccessModifier() { return accessModifier; }
}
