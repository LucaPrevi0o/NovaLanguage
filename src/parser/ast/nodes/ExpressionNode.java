package parser.ast.nodes;

import parser.ast.AstNode;

/// Represents an expression node in the abstract syntax tree (AST).
public abstract class ExpressionNode extends AstNode {

    /// Constructs a new ExpressionNode with the specified line and column numbers.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    public ExpressionNode(int line, int column) { super(line, column); }
}
