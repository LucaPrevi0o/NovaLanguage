package parser.ast.nodes.statement;

import parser.ast.nodes.*;
import parser.ast.visitor.NodeVisitor;

/// Represents a block statement in the abstract syntax tree (AST), which contains a sequence of statements.
public class BlockStatement extends StatementNode {
    
    private final StatementNode[] statements;

    /// Constructs a new BlockStatement with the specified line, column, and array of statements.
    /// @param line The line number in the source code where this block statement occurs.
    /// @param column The column number in the source code where this block statement starts.
    /// @param statements An array of StatementNode objects representing the statements contained within this block.
    public BlockStatement(int line, int column, StatementNode[] statements) {

        super(line, column);
        this.statements = statements;
    }

    /// Returns the array of statements contained within this block statement.
    /// @return An array of StatementNode objects representing the statements in this block.
    public StatementNode[] getStatements() { return statements; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitBlock(this); }
}
