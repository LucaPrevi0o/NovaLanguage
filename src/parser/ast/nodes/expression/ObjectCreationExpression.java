package parser.ast.nodes.expression;

import parser.ast.nodes.ExpressionNode;

/// Represents an object creation expression (new expression), used to instantiate a class.
public class ObjectCreationExpression extends ExpressionNode {

    private final String className;
    private final ExpressionNode[] arguments;

    /// Constructs a new ObjectCreationExpression with the specified line, column, class name, and constructor arguments.
    /// @param line The line number where the new expression appears.
    /// @param column The column number where the new expression starts.
    /// @param className The name of the class being instantiated.
    /// @param arguments The constructor arguments to pass to the class constructor.
    public ObjectCreationExpression(int line, int column, String className, ExpressionNode[] arguments) {

        super(line, column);
        this.className = className;
        this.arguments = arguments;
    }

    /// Returns the name of the class being instantiated.
    /// @return The className.
    public String getClassName() { return className; }

    /// Returns the constructor arguments.
    /// @return An array of ExpressionNode objects representing the constructor arguments.
    public ExpressionNode[] getArguments() { return arguments; }
}

