package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.BreakStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.ContinueStatement;
import parser.ast.nodes.statement.conditional.ForEachStatement;
import parser.ast.nodes.statement.conditional.ForStatement;
import parser.ast.nodes.statement.conditional.IfStatement;
import parser.ast.nodes.statement.conditional.SwitchStatement;
import parser.ast.nodes.statement.conditional.WhileStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;

import java.util.List;

/// Checks whether break and continue statements appear in a valid control-flow context.
public final class LoopControlChecker {

    private final DiagnosticBag diagnostics = new DiagnosticBag();

    /// Checks a list of statements for invalid break and continue statements.
    /// @param statements The list of statements to check.
    /// @return A list of diagnostics for any invalid break or continue statements found.
    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        if (statements != null)
            for (var statement : statements) checkStatement(statement, new Context(0, 0));
        return diagnostics.getDiagnostics();
    }

    /// Checks a single statement for invalid break and continue statements, recursively checking nested statements.
    /// @param statement The statement to check.
    /// @param context The current control-flow context, tracking loop and switch depths.
    private void checkStatement(StatementNode statement, Context context) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> {

                for (var method : classDeclaration.getMethods()) checkFunction(method);
                for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor);
                for (var innerClass : classDeclaration.getInnerClasses()) checkStatement(innerClass, new Context(0, 0));
            }
            case FunctionDeclarationStatement functionDeclaration -> checkFunction(functionDeclaration);
            case BreakStatement breakStatement -> {

                if (context.loopDepth == 0 && context.switchDepth == 0)
                    report("Break statement outside loop or switch", breakStatement);
            }
            case ContinueStatement continueStatement -> {
                if (context.loopDepth == 0) report("Continue statement outside loop", continueStatement);
            }
            case BlockStatement block -> {
                for (var child : block.getStatements()) checkStatement(child, context);
            }
            case IfStatement ifStatement -> {

                checkStatement(ifStatement.getThenBlock(), context);
                checkStatement(ifStatement.getElseBlock(), context);
            }
            case WhileStatement whileStatement -> checkStatement(whileStatement.getBody(), context.inLoop());
            case ForStatement forStatement -> {

                var loopContext = context.inLoop();
                checkStatement(forStatement.getInitialization(), loopContext);
                checkStatement(forStatement.getIncrement(), loopContext);
                checkStatement(forStatement.getBody(), loopContext);
            }
            case ForEachStatement forEachStatement -> checkStatement(forEachStatement.getBody(), context.inLoop());
            case SwitchStatement switchStatement -> {

                for (var switchCase : switchStatement.getCases())
                    checkStatement(switchCase.getBody(), context.inSwitch());
            }
            case null, default -> {}
        }
    }

    /// Checks a function declaration for invalid break and continue statements in its body.
    /// @param functionDeclaration The function declaration to check.
    private void checkFunction(FunctionDeclarationStatement functionDeclaration) {
        checkStatement(functionDeclaration.getBody(), new Context(0, 0));
    }

    /// Checks a class constructor declaration for invalid break and continue statements in its body.
    /// @param constructorDeclaration The class constructor declaration to check.
    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration) {
        checkStatement(constructorDeclaration.getBody(), new Context(0, 0));
    }

    /// Reports a diagnostic for an invalid break or continue statement.
    /// @param message The error message to report.
    /// @param statement The statement node where the error occurred.
    private void report(String message, StatementNode statement) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            message,
            statement.getLine(),
            statement.getColumn()
        ));
    }

    /// Represents the current control-flow context, tracking the depth of nested loops and switch statements.
    private record Context(int loopDepth, int switchDepth) {

        /// Creates a new context representing a deeper loop level.
        /// @return A new Context with loopDepth incremented by 1.
        Context inLoop() { return new Context(loopDepth + 1, switchDepth); }

        /// Creates a new context representing a deeper switch statement level.
        /// @return A new Context with switchDepth incremented by 1.
        Context inSwitch() { return new Context(loopDepth, switchDepth + 1); }
    }
}
