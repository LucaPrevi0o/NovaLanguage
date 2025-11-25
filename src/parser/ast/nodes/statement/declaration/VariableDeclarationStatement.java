package src.parser.ast.nodes.statement.declaration;

import src.parser.ast.nodes.*;
import src.parser.ast.nodes.statement.DeclarationStatement;
import src.token.ReturnType;

public class VariableDeclarationStatement extends DeclarationStatement {

    private final ExpressionNode initialValue;

    public VariableDeclarationStatement(int line, int column, ReturnType variableType, String variableName, ExpressionNode initialValue) {

        super(line, column, variableType, variableName);
        this.initialValue = initialValue;
    }

    public ExpressionNode getInitialValue() { return initialValue; }
}
