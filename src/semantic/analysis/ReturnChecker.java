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

    /// Checks a list of statements for return statement issues.
    /// @param statements The list of statements to check.
    /// @return A list of diagnostics for any return statement issues found.
    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        if (statements != null)
            for (var statement : statements) checkStatement(statement, null);
        return diagnostics.getDiagnostics();
    }

    /// Recursively checks a statement and its nested statements for return statement issues.
    /// @param statement The statement to check.
    /// @param context The current return context, which tracks the enclosing function or constructor.
    private void checkStatement(StatementNode statement, ReturnContext context) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> {

                for (var method : classDeclaration.getMethods()) checkFunction(method);
                for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor);
                for (var innerClass : classDeclaration.getInnerClasses()) checkStatement(innerClass, context);
            }
            case FunctionDeclarationStatement functionDeclaration -> checkFunction(functionDeclaration);
            case ReturnStatement returnStatement -> checkReturn(returnStatement, context);
            case BlockStatement block -> {
                for (var child : block.getStatements()) checkStatement(child, context);
            }
            case IfStatement ifStatement -> {

                checkStatement(ifStatement.getThenBlock(), context);
                checkStatement(ifStatement.getElseBlock(), context);
            }
            case WhileStatement whileStatement -> checkStatement(whileStatement.getBody(), context);
            case ForStatement forStatement -> {

                checkStatement(forStatement.getInitialization(), context);
                checkStatement(forStatement.getIncrement(), context);
                checkStatement(forStatement.getBody(), context);
            }
            case ForEachStatement forEachStatement -> checkStatement(forEachStatement.getBody(), context);
            case SwitchStatement switchStatement -> {
                for (var switchCase : switchStatement.getCases()) checkStatement(switchCase.getBody(), context);
            }
            case null, default -> {}
        }

    }

    /// Checks a function declaration for return statement issues, ensuring that non-void functions have a return statement in all code paths.
    /// @param functionDeclaration The function declaration to check.
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

    /// Checks a class constructor declaration for return statement issues, ensuring that constructors do not return a value.
    /// @param constructorDeclaration The class constructor declaration to check.
    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration) {

        var context = new ReturnContext("<constructor>", new ReturnType(PrimitiveType.VOID), true);
        checkStatement(constructorDeclaration.getBody(), context);
    }

    /// Checks a return statement against its enclosing function or constructor context, reporting errors for invalid return usage.
    /// @param returnStatement The return statement to check.
    /// @param context The current return context, which tracks the enclosing function or constructor.
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

    /// Determines if a statement guarantees a return in all code paths.
    /// @param statement The statement to check.
    /// @return `true` if the statement guarantees a return; otherwise, `false`.
    private boolean guaranteesReturn(StatementNode statement) {

        return switch (statement) {

            case ReturnStatement _ -> true;
            case BlockStatement block -> {

                for (var child : block.getStatements())
                    if (guaranteesReturn(child)) yield true;
                yield false;
            }
            case IfStatement ifStatement -> ifStatement.getElseBlock() != null &&
                    guaranteesReturn(ifStatement.getThenBlock()) &&
                    guaranteesReturn(ifStatement.getElseBlock());
            case null, default -> false;
        };
    }

    /// Determines if a return type is void.
    /// @param returnType The return type to check.
    /// @return `true` if the return type is void; otherwise, `false`.
    private boolean isVoid(ReturnType returnType) {
        return returnType != null && returnType.getTokenClass() == PrimitiveType.VOID;
    }

    /// Represents the context of a return statement, including the name of the enclosing function or constructor, its return type, and whether it is a constructor.
    private record ReturnContext(String name, ReturnType returnType, boolean constructor) { }
}
