package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import lexer.token.family.Operator;
import parser.ast.AstNode;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.expression.AssignmentExpression;
import parser.ast.nodes.expression.BinaryExpression;
import parser.ast.nodes.expression.CallExpression;
import parser.ast.nodes.expression.ObjectCreationExpression;
import parser.ast.nodes.expression.PostfixUnaryExpression;
import parser.ast.nodes.expression.TernaryExpression;
import parser.ast.nodes.expression.UnaryExpression;
import parser.ast.nodes.expression.access.ArrayAccessExpression;
import parser.ast.nodes.expression.access.MemberAccessExpression;
import parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.ExpressionStatement;
import parser.ast.nodes.statement.ReturnStatement;
import parser.ast.nodes.statement.conditional.ForEachStatement;
import parser.ast.nodes.statement.conditional.ForStatement;
import parser.ast.nodes.statement.conditional.IfStatement;
import parser.ast.nodes.statement.conditional.SwitchStatement;
import parser.ast.nodes.statement.conditional.WhileStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;

import java.util.List;

/// Checks that assignments and update operators target assignable expressions.
public final class LValueChecker {

    private final DiagnosticBag diagnostics = new DiagnosticBag();

    /// Checks a list of statements for invalid assignment targets and update operators.
    /// @param statements The list of statements to check.
    /// @return A list of diagnostics for any invalid assignment targets or update operators found.
    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        if (statements != null)
            for (var statement : statements) checkStatement(statement);
        return diagnostics.getDiagnostics();
    }

    /// Recursively checks a statement and its nested statements for invalid assignment targets and update operators.
    /// @param statement The statement to check.
    private void checkStatement(StatementNode statement) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> {

                for (var field : classDeclaration.getFields()) checkStatement(field);
                for (var method : classDeclaration.getMethods()) checkStatement(method);
                for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor);
                for (var innerClass : classDeclaration.getInnerClasses()) checkStatement(innerClass);
            }
            case FunctionDeclarationStatement functionDeclaration -> checkStatement(functionDeclaration.getBody());
            case VariableDeclarationStatement variableDeclaration -> checkExpression(variableDeclaration.getInitialValue());
            case BlockStatement block -> {
                for (var child : block.getStatements()) checkStatement(child);
            }
            case ExpressionStatement expressionStatement -> checkExpression(expressionStatement.getExpression());
            case ReturnStatement returnStatement -> checkExpression(returnStatement.getReturnValue());
            case IfStatement ifStatement -> {

                checkExpression(ifStatement.getCondition());
                checkStatement(ifStatement.getThenBlock());
                checkStatement(ifStatement.getElseBlock());
            }
            case WhileStatement whileStatement -> {

                checkExpression(whileStatement.getCondition());
                checkStatement(whileStatement.getBody());
            }
            case ForStatement forStatement -> {

                checkStatement(forStatement.getInitialization());
                checkExpression(forStatement.getCondition());
                checkStatement(forStatement.getIncrement());
                checkStatement(forStatement.getBody());
            }
            case ForEachStatement forEachStatement -> {

                checkExpression(forEachStatement.getIterable());
                checkStatement(forEachStatement.getBody());
            }
            case SwitchStatement switchStatement -> {

                checkExpression(switchStatement.getSubject());
                for (var switchCase : switchStatement.getCases()) {

                    checkExpression(switchCase.getValue());
                    checkStatement(switchCase.getBody());
                }
            }
            case null, default -> {}
        }

    }

    /// Checks a class constructor declaration for invalid assignment targets and update operators in its body.
    /// @param constructorDeclaration The class constructor declaration to check.
    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration) {
        checkStatement(constructorDeclaration.getBody());
    }

    /// Recursively checks an expression and its nested expressions for invalid assignment targets and update operators.
    /// @param expression The expression to check.
    private void checkExpression(ExpressionNode expression) {

        switch (expression) {

            case AssignmentExpression assignment -> {

                if (isNotAssignableTarget(assignment.getTarget())) reportInvalidTarget(assignment.getTarget());
                checkExpression(assignment.getTarget());
                checkExpression(assignment.getValue());
            }
            case UnaryExpression unary -> {

                if (isUpdateOperator(unary.getOperator().getType()) && isNotAssignableTarget(unary.getOperand()))
                    reportInvalidTarget(unary.getOperand());
                checkExpression(unary.getOperand());
            }
            case PostfixUnaryExpression postfix -> {

                if (isUpdateOperator(postfix.getOperator().getType()) && isNotAssignableTarget(postfix.getOperand()))
                    reportInvalidTarget(postfix.getOperand());
                checkExpression(postfix.getOperand());
            }
            case BinaryExpression binary -> {

                checkExpression(binary.getLeft());
                checkExpression(binary.getRight());
            }
            case TernaryExpression ternary -> {

                checkExpression(ternary.getCondition());
                checkExpression(ternary.getThenExpr());
                checkExpression(ternary.getElseExpr());
            }
            case CallExpression call -> {

                checkExpression(call.getCallee());
                for (var argument : call.getArguments()) checkExpression(argument);
            }
            case ObjectCreationExpression objectCreation -> {
                for (var argument : objectCreation.getArguments()) checkExpression(argument);
            }
            case MemberAccessExpression memberAccess -> checkExpression(memberAccess.getObject());
            case ArrayAccessExpression arrayAccess -> {

                checkExpression(arrayAccess.getArray());
                checkExpression(arrayAccess.getIndex());
            }
            case null, default -> {}
        }

    }

    /// Checks if an expression is not a valid assignment target (L-value).
    /// @param expression The expression to check.
    /// @return `true` if the expression is not a valid assignment target; otherwise, `false`.
    private boolean isNotAssignableTarget(ExpressionNode expression) {

        return !(expression instanceof IdentifierLiteralExpression) &&
                !(expression instanceof ArrayAccessExpression) &&
                !(expression instanceof MemberAccessExpression);
    }

    /// Checks if an operator is an update operator (increment or decrement).
    /// @param operator The operator to check.
    /// @return `true` if the operator is an update operator; otherwise, `false`.
    private boolean isUpdateOperator(Object operator) {
        return operator == Operator.INCREMENT || operator == Operator.DECREMENT;
    }

    /// Reports a diagnostic for an invalid assignment target.
    /// @param node The AST node representing the invalid assignment target.
    private void reportInvalidTarget(AstNode node) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Invalid assignment target",
            node.getLine(),
            node.getColumn()
        ));
    }
}
