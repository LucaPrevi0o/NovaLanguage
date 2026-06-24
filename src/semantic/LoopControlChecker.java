package semantic;

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

    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        if (statements != null)
            for (var statement : statements) checkStatement(statement, new Context(0, 0));
        return diagnostics.getDiagnostics();
    }

    private void checkStatement(StatementNode statement, Context context) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            for (var method : classDeclaration.getMethods()) checkFunction(method);
            for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor);
            for (var innerClass : classDeclaration.getInnerClasses()) checkStatement(innerClass, new Context(0, 0));
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            checkFunction(functionDeclaration);
            return;
        }

        if (statement instanceof BreakStatement breakStatement) {

            if (context.loopDepth == 0 && context.switchDepth == 0)
                report("Break statement outside loop or switch", breakStatement);
            return;
        }

        if (statement instanceof ContinueStatement continueStatement) {

            if (context.loopDepth == 0) report("Continue statement outside loop", continueStatement);
            return;
        }

        if (statement instanceof BlockStatement block) {

            for (var child : block.getStatements()) checkStatement(child, context);
            return;
        }

        if (statement instanceof IfStatement ifStatement) {

            checkStatement(ifStatement.getThenBlock(), context);
            checkStatement(ifStatement.getElseBlock(), context);
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {

            checkStatement(whileStatement.getBody(), context.inLoop());
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            var loopContext = context.inLoop();
            checkStatement(forStatement.getInitialization(), loopContext);
            checkStatement(forStatement.getIncrement(), loopContext);
            checkStatement(forStatement.getBody(), loopContext);
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            checkStatement(forEachStatement.getBody(), context.inLoop());
            return;
        }

        if (statement instanceof SwitchStatement switchStatement)
            for (var switchCase : switchStatement.getCases()) checkStatement(switchCase.getBody(), context.inSwitch());
    }

    private void checkFunction(FunctionDeclarationStatement functionDeclaration) {
        checkStatement(functionDeclaration.getBody(), new Context(0, 0));
    }

    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration) {
        checkStatement(constructorDeclaration.getBody(), new Context(0, 0));
    }

    private void report(String message, StatementNode statement) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            message,
            statement.getLine(),
            statement.getColumn()
        ));
    }

    private record Context(int loopDepth, int switchDepth) {

        Context inLoop() { return new Context(loopDepth + 1, switchDepth); }

        Context inSwitch() { return new Context(loopDepth, switchDepth + 1); }
    }
}
