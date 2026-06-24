package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
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
import parser.ast.nodes.expression.literal.BoolLiteralExpression;
import parser.ast.nodes.expression.literal.CharLiteralExpression;
import parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import parser.ast.nodes.expression.literal.NullLiteralExpression;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;
import parser.ast.nodes.expression.literal.StringLiteralExpression;
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
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;

import java.util.List;

/// Performs an initial semantic type check for declarations, assignments, and simple expressions.
public final class TypeChecker {

    private static final ReturnType BOOL_TYPE = new ReturnType(PrimitiveType.BOOL);

    private final DiagnosticBag diagnostics = new DiagnosticBag();
    private SemanticScope rootScope;

    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        rootScope = new SemanticScopeBuilder().build(statements);
        if (statements != null)
            for (var statement : statements) checkStatement(statement, rootScope);
        return diagnostics.getDiagnostics();
    }

    public SemanticScope getRootScope() { return rootScope; }

    private void checkStatement(StatementNode statement, SemanticScope scope) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            checkClass(classDeclaration, scope);
            return;
        }

        if (statement instanceof ClassMethodDeclaration methodDeclaration) {

            checkFunction(methodDeclaration, scope, "method " + methodDeclaration.getName());
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            checkFunction(functionDeclaration, scope, "function " + functionDeclaration.getName());
            return;
        }

        if (statement instanceof VariableDeclarationStatement variableDeclaration) {

            checkInitializer(variableDeclaration, scope);
            return;
        }

        if (statement instanceof BlockStatement block) {

            checkBlock(block, scope);
            return;
        }

        if (statement instanceof ExpressionStatement expressionStatement) {

            inferExpression(expressionStatement.getExpression(), scope);
            return;
        }

        if (statement instanceof ReturnStatement returnStatement) {

            inferExpression(returnStatement.getReturnValue(), scope);
            return;
        }

        if (statement instanceof IfStatement ifStatement) {

            expectBool(ifStatement.getCondition(), scope, "If condition must be bool");
            checkBranch(ifStatement.getThenBlock(), scope, "if-then", ifStatement);
            checkBranch(ifStatement.getElseBlock(), scope, "if-else", ifStatement);
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {

            expectBool(whileStatement.getCondition(), scope, "While condition must be bool");
            checkBranch(whileStatement.getBody(), scope, "while", whileStatement);
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            var forScope = childScope(scope, "for", forStatement);
            checkStatement(forStatement.getInitialization(), forScope);
            expectBool(forStatement.getCondition(), forScope, "For condition must be bool");
            checkStatement(forStatement.getIncrement(), forScope);
            checkBranch(forStatement.getBody(), forScope, "for-body", forStatement);
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            inferExpression(forEachStatement.getIterable(), scope);
            var forEachScope = childScope(scope, "for-each", forEachStatement);
            checkBranch(forEachStatement.getBody(), forEachScope, "for-each-body", forEachStatement);
            return;
        }

        if (statement instanceof SwitchStatement switchStatement) {

            inferExpression(switchStatement.getSubject(), scope);
            var switchScope = childScope(scope, "switch", switchStatement);
            for (var switchCase : switchStatement.getCases()) {
                inferExpression(switchCase.getValue(), scope);
                checkBranch(switchCase.getBody(), switchScope, "switch-case", switchCase);
            }
        }
    }

    private void checkClass(ClassDeclarationStatement classDeclaration, SemanticScope scope) {

        var classScope = childScope(scope, "class " + classDeclaration.getName(), classDeclaration);

        for (var field : classDeclaration.getFields()) checkStatement(field, classScope);
        for (var method : classDeclaration.getMethods()) checkStatement(method, classScope);
        for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor, classDeclaration, classScope);
        for (var innerClass : classDeclaration.getInnerClasses()) checkClass(innerClass, classScope);
    }

    private void checkFunction(FunctionDeclarationStatement functionDeclaration, SemanticScope scope, String scopeName) {

        var functionScope = childScope(scope, scopeName, functionDeclaration);
        checkFunctionBody(functionDeclaration.getBody(), functionScope);
    }

    private void checkConstructor(
        ClassConstructorDeclaration constructorDeclaration,
        ClassDeclarationStatement classDeclaration,
        SemanticScope classScope
    ) {

        var constructorScope = childScope(classScope, "constructor " + classDeclaration.getName(), constructorDeclaration);
        checkFunctionBody(constructorDeclaration.getBody(), constructorScope);
    }

    private void checkFunctionBody(StatementNode body, SemanticScope functionScope) {

        if (body instanceof BlockStatement block)
            for (var statement : block.getStatements()) checkStatement(statement, functionScope);
        else checkStatement(body, functionScope);
    }

    private void checkBlock(BlockStatement block, SemanticScope scope) {

        var blockScope = childScope(scope, "block", block);
        for (var statement : block.getStatements()) checkStatement(statement, blockScope);
    }

    private void checkBranch(StatementNode statement, SemanticScope scope, String scopeName, AstNode owner) {

        if (statement == null) return;
        if (statement instanceof BlockStatement block) checkBlock(block, scope);
        else checkStatement(statement, childScope(scope, scopeName, owner));
    }

    private void checkInitializer(VariableDeclarationStatement declaration, SemanticScope scope) {

        var valueType = inferExpression(declaration.getInitialValue(), scope);
        if (!isAssignable(declaration.getDeclaredType(), valueType))
            reportTypeMismatch(declaration.getDeclaredType(), valueType, declaration.getInitialValue());
    }

    private ReturnType inferExpression(ExpressionNode expression, SemanticScope scope) {

        if (expression == null) return null;

        if (expression instanceof BoolLiteralExpression) return BOOL_TYPE;
        if (expression instanceof CharLiteralExpression) return new ReturnType(PrimitiveType.CHAR);
        if (expression instanceof StringLiteralExpression) return new ReturnType(PrimitiveType.STRING);
        if (expression instanceof NullLiteralExpression) return null;
        if (expression instanceof NumberLiteralExpression number) return new ReturnType(number.getTypeToken().getType());

        if (expression instanceof IdentifierLiteralExpression identifier) {

            var declaration = firstVisible(scope, identifier.getName());
            return declaration != null ? declaration.getDeclaredType() : null;
        }

        if (expression instanceof ObjectCreationExpression objectCreation) {

            for (var argument : objectCreation.getArguments()) inferExpression(argument, scope);
            return new ReturnType(new NonPrimitiveType(objectCreation.getClassName()));
        }

        if (expression instanceof AssignmentExpression assignment) {

            var targetType = inferExpression(assignment.getTarget(), scope);
            var valueType = inferExpression(assignment.getValue(), scope);
            if (!isAssignable(targetType, valueType)) reportTypeMismatch(targetType, valueType, assignment.getValue());
            return targetType;
        }

        if (expression instanceof BinaryExpression binary) {

            var leftType = inferExpression(binary.getLeft(), scope);
            var rightType = inferExpression(binary.getRight(), scope);
            var operator = binary.getOperator().getType();
            if (operator == Operator.LESS_THAN || operator == Operator.GREATER_THAN ||
                operator == Operator.LESS_EQUAL || operator == Operator.GREATER_EQUAL ||
                operator == Operator.EQUAL || operator == Operator.NOT_EQUAL ||
                operator == Operator.LOGICAL_AND || operator == Operator.LOGICAL_OR)
                return BOOL_TYPE;
            return sameType(leftType, rightType) ? leftType : null;
        }

        if (expression instanceof UnaryExpression unary) return inferExpression(unary.getOperand(), scope);
        if (expression instanceof PostfixUnaryExpression postfix) return inferExpression(postfix.getOperand(), scope);

        if (expression instanceof TernaryExpression ternary) {

            expectBool(ternary.getCondition(), scope, "Ternary condition must be bool");
            var thenType = inferExpression(ternary.getThenExpr(), scope);
            var elseType = inferExpression(ternary.getElseExpr(), scope);
            return sameType(thenType, elseType) ? thenType : null;
        }

        if (expression instanceof CallExpression call) {

            var calleeType = inferExpression(call.getCallee(), scope);
            for (var argument : call.getArguments()) inferExpression(argument, scope);
            return calleeType;
        }

        if (expression instanceof MemberAccessExpression memberAccess) {

            inferExpression(memberAccess.getObject(), scope);
            return null;
        }

        if (expression instanceof ArrayAccessExpression arrayAccess) {

            inferExpression(arrayAccess.getArray(), scope);
            inferExpression(arrayAccess.getIndex(), scope);
            return null;
        }

        return null;
    }

    private void expectBool(ExpressionNode expression, SemanticScope scope, String message) {

        if (expression == null) return;
        var type = inferExpression(expression, scope);
        if (type != null && !sameType(BOOL_TYPE, type)) diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            message,
            expression.getLine(),
            expression.getColumn()
        ));
    }

    private SemanticDeclaration firstVisible(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            var local = current.findLocal(name);
            if (!local.isEmpty()) return local.getFirst();
            current = current.getParent();
        }
        return null;
    }

    private boolean isAssignable(ReturnType targetType, ReturnType valueType) {
        return targetType == null || valueType == null || sameType(targetType, valueType);
    }

    private boolean sameType(ReturnType left, ReturnType right) {

        if (left == null || right == null) return false;
        if (left.getSizes().length != right.getSizes().length) return false;
        if (left.getTokenClass() == null || right.getTokenClass() == null) return false;
        return left.getTokenClass().token().equals(right.getTokenClass().token());
    }

    private SemanticScope childScope(SemanticScope scope, String name, AstNode owner) {

        for (var child : scope.getChildren())
            if (child.getOwner() == owner && child.getName().equals(name)) return child;
        return scope;
    }

    private void reportTypeMismatch(ReturnType targetType, ReturnType valueType, AstNode node) {

        if (node == null) return;
        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Type mismatch: cannot assign " + typeName(valueType) + " to " + typeName(targetType),
            node.getLine(),
            node.getColumn()
        ));
    }

    private String typeName(ReturnType type) {
        return type != null && type.getTokenClass() != null ? type.getTokenClass().token() : "<unknown>";
    }
}
