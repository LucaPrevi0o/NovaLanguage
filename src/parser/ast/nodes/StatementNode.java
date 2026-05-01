package src.parser.ast.nodes;

import src.parser.ast.AstNode;

/// Represents a statement node in the abstract syntax tree (AST).
///
/// This is an abstract class that serves as a base for all types of statements, such as declaration statements,
/// expression statements, control flow statements, etc.
public abstract class StatementNode extends AstNode {

    /// Constructs a new StatementNode with the specified line and column numbers.
    /// @param line The line number in the source code where this statement occurs.
    /// @param column The column number in the source code where this statement starts.
    public StatementNode(int line, int column) { super(line, column); }
}
