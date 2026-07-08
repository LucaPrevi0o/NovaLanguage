package semantic.scope;

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
import parser.ast.nodes.type.TypeSyntax;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;

import java.util.List;

/// Builds semantic lexical scopes from a parsed AST without performing validation.
public final class SemanticScopeBuilder {

    /// Builds a semantic scope tree from the given list of statements.
    /// @param statements The list of statements to build the scope tree from.
    /// @return The root semantic scope representing the global scope.
    public SemanticScope build(List<StatementNode> statements) {

        var root = new SemanticScope("global", null, null);
        if (statements != null)
            for (var statement : statements) collectStatement(statement, root);
        return root;
    }

    /// Collects declarations from the given statement and its children, adding them to the provided scope.
    /// @param statement The statement to collect declarations from.
    /// @param scope The semantic scope to add declarations to.
    private void collectStatement(StatementNode statement, SemanticScope scope) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> collectClass(classDeclaration, scope);
            case ClassMethodDeclaration methodDeclaration -> collectFunction(methodDeclaration, scope, DeclarationKind.METHOD, "method " + methodDeclaration.getName());
            case FunctionDeclarationStatement functionDeclaration -> collectFunction(functionDeclaration, scope, DeclarationKind.FUNCTION, "function " + functionDeclaration.getName());
            case ClassFieldDeclaration fieldDeclaration -> declare(scope, DeclarationKind.FIELD, fieldDeclaration.getName(), fieldDeclaration.getDeclaredTypeSyntax(), fieldDeclaration);
            case VariableDeclarationStatement variableDeclaration -> declare(scope, DeclarationKind.VARIABLE, variableDeclaration.getName(), variableDeclaration.getDeclaredTypeSyntax(), variableDeclaration);
            case BlockStatement block -> collectBlock(block, scope);
            case IfStatement ifStatement -> {

                collectBranch(ifStatement.getThenBlock(), scope, "if-then", ifStatement);
                collectBranch(ifStatement.getElseBlock(), scope, "if-else", ifStatement);
            }
            case WhileStatement whileStatement -> collectBranch(whileStatement.getBody(), scope, "while", whileStatement);
            case ForStatement forStatement -> {

                var forScope = scope.createChild("for", forStatement);
                collectStatement(forStatement.getInitialization(), forScope);
                collectBranch(forStatement.getBody(), forScope, "for-body", forStatement);
            }
            case ForEachStatement forEachStatement -> {

                var forEachScope = scope.createChild("for-each", forEachStatement);
                declare(
                        forEachScope,
                        DeclarationKind.FOREACH_VARIABLE,
                        forEachStatement.getElementName(),
                        forEachStatement.getElementTypeSyntax(),
                        forEachStatement
                );
                collectBranch(forEachStatement.getBody(), forEachScope, "for-each-body", forEachStatement);
            }
            case SwitchStatement switchStatement -> {

                var switchScope = scope.createChild("switch", switchStatement);
                for (var switchCase : switchStatement.getCases())
                    collectBranch(switchCase.getBody(), switchScope, "switch-case", switchCase);
            }
            case null, default -> {}
        }

    }

    /// Collects declarations from the given class declaration and its members, adding them to the provided scope.
    /// @param classDeclaration The class declaration to collect declarations from.
    /// @param scope The semantic scope to add declarations to.
    private void collectClass(ClassDeclarationStatement classDeclaration, SemanticScope scope) {

        declare(scope, DeclarationKind.CLASS, classDeclaration.getName(), classDeclaration.getTypeSyntax(), classDeclaration);
        var classScope = scope.createChild("class " + classDeclaration.getName(), classDeclaration);

        for (var field : classDeclaration.getFields()) collectStatement(field, classScope);
        for (var method : classDeclaration.getMethods()) collectStatement(method, classScope);
        for (var constructor : classDeclaration.getConstructors()) collectConstructor(constructor, classDeclaration, classScope);
        for (var innerClass : classDeclaration.getInnerClasses()) collectClass(innerClass, classScope);
    }

    /// Collects declarations from the given function declaration and its body, adding them to the provided scope.
    /// @param functionDeclaration The function declaration to collect declarations from.
    /// @param scope The semantic scope to add declarations to.
    /// @param kind The kind of the function declaration (e.g., `METHOD` or `FUNCTION`).
    /// @param scopeName The name of the scope to create for the function body.
    private void collectFunction(FunctionDeclarationStatement functionDeclaration, SemanticScope scope, DeclarationKind kind, String scopeName) {

        declare(scope, kind, functionDeclaration.getName(), functionDeclaration.getDeclaredTypeSyntax(), functionDeclaration);
        var functionScope = scope.createChild(scopeName, functionDeclaration);
        for (var parameter : functionDeclaration.getParameters())
            declare(functionScope, DeclarationKind.PARAMETER, parameter.getName(), parameter.getTypeSyntax(), parameter);
        collectFunctionBody(functionDeclaration.getBody(), functionScope);
    }

    /// Collects declarations from the given constructor declaration and its body, adding them to the provided scope.
    /// @param constructorDeclaration The constructor declaration to collect declarations from.
    /// @param classDeclaration The class declaration that owns the constructor.
    /// @param classScope The semantic scope of the class that owns the constructor.
    private void collectConstructor(ClassConstructorDeclaration constructorDeclaration, ClassDeclarationStatement classDeclaration, SemanticScope classScope) {

        declare(classScope, DeclarationKind.CONSTRUCTOR, classDeclaration.getName(), null, constructorDeclaration);
        var constructorScope = classScope.createChild("constructor " + classDeclaration.getName(), constructorDeclaration);
        for (var parameter : constructorDeclaration.getParameters())
            declare(constructorScope, DeclarationKind.PARAMETER, parameter.getName(), parameter.getTypeSyntax(), parameter);
        collectFunctionBody(constructorDeclaration.getBody(), constructorScope);
    }

    /// Collects declarations from the body of a function or constructor, adding them to the provided scope.
    /// @param body The body of the function or constructor to collect declarations from.
    /// @param functionScope The semantic scope of the function or constructor body.
    private void collectFunctionBody(StatementNode body, SemanticScope functionScope) {

        if (body instanceof BlockStatement block)
            for (var statement : block.getStatements()) collectStatement(statement, functionScope);
        else collectStatement(body, functionScope);
    }

    /// Collects declarations from the given block statement and its statements, adding them to the provided scope.
    /// @param block The block statement to collect declarations from.
    /// @param scope The semantic scope to add declarations to.
    private void collectBlock(BlockStatement block, SemanticScope scope) {

        var blockScope = scope.createChild("block", block);
        for (var statement : block.getStatements()) collectStatement(statement, blockScope);
    }

    /// Collects declarations from the given statement and its children, adding them to a new child scope of the provided scope.
    /// @param statement The statement to collect declarations from.
    /// @param scope The semantic scope to add declarations to.
    /// @param scopeName The name of the new child scope to create for the statement.
    /// @param owner The AST node that owns the statement, or `null` if the statement has no owner.
    private void collectBranch(StatementNode statement, SemanticScope scope, String scopeName, AstNode owner) {

        if (statement == null) return;
        if (statement instanceof BlockStatement block) collectBlock(block, scope);
        else collectStatement(statement, scope.createChild(scopeName, owner));
    }

    /// Declares a new semantic declaration and adds it to the provided scope.
    /// @param scope The semantic scope to add the declaration to.
    /// @param kind The kind of the declaration (e.g., `FUNCTION`, `VARIABLE`, `CLASS`, etc.).
    /// @param name The name of the declaration.
    /// @param declaredTypeSyntax The parsed source type syntax for the declaration, or `null` if absent.
    /// @param node The AST node that represents the declaration.
    private void declare(SemanticScope scope, DeclarationKind kind, String name, TypeSyntax declaredTypeSyntax, AstNode node) {

        var declaration = new SemanticDeclaration(kind, name, declaredTypeSyntax, node, scope.getOwner());
        scope.declare(declaration);
    }
}
