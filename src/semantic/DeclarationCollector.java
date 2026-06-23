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

import java.util.ArrayList;
import java.util.List;

/// Collects declarations from a parsed AST without resolving names or validating duplicates.
public final class DeclarationCollector {

    private final List<SemanticDeclaration> declarations = new ArrayList<>();

    public DeclarationCollection collect(List<StatementNode> statements) {

        declarations.clear();
        if (statements != null)
            for (var statement : statements) collectStatement(statement, null);
        return new DeclarationCollection(declarations);
    }

    private void collectStatement(StatementNode statement, AstNode owner) {

        if (statement == null) return;

        if (statement instanceof ClassDeclarationStatement classDeclaration) {

            collectClass(classDeclaration, owner);
            return;
        }

        if (statement instanceof ClassMethodDeclaration methodDeclaration) {

            collectFunction(methodDeclaration, owner, DeclarationKind.METHOD);
            return;
        }

        if (statement instanceof FunctionDeclarationStatement functionDeclaration) {

            collectFunction(functionDeclaration, owner, DeclarationKind.FUNCTION);
            return;
        }

        if (statement instanceof ClassFieldDeclaration fieldDeclaration) {

            declare(DeclarationKind.FIELD, fieldDeclaration.getName(), fieldDeclaration.getDeclaredType(), fieldDeclaration, owner);
            return;
        }

        if (statement instanceof VariableDeclarationStatement variableDeclaration) {

            declare(DeclarationKind.VARIABLE, variableDeclaration.getName(), variableDeclaration.getDeclaredType(), variableDeclaration, owner);
            return;
        }

        if (statement instanceof BlockStatement block) {

            for (var child : block.getStatements()) collectStatement(child, block);
            return;
        }

        if (statement instanceof IfStatement ifStatement) {

            collectStatement(ifStatement.getThenBlock(), ifStatement);
            collectStatement(ifStatement.getElseBlock(), ifStatement);
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {

            collectStatement(whileStatement.getBody(), whileStatement);
            return;
        }

        if (statement instanceof ForStatement forStatement) {

            collectStatement(forStatement.getInitialization(), forStatement);
            collectStatement(forStatement.getBody(), forStatement);
            return;
        }

        if (statement instanceof ForEachStatement forEachStatement) {

            declare(
                DeclarationKind.FOREACH_VARIABLE,
                forEachStatement.getElementName(),
                forEachStatement.getElementType(),
                forEachStatement,
                owner
            );
            collectStatement(forEachStatement.getBody(), forEachStatement);
            return;
        }

        if (statement instanceof SwitchStatement switchStatement)
            for (var switchCase : switchStatement.getCases()) collectStatement(switchCase.getBody(), switchCase);
    }

    private void collectClass(ClassDeclarationStatement classDeclaration, AstNode owner) {

        declare(DeclarationKind.CLASS, classDeclaration.getName(), classDeclaration.getReturnType(), classDeclaration, owner);

        for (var field : classDeclaration.getFields()) collectStatement(field, classDeclaration);
        for (var method : classDeclaration.getMethods()) collectStatement(method, classDeclaration);
        for (var constructor : classDeclaration.getConstructors()) collectConstructor(constructor, classDeclaration);
        for (var innerClass : classDeclaration.getInnerClasses()) collectClass(innerClass, classDeclaration);
    }

    private void collectFunction(FunctionDeclarationStatement functionDeclaration, AstNode owner, DeclarationKind kind) {

        declare(kind, functionDeclaration.getName(), functionDeclaration.getDeclaredType(), functionDeclaration, owner);
        for (var parameter : functionDeclaration.getParameters())
            declare(DeclarationKind.PARAMETER, parameter.getName(), parameter.getType(), parameter, functionDeclaration);
        collectStatement(functionDeclaration.getBody(), functionDeclaration);
    }

    private void collectConstructor(ClassConstructorDeclaration constructorDeclaration, ClassDeclarationStatement owner) {

        declare(DeclarationKind.CONSTRUCTOR, owner.getName(), null, constructorDeclaration, owner);
        for (var parameter : constructorDeclaration.getParameters())
            declare(DeclarationKind.PARAMETER, parameter.getName(), parameter.getType(), parameter, constructorDeclaration);
        collectStatement(constructorDeclaration.getBody(), constructorDeclaration);
    }

    private void declare(DeclarationKind kind, String name, ReturnType declaredType, AstNode node, AstNode owner) {
        declarations.add(new SemanticDeclaration(kind, name, declaredType, node, owner));
    }
}
