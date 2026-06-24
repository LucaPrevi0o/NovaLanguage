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

    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        if (statements != null)
            for (var statement : statements) checkStatement(statement);
        return diagnostics.getDiagnostics();
    }

    private void checkStatement(StatementNode statement) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            for (var field : classDeclaration.getFields()) checkStatement(field);
            for (var method : classDeclaration.getMethods()) checkStatement(method);
            for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor);
            for (var innerClass : classDeclaration.getInnerClasses()) checkStatement(innerClass);
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            checkStatement(functionDeclaration.getBody());
            return;
        }

        if (statement instanceof VariableDeclarationStatement variableDeclaration) {

            checkExpression(variableDeclaration.getInitialValue());
            return;
        }

        if (statement instanceof BlockStatement block) {

            for (var child : block.getStatements()) checkStatement(child);
            return;
        }

        if (statement instanceof ExpressionStatement expressionStatement) {

            checkExpression(expressionStatement.getExpression());
            return;
        }

        if (statement instanceof ReturnStatement returnStatement) {

            checkExpression(returnStatement.getReturnValue());
            return;
        }

        if (statement instanceof IfStatement ifStatement) {

            checkExpression(ifStatement.getCondition());
            checkStatement(ifStatement.getThenBlock());
            checkStatement(ifStatement.getElseBlock());
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {

            checkExpression(whileStatement.getCondition());
            checkStatement(whileStatement.getBody());
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            checkStatement(forStatement.getInitialization());
            checkExpression(forStatement.getCondition());
            checkStatement(forStatement.getIncrement());
            checkStatement(forStatement.getBody());
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            checkExpression(forEachStatement.getIterable());
            checkStatement(forEachStatement.getBody());
            return;
        }

        if (statement instanceof SwitchStatement switchStatement) {

            checkExpression(switchStatement.getSubject());
            for (var switchCase : switchStatement.getCases()) {
                checkExpression(switchCase.getValue());
                checkStatement(switchCase.getBody());
            }
        }
    }

    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration) {
        checkStatement(constructorDeclaration.getBody());
    }

    private void checkExpression(ExpressionNode expression) {

        if (expression == null) return;

        if (expression instanceof AssignmentExpression assignment) {

            if (!isAssignableTarget(assignment.getTarget())) reportInvalidTarget(assignment.getTarget());
            checkExpression(assignment.getTarget());
            checkExpression(assignment.getValue());
            return;
        }

        if (expression instanceof UnaryExpression unary) {

            if (isUpdateOperator(unary.getOperator().getType()) && !isAssignableTarget(unary.getOperand()))
                reportInvalidTarget(unary.getOperand());
            checkExpression(unary.getOperand());
            return;
        }

        if (expression instanceof PostfixUnaryExpression postfix) {

            if (isUpdateOperator(postfix.getOperator().getType()) && !isAssignableTarget(postfix.getOperand()))
                reportInvalidTarget(postfix.getOperand());
            checkExpression(postfix.getOperand());
            return;
        }

        if (expression instanceof BinaryExpression binary) {

            checkExpression(binary.getLeft());
            checkExpression(binary.getRight());
            return;
        }

        if (expression instanceof TernaryExpression ternary) {

            checkExpression(ternary.getCondition());
            checkExpression(ternary.getThenExpr());
            checkExpression(ternary.getElseExpr());
            return;
        }

        if (expression instanceof CallExpression call) {

            checkExpression(call.getCallee());
            for (var argument : call.getArguments()) checkExpression(argument);
            return;
        }

        if (expression instanceof ObjectCreationExpression objectCreation) {

            for (var argument : objectCreation.getArguments()) checkExpression(argument);
            return;
        }

        if (expression instanceof MemberAccessExpression memberAccess) {

            checkExpression(memberAccess.getObject());
            return;
        }

        if (expression instanceof ArrayAccessExpression arrayAccess) {

            checkExpression(arrayAccess.getArray());
            checkExpression(arrayAccess.getIndex());
        }
    }

    private boolean isAssignableTarget(ExpressionNode expression) {

        return expression instanceof IdentifierLiteralExpression ||
               expression instanceof ArrayAccessExpression ||
               expression instanceof MemberAccessExpression;
    }

    private boolean isUpdateOperator(Object operator) {
        return operator == Operator.INCREMENT || operator == Operator.DECREMENT;
    }

    private void reportInvalidTarget(AstNode node) {

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Invalid assignment target",
            node.getLine(),
            node.getColumn()
        ));
    }
}
