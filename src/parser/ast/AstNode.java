package src.parser.ast;

public abstract class AstNode {
    
    public AstNode(int line, int column) {

        this.line = line;
        this.column = column;
    }

    public int line;
    public int column;

    public int getLine() { return line; }
    public int getColumn() { return column; }

    public String toString() { return this.getClass().getSimpleName() + " (line: " + line + ", column: " + column + ")"; }
}
