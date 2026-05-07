package parser.ast.nodes.statement.conditional;

import parser.ast.Printable;
import parser.ast.nodes.*;
import parser.ast.nodes.statement.ConditionalStatement;

import java.util.ArrayList;
import java.util.List;

/// Represents a for loop statement in the AST.
///
/// A for loop consists of an initialization statement, a condition expression, an increment statement, and a body statement.
/// The initialization is executed once before the loop starts, the condition is evaluated before each iteration,
/// the body is executed if the condition is true, and the increment is executed after each iteration.
public class ForStatement extends ConditionalStatement implements Printable {
    
    private final StatementNode initialization;
    private final StatementNode increment;
    private final StatementNode body;

    /// Constructs a new ForStatement with the specified line, column, initialization, condition, increment, and body.
    /// @param line The line number in the source code where this statement occurs.
    /// @param column The column number in the source code where this statement starts.
    /// @param initialization The statement that initializes the loop variable(s) before the loop starts.
    /// @param condition The expression that is evaluated before each iteration to determine if the loop should continue.
    /// @param increment The statement that is executed after each iteration to update the loop variable(s).
    /// @param body The statement that represents the body of the loop, which is executed if the condition is true.
    public ForStatement(int line, int column, StatementNode initialization, ExpressionNode condition, StatementNode increment, StatementNode body) {

        super(line, column, condition);
        this.initialization = initialization;
        this.increment = increment;
        this.body = body;
    }

    /// Returns the statement that initializes the loop variable(s) before the loop starts.
    /// @return The initialization statement.
    public StatementNode getInitialization() { return initialization; }

    /// Returns the statement that is executed after each iteration to update the loop variable(s).
    /// @return The increment statement.
    public StatementNode getIncrement() { return increment; }

    /// Returns the statement that represents the body of the loop, which is executed if the condition is true.
    /// @return The body statement.
    public StatementNode getBody() { return body; }

    @Override
    public String toPrintString() { return "ForStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {

        var entries = new ArrayList<PrintEntry>();
        entries.add(new PrintEntry.Child("Condition", getCondition()));
        if (initialization != null) entries.add(new PrintEntry.Child("Initializer", initialization));
        if (increment != null) entries.add(new PrintEntry.Child("Increment", increment));
        entries.add(new PrintEntry.Child("Body", body));
        return entries;
    }
}
