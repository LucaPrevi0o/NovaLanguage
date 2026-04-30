package src.parser.ast.nodes;

public abstract class Symbol extends StatementNode {
    
    private final String name;

    public Symbol(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    public String getName() { return name; }
}
