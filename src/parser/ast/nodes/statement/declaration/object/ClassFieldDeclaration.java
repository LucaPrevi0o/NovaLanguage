package src.parser.ast.nodes.statement.declaration.object;

import src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import src.token.ReturnType;
import src.token.family.AccessModifier;
import src.parser.ast.nodes.ExpressionNode;

public class ClassFieldDeclaration extends VariableDeclarationStatement {

    private final AccessModifier accessModifier;

    public ClassFieldDeclaration(int line, int column, ReturnType variableType, String variableName, ExpressionNode initialValue, AccessModifier accessModifier) {

        super(line, column, variableType, variableName, initialValue);
        this.accessModifier = accessModifier;
    }

    public AccessModifier getAccessModifier() { return accessModifier; }
}
