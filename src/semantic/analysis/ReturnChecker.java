package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
import lexer.token.family.PrimitiveType;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.ReturnStatement;
import parser.ast.nodes.statement.conditional.ForEachStatement;
import parser.ast.nodes.statement.conditional.ForStatement;
import parser.ast.nodes.statement.conditional.IfStatement;
import parser.ast.nodes.statement.conditional.SwitchStatement;
import parser.ast.nodes.statement.conditional.WhileStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;

import java.util.List;

/// Checks return statements against their enclosing function, method, or constructor.
public final class ReturnChecker {

    private final DiagnosticBag diagnostics = new DiagnosticBag();

    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        if (statements != null)
            for (var statement : statements) checkStatement(statement, null);
        return diagnostics.getDiagnostics();
    }

    private void checkStatement(StatementNode statement, ReturnContext context) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            for (var method : classDeclaration.getMethods()) checkFunction(method);
            for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor);
            for (var innerClass : classDeclaration.getInnerClasses()) checkStatement(innerClass, context);
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            checkFunction(functionDeclaration);
            return;
        }

        if (statement instanceof ReturnStatement returnStatement) {

            checkReturn(returnStatement, context);
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

            checkStatement(whileStatement.getBody(), context);
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            checkStatement(forStatement.getInitialization(), context);
            checkStatement(forStatement.getIncrement(), context);
            checkStatement(forStatement.getBody(), context);
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            checkStatement(forEachStatement.getBody(), context);
            return;
        }

        if (statement instanceof SwitchStatement switchStatement)
            for (var switchCase : switchStatement.getCases()) checkStatement(switchCase.getBody(), context);
    }

    private void checkFunction(FunctionDeclarationStatement functionDeclaration) {

        var context = new ReturnContext(functionDeclaration.getName(), functionDeclaration.getDeclaredType(), false);
        checkStatement(functionDeclaration.getBody(), context);

        if (!isVoid(functionDeclaration.getDeclaredType()) && !guaranteesReturn(functionDeclaration.getBody()))
            diagnostics.report(Diagnostic.error(
                DiagnosticPhase.SEMANTIC,
                "Missing return in non-void function '" + functionDeclaration.getName() + "'",
                functionDeclaration.getLine(),
                functionDeclaration.getColumn()
            ));
    }

    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration) {

        var context = new ReturnContext("<constructor>", new ReturnType(PrimitiveType.VOID), true);
        checkStatement(constructorDeclaration.getBody(), context);
    }

    private void checkReturn(ReturnStatement returnStatement, ReturnContext context) {

        if (context == null) {

            diagnostics.report(Diagnostic.error(
                DiagnosticPhase.SEMANTIC,
                "Return statement outside function",
                returnStatement.getLine(),
                returnStatement.getColumn()
            ));
            return;
        }

        var hasValue = returnStatement.getReturnValue() != null;
        if (context.constructor && hasValue) {

            diagnostics.report(Diagnostic.error(
                DiagnosticPhase.SEMANTIC,
                "Constructor cannot return a value",
                returnStatement.getLine(),
                returnStatement.getColumn()
            ));
            return;
        }

        if (isVoid(context.returnType) && hasValue) diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Void function '" + context.name + "' cannot return a value",
            returnStatement.getLine(),
            returnStatement.getColumn()
        ));

        if (!isVoid(context.returnType) && !hasValue) diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Non-void function '" + context.name + "' must return a value",
            returnStatement.getLine(),
            returnStatement.getColumn()
        ));
    }

    private boolean guaranteesReturn(StatementNode statement) {

        if (statement == null) return false;
        if (statement instanceof ReturnStatement) return true;

        if (statement instanceof BlockStatement block) {

            for (var child : block.getStatements())
                if (guaranteesReturn(child)) return true;
            return false;
        }

        if (statement instanceof IfStatement ifStatement)
            return ifStatement.getElseBlock() != null &&
                guaranteesReturn(ifStatement.getThenBlock()) &&
                guaranteesReturn(ifStatement.getElseBlock());

        return false;
    }

    private boolean isVoid(ReturnType returnType) {
        return returnType != null && returnType.getTokenClass() == PrimitiveType.VOID;
    }

    private record ReturnContext(String name, ReturnType returnType, boolean constructor) { }
}
