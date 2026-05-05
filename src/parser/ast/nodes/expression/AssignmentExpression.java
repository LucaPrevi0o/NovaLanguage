package parser.ast.nodes.expression;

import parser.ast.nodes.*;

/// Represents an assignment expression in the AST, where a value is assigned to a target (which can be a variable,
/// array element, or object property).
public class AssignmentExpression extends ExpressionNode {

    private final ExpressionNode target;  // Can be IdentifierLiteralExpression, ArrayAccessExpression, or MemberAccessExpression
    private final ExpressionNode value;

    /// Constructs an AssignmentExpression with the specified line, column, target, and value.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param target The expression representing the target of the assignment (e.g., variable, array element, or object property).
    /// @param value The expression representing the value being assigned to the target.
    public AssignmentExpression(int line, int column, ExpressionNode target, ExpressionNode value) {

        super(line, column);
        this.target = target;
        this.value = value;
    }

    /// Returns the expression representing the target of the assignment.
    /// @return The target expression.
    public ExpressionNode getTarget() { return target; }

    /// Returns the expression representing the value being assigned to the target.
    /// @return The value expression.
    public ExpressionNode getValue() { return value; }
}