package src.parser.ast.nodes.statement.declaration;

import src.parser.ast.nodes.Symbol;
import src.token.ReturnType;

public class FunctionParameter extends Symbol {

    private final ReturnType type;

    public FunctionParameter(int line, int column, String name, ReturnType type) {

        super(line, column, name);
        this.type = type;
    }

    public ReturnType getType() { return type; }
}