package parser.ast.nodes.expression.access;

import parser.ast.nodes.ExpressionNode;
import parser.ast.visitor.NodeVisitor;

/// Represents an expression that accesses a member of an object.
///
/// Examples: `person.name`, `car.engine.type`, `myObject.field`
public class MemberAccessExpression extends ExpressionNode {
    
    private final ExpressionNode object;      // The object being accessed
    private final String memberName;          // The member name

    /// Constructs a MemberAccessExpression with the given object and member name.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param object The expression representing the object being accessed.
    /// @param memberName The name of the member being accessed.
    public MemberAccessExpression(int line, int column, ExpressionNode object, String memberName) {

        super(line, column);
        this.object = object;
        this.memberName = memberName;
    }

    /// Returns the expression representing the object being accessed.
    /// @return The object expression.
    public ExpressionNode getObject() { return object; }

    /// Returns the name of the member being accessed.
    /// @return The member name.
    public String getMemberName() { return memberName; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitMemberAccess(this); }
}
