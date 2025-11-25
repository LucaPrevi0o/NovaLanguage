package src.parser.ast.nodes.expression.access;

import src.parser.ast.nodes.ExpressionNode;

/**
 * Represents member access expression: object.member
 * Examples: person.name, array.length, obj.method()
 */
public class MemberAccessExpression extends ExpressionNode {
    
    private final ExpressionNode object;      // The object being accessed
    private final String memberName;          // The member name
    
    public MemberAccessExpression(int line, int column, ExpressionNode object, String memberName) {

        super(line, column);
        this.object = object;
        this.memberName = memberName;
    }
    
    public ExpressionNode getObject() { return object; }
    
    public String getMemberName() { return memberName; }
}
