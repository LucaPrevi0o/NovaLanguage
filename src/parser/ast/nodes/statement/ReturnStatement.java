package parser.ast.nodes.statement;

import parser.ast.nodes.*;

/// Represents a return statement in the abstract syntax tree (AST), which consists of an optional return value expression.
public class ReturnStatement extends StatementNode {
    
    private final ExpressionNode returnValue;

    /// Constructs a new ReturnStatement with the specified line, column, and optional return value expression.
    /// @param line The line number in the source code where this return statement occurs.
    /// @param column The column number in the source code where this return statement starts.
    /// @param returnValue An ExpressionNode representing the value that is returned by this return statement, or null if no value is returned.
    public ReturnStatement(int line, int column, ExpressionNode returnValue) {

        super(line, column);
        this.returnValue = returnValue;
    }

    /// Returns the expression that represents the value returned by this return statement, or null if no value is returned.
    /// @return An ExpressionNode representing the return value of this return statement, or null if no value is returned.
    public ExpressionNode getReturnValue() { return returnValue; }
}
