package src.parser.ast.nodes;

/// Represents a symbol in the abstract syntax tree (AST), which can be a variable, function, or object name.
public abstract class Symbol extends StatementNode {
    
    private final String name;

    /// Constructs a new Symbol with the specified line, column, and name.
    /// @param line The line number in the source code where this symbol is defined.
    /// @param column The column number in the source code where this symbol starts.
    /// @param name The name of the symbol.
    public Symbol(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    /// Returns the name of this symbol.
    /// @return The name of this symbol.
    public String getName() { return name; }
}
