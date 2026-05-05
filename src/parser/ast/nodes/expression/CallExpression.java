package parser.ast.nodes.expression;

import parser.ast.nodes.ExpressionNode;
import parser.ast.visitor.NodeVisitor;

/// Represents a function or method call expression in the AST.
///
/// - A call expression represents the invocation of a function or method, including the callee being called and the
/// arguments passed to it.
/// - The callee can be any expression that evaluates to a function or method, such as an identifier,
/// a member access expression, or another call expression.
/// - The arguments are represented as an array of expressions, allowing for any number of arguments to be passed to the callee.
public class CallExpression extends ExpressionNode {
    
    private final ExpressionNode callee;
    private final ExpressionNode[] arguments;

    /// Constructs a new CallExpression with the specified line, column, callee, and arguments.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param callee The expression representing the function or method being called.
    /// @param arguments An array of expressions representing the arguments passed to the callee.
    public CallExpression(int line, int column, ExpressionNode callee, ExpressionNode[] arguments) {

        super(line, column);
        this.callee = callee;
        this.arguments = arguments;
    }

    /// Returns the expression representing the function or method being called.
    /// @return The callee expression.
    public ExpressionNode getCallee() { return callee; }

    /// Returns an array of expressions representing the arguments passed to the callee.
    /// @return An array of argument expressions.
    public ExpressionNode[] getArguments() { return arguments; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitCall(this); }
}
