package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.Symbol;
import src.token.ReturnType;

public abstract class DeclarationStatement extends Symbol {

    private final ReturnType type;

    public DeclarationStatement(int line, int column, ReturnType type, String name) {
        
        super(line, column, name);
        this.type = type;
    }

    public ReturnType getDeclaredType() { return type; }
}
