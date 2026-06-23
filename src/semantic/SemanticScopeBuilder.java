package semantic;

import lexer.token.ReturnType;
import parser.ast.AstNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
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

/// Builds semantic lexical scopes from a parsed AST without performing validation.
public final class SemanticScopeBuilder {

    public SemanticScope build(List<StatementNode> statements) {

        var root = new SemanticScope("global", null, null);
        if (statements != null)
            for (var statement : statements) collectStatement(statement, root);
        return root;
    }

    private void collectStatement(StatementNode statement, SemanticScope scope) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            collectClass(classDeclaration, scope);
            return;
        }

        if (statement instanceof ClassMethodDeclaration methodDeclaration) {

            collectFunction(methodDeclaration, scope, DeclarationKind.METHOD, "method " + methodDeclaration.getName());
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            collectFunction(functionDeclaration, scope, DeclarationKind.FUNCTION, "function " + functionDeclaration.getName());
            return;
        }

        if (statement instanceof ClassFieldDeclaration fieldDeclaration) {

            declare(scope, DeclarationKind.FIELD, fieldDeclaration.getName(), fieldDeclaration.getDeclaredType(), fieldDeclaration);
            return;
        }

        if (statement instanceof VariableDeclarationStatement variableDeclaration) {

            declare(scope, DeclarationKind.VARIABLE, variableDeclaration.getName(), variableDeclaration.getDeclaredType(), variableDeclaration);
            return;
        }

        if (statement instanceof BlockStatement block) {

            collectBlock(block, scope);
            return;
        }

        if (statement instanceof IfStatement ifStatement) {

            collectBranch(ifStatement.getThenBlock(), scope, "if-then", ifStatement);
            collectBranch(ifStatement.getElseBlock(), scope, "if-else", ifStatement);
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {

            collectBranch(whileStatement.getBody(), scope, "while", whileStatement);
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            var forScope = scope.createChild("for", forStatement);
            collectStatement(forStatement.getInitialization(), forScope);
            collectBranch(forStatement.getBody(), forScope, "for-body", forStatement);
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            var forEachScope = scope.createChild("for-each", forEachStatement);
            declare(
                forEachScope,
                DeclarationKind.FOREACH_VARIABLE,
                forEachStatement.getElementName(),
                forEachStatement.getElementType(),
                forEachStatement
            );
            collectBranch(forEachStatement.getBody(), forEachScope, "for-each-body", forEachStatement);
            return;
        }

        if (statement instanceof SwitchStatement switchStatement) {

            var switchScope = scope.createChild("switch", switchStatement);
            for (var switchCase : switchStatement.getCases())
                collectBranch(switchCase.getBody(), switchScope, "switch-case", switchCase);
        }
    }

    private void collectClass(ClassDeclarationStatement classDeclaration, SemanticScope scope) {

        declare(scope, DeclarationKind.CLASS, classDeclaration.getName(), classDeclaration.getReturnType(), classDeclaration);
        var classScope = scope.createChild("class " + classDeclaration.getName(), classDeclaration);

        for (var field : classDeclaration.getFields()) collectStatement(field, classScope);
        for (var method : classDeclaration.getMethods()) collectStatement(method, classScope);
        for (var constructor : classDeclaration.getConstructors()) collectConstructor(constructor, classDeclaration, classScope);
        for (var innerClass : classDeclaration.getInnerClasses()) collectClass(innerClass, classScope);
    }

    private void collectFunction(
        FunctionDeclarationStatement functionDeclaration,
        SemanticScope scope,
        DeclarationKind kind,
        String scopeName
    ) {

        declare(scope, kind, functionDeclaration.getName(), functionDeclaration.getDeclaredType(), functionDeclaration);
        var functionScope = scope.createChild(scopeName, functionDeclaration);
        for (var parameter : functionDeclaration.getParameters())
            declare(functionScope, DeclarationKind.PARAMETER, parameter.getName(), parameter.getType(), parameter);
        collectFunctionBody(functionDeclaration.getBody(), functionScope);
    }

    private void collectConstructor(
        ClassConstructorDeclaration constructorDeclaration,
        ClassDeclarationStatement classDeclaration,
        SemanticScope classScope
    ) {

        declare(classScope, DeclarationKind.CONSTRUCTOR, classDeclaration.getName(), null, constructorDeclaration);
        var constructorScope = classScope.createChild("constructor " + classDeclaration.getName(), constructorDeclaration);
        for (var parameter : constructorDeclaration.getParameters())
            declare(constructorScope, DeclarationKind.PARAMETER, parameter.getName(), parameter.getType(), parameter);
        collectFunctionBody(constructorDeclaration.getBody(), constructorScope);
    }

    private void collectFunctionBody(StatementNode body, SemanticScope functionScope) {

        if (body instanceof BlockStatement block)
            for (var statement : block.getStatements()) collectStatement(statement, functionScope);
        else collectStatement(body, functionScope);
    }

    private void collectBlock(BlockStatement block, SemanticScope scope) {

        var blockScope = scope.createChild("block", block);
        for (var statement : block.getStatements()) collectStatement(statement, blockScope);
    }

    private void collectBranch(StatementNode statement, SemanticScope scope, String scopeName, AstNode owner) {

        if (statement == null) return;
        if (statement instanceof BlockStatement block) collectBlock(block, scope);
        else collectStatement(statement, scope.createChild(scopeName, owner));
    }

    private SemanticDeclaration declare(
        SemanticScope scope,
        DeclarationKind kind,
        String name,
        ReturnType declaredType,
        AstNode node
    ) {

        var declaration = new SemanticDeclaration(kind, name, declaredType, node, scope.getOwner());
        scope.declare(declaration);
        return declaration;
    }
}
