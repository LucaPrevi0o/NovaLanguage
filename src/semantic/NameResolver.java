package semantic;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
import lexer.token.family.GenericParameterType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
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
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;

import java.util.List;

/// Resolves expression names against semantic scopes and reports unresolved references.
public final class NameResolver {

    private final DiagnosticBag diagnostics = new DiagnosticBag();
    private SemanticScope rootScope;

    public List<Diagnostic> resolve(List<StatementNode> statements) {

        diagnostics.clear();
        rootScope = new SemanticScopeBuilder().build(statements);
        if (statements != null)
            for (var statement : statements) resolveStatement(statement, rootScope);
        return diagnostics.getDiagnostics();
    }

    public SemanticScope getRootScope() { return rootScope; }

    private void resolveStatement(StatementNode statement, SemanticScope scope) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            resolveClass(classDeclaration, scope);
            return;
        }

        if (statement instanceof ClassMethodDeclaration methodDeclaration) {

            resolveFunction(methodDeclaration, scope, "method " + methodDeclaration.getName());
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            resolveFunction(functionDeclaration, scope, "function " + functionDeclaration.getName());
            return;
        }

        if (statement instanceof VariableDeclarationStatement variableDeclaration) {

            resolveType(variableDeclaration.getDeclaredType(), variableDeclaration, scope);
            resolveExpression(variableDeclaration.getInitialValue(), scope);
            return;
        }

        if (statement instanceof BlockStatement block) {

            resolveBlock(block, scope);
            return;
        }

        if (statement instanceof ExpressionStatement expressionStatement) {

            resolveExpression(expressionStatement.getExpression(), scope);
            return;
        }

        if (statement instanceof ReturnStatement returnStatement) {

            resolveExpression(returnStatement.getReturnValue(), scope);
            return;
        }

        if (statement instanceof IfStatement ifStatement) {

            resolveExpression(ifStatement.getCondition(), scope);
            resolveBranch(ifStatement.getThenBlock(), scope, "if-then", ifStatement);
            resolveBranch(ifStatement.getElseBlock(), scope, "if-else", ifStatement);
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {

            resolveExpression(whileStatement.getCondition(), scope);
            resolveBranch(whileStatement.getBody(), scope, "while", whileStatement);
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            var forScope = childScope(scope, "for", forStatement);
            resolveStatement(forStatement.getInitialization(), forScope);
            resolveExpression(forStatement.getCondition(), forScope);
            resolveStatement(forStatement.getIncrement(), forScope);
            resolveBranch(forStatement.getBody(), forScope, "for-body", forStatement);
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            resolveType(forEachStatement.getElementType(), forEachStatement, scope);
            resolveExpression(forEachStatement.getIterable(), scope);
            var forEachScope = childScope(scope, "for-each", forEachStatement);
            resolveBranch(forEachStatement.getBody(), forEachScope, "for-each-body", forEachStatement);
            return;
        }

        if (statement instanceof SwitchStatement switchStatement) {

            resolveExpression(switchStatement.getSubject(), scope);
            var switchScope = childScope(scope, "switch", switchStatement);
            for (var switchCase : switchStatement.getCases()) {
                resolveExpression(switchCase.getValue(), scope);
                resolveBranch(switchCase.getBody(), switchScope, "switch-case", switchCase);
            }
        }
    }

    private void resolveClass(ClassDeclarationStatement classDeclaration, SemanticScope scope) {

        for (var superClass : classDeclaration.getSuperClasses()) {

            var superClassName = superClass.getTokenClass().token();
            if (!hasVisibleClass(scope, superClassName))
                report("Undefined superclass '" + superClassName + "'", classDeclaration);
        }

        var classScope = childScope(scope, "class " + classDeclaration.getName(), classDeclaration);

        for (var field : classDeclaration.getFields()) resolveStatement(field, classScope);
        for (var method : classDeclaration.getMethods()) resolveStatement(method, classScope);
        for (var constructor : classDeclaration.getConstructors()) resolveConstructor(constructor, classDeclaration, classScope);
        for (var innerClass : classDeclaration.getInnerClasses()) resolveClass(innerClass, classScope);
    }

    private void resolveFunction(FunctionDeclarationStatement functionDeclaration, SemanticScope scope, String scopeName) {

        resolveType(functionDeclaration.getDeclaredType(), functionDeclaration, scope);
        var functionScope = childScope(scope, scopeName, functionDeclaration);
        for (var parameter : functionDeclaration.getParameters())
            resolveType(parameter.getType(), parameter, functionScope);
        resolveFunctionBody(functionDeclaration.getBody(), functionScope);
    }

    private void resolveConstructor(
        ClassConstructorDeclaration constructorDeclaration,
        ClassDeclarationStatement classDeclaration,
        SemanticScope classScope
    ) {

        var constructorScope = childScope(classScope, "constructor " + classDeclaration.getName(), constructorDeclaration);
        for (var parameter : constructorDeclaration.getParameters())
            resolveType(parameter.getType(), parameter, constructorScope);
        resolveFunctionBody(constructorDeclaration.getBody(), constructorScope);
    }

    private void resolveFunctionBody(StatementNode body, SemanticScope functionScope) {

        if (body instanceof BlockStatement block)
            for (var statement : block.getStatements()) resolveStatement(statement, functionScope);
        else resolveStatement(body, functionScope);
    }

    private void resolveBlock(BlockStatement block, SemanticScope scope) {

        var blockScope = childScope(scope, "block", block);
        for (var statement : block.getStatements()) resolveStatement(statement, blockScope);
    }

    private void resolveBranch(StatementNode statement, SemanticScope scope, String scopeName, AstNode owner) {

        if (statement == null) return;
        if (statement instanceof BlockStatement block) resolveBlock(block, scope);
        else resolveStatement(statement, childScope(scope, scopeName, owner));
    }

    private void resolveExpression(ExpressionNode expression, SemanticScope scope) {

        if (expression == null) return;

        if (expression instanceof IdentifierLiteralExpression identifier) {

            if (findVisible(scope, identifier.getName()).isEmpty())
                report("Undefined name '" + identifier.getName() + "'", identifier);
            return;
        }

        if (expression instanceof ObjectCreationExpression objectCreation) {

            if (!hasVisibleClass(scope, objectCreation.getClassName()))
                report("Undefined class '" + objectCreation.getClassName() + "'", objectCreation);
            for (var argument : objectCreation.getArguments()) resolveExpression(argument, scope);
            return;
        }

        if (expression instanceof AssignmentExpression assignment) {

            resolveExpression(assignment.getTarget(), scope);
            resolveExpression(assignment.getValue(), scope);
            return;
        }

        if (expression instanceof BinaryExpression binary) {

            resolveExpression(binary.getLeft(), scope);
            resolveExpression(binary.getRight(), scope);
            return;
        }

        if (expression instanceof UnaryExpression unary) {

            resolveExpression(unary.getOperand(), scope);
            return;
        }

        if (expression instanceof PostfixUnaryExpression postfix) {

            resolveExpression(postfix.getOperand(), scope);
            return;
        }

        if (expression instanceof TernaryExpression ternary) {

            resolveExpression(ternary.getCondition(), scope);
            resolveExpression(ternary.getThenExpr(), scope);
            resolveExpression(ternary.getElseExpr(), scope);
            return;
        }

        if (expression instanceof CallExpression call) {

            resolveExpression(call.getCallee(), scope);
            for (var argument : call.getArguments()) resolveExpression(argument, scope);
            return;
        }

        if (expression instanceof MemberAccessExpression memberAccess) {

            resolveExpression(memberAccess.getObject(), scope);
            return;
        }

        if (expression instanceof ArrayAccessExpression arrayAccess) {

            resolveExpression(arrayAccess.getArray(), scope);
            resolveExpression(arrayAccess.getIndex(), scope);
        }
    }

    private void resolveType(ReturnType type, AstNode owner, SemanticScope scope) {

        if (type == null) return;
        for (var size : type.getSizes()) resolveExpression(size, scope);

        var tokenClass = type.getTokenClass();
        if (tokenClass instanceof PrimitiveType || tokenClass instanceof GenericParameterType) return;

        if (tokenClass instanceof NonPrimitiveType nonPrimitiveType &&
            !hasVisibleClass(scope, nonPrimitiveType.typeName()))
            report("Undefined type '" + nonPrimitiveType.typeName() + "'", owner);
    }

    private List<SemanticDeclaration> findVisible(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            var local = current.findLocal(name);
            if (!local.isEmpty()) return local;
            current = current.getParent();
        }
        return List.of();
    }

    private boolean hasVisibleClass(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            for (var declaration : current.findLocal(name))
                if (declaration.getKind() == DeclarationKind.CLASS) return true;
            current = current.getParent();
        }
        return false;
    }

    private SemanticScope childScope(SemanticScope scope, String name, AstNode owner) {

        for (var child : scope.getChildren())
            if (child.getOwner() == owner && child.getName().equals(name)) return child;
        return scope;
    }

    private void report(String message, AstNode node) {
        diagnostics.report(Diagnostic.error(DiagnosticPhase.SEMANTIC, message, node.getLine(), node.getColumn()));
    }
}
