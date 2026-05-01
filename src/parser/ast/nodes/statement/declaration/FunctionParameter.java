package src.parser.ast.nodes.statement.declaration;

import src.parser.ast.nodes.Symbol;
import src.token.ReturnType;

/// Represents a parameter in a function declaration, including its type and name.
public class FunctionParameter extends Symbol {

    private final ReturnType type;

    /// Constructs a new FunctionParameter.
    /// @param line The line number where the parameter is declared.
    /// @param column The column number where the parameter is declared.
    /// @param name The name of the parameter.
    /// @param type The return type of the parameter.
    public FunctionParameter(int line, int column, String name, ReturnType type) {

        super(line, column, name);
        this.type = type;
    }

    /// Returns the return type of the parameter.
    /// @return The return type of the parameter.
    public ReturnType getType() { return type; }
}