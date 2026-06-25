package semantic.declaration;

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
import parser.ast.nodes.type.TypeSyntax;

import java.util.ArrayList;
import java.util.List;

/// Collects declarations from a parsed AST without resolving names or validating duplicates.
public final class DeclarationCollector {

    private final List<SemanticDeclaration> declarations = new ArrayList<>();

    /// Collects declarations from the given list of statements.
    /// @param statements The list of statements to collect declarations from.
    /// @return A collection of declarations found in the statements.
    public DeclarationCollection collect(List<StatementNode> statements) {

        declarations.clear();
        if (statements != null)
            for (var statement : statements) collectStatement(statement, null);
        return new DeclarationCollection(declarations);
    }

    /// Collects declarations from the given statement and its children.
    /// @param statement The statement to collect declarations from.
    /// @param owner The owner of the statement, or null if the statement has no owner.
    private void collectStatement(StatementNode statement, AstNode owner) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> collectClass(classDeclaration, owner);
            case ClassMethodDeclaration methodDeclaration -> collectFunction(methodDeclaration, owner, DeclarationKind.METHOD);
            case FunctionDeclarationStatement functionDeclaration -> collectFunction(functionDeclaration, owner, DeclarationKind.FUNCTION);
            case ClassFieldDeclaration fieldDeclaration -> declare(DeclarationKind.FIELD, fieldDeclaration.getName(), fieldDeclaration.getDeclaredType(), fieldDeclaration.getDeclaredTypeSyntax(), fieldDeclaration, owner);
            case VariableDeclarationStatement variableDeclaration -> declare(DeclarationKind.VARIABLE, variableDeclaration.getName(), variableDeclaration.getDeclaredType(), variableDeclaration.getDeclaredTypeSyntax(), variableDeclaration, owner);
            case BlockStatement block -> {
                for (var child : block.getStatements()) collectStatement(child, block);
            }
            case IfStatement ifStatement -> {

                collectStatement(ifStatement.getThenBlock(), ifStatement);
                collectStatement(ifStatement.getElseBlock(), ifStatement);
            }
            case WhileStatement whileStatement -> collectStatement(whileStatement.getBody(), whileStatement);
            case ForStatement forStatement -> {

                collectStatement(forStatement.getInitialization(), forStatement);
                collectStatement(forStatement.getBody(), forStatement);
            }
            case ForEachStatement forEachStatement -> {

                declare(
                        DeclarationKind.FOREACH_VARIABLE,
                        forEachStatement.getElementName(),
                        forEachStatement.getElementType(),
                        forEachStatement.getElementTypeSyntax(),
                        forEachStatement,
                        owner
                );
                collectStatement(forEachStatement.getBody(), forEachStatement);
            }
            case SwitchStatement switchStatement -> {
                for (var switchCase : switchStatement.getCases()) collectStatement(switchCase.getBody(), switchCase);
            }
            case null, default -> {}
        }
    }

    /// Collects declarations from the given class declaration and its members.
    /// @param classDeclaration The class declaration to collect declarations from.
    /// @param owner The owner of the class declaration, or null if the class has no owner.
    private void collectClass(ClassDeclarationStatement classDeclaration, AstNode owner) {

        declare(DeclarationKind.CLASS, classDeclaration.getName(), classDeclaration.getReturnType(), classDeclaration.getTypeSyntax(), classDeclaration, owner);

        for (var field : classDeclaration.getFields()) collectStatement(field, classDeclaration);
        for (var method : classDeclaration.getMethods()) collectStatement(method, classDeclaration);
        for (var constructor : classDeclaration.getConstructors()) collectConstructor(constructor, classDeclaration);
        for (var innerClass : classDeclaration.getInnerClasses()) collectClass(innerClass, classDeclaration);
    }

    /// Collects declarations from the given function declaration and its parameters.
    /// @param functionDeclaration The function declaration to collect declarations from.
    /// @param owner The owner of the function declaration, or `null` if the function has no owner.
    /// @param kind The kind of the function declaration (e.g., `FUNCTION` or `METHOD`).
    private void collectFunction(FunctionDeclarationStatement functionDeclaration, AstNode owner, DeclarationKind kind) {

        declare(kind, functionDeclaration.getName(), functionDeclaration.getDeclaredType(), functionDeclaration.getDeclaredTypeSyntax(), functionDeclaration, owner);
        for (var parameter : functionDeclaration.getParameters())
            declare(DeclarationKind.PARAMETER, parameter.getName(), parameter.getType(), parameter.getTypeSyntax(), parameter, functionDeclaration);
        collectStatement(functionDeclaration.getBody(), functionDeclaration);
    }

    /// Collects declarations from the given constructor declaration and its parameters.
    /// @param constructorDeclaration The constructor declaration to collect declarations from.
    /// @param owner The owner of the constructor declaration, or `null` if the constructor has no owner.
    private void collectConstructor(ClassConstructorDeclaration constructorDeclaration, ClassDeclarationStatement owner) {

        declare(DeclarationKind.CONSTRUCTOR, owner.getName(), null, constructorDeclaration, owner);
        for (var parameter : constructorDeclaration.getParameters())
            declare(DeclarationKind.PARAMETER, parameter.getName(), parameter.getType(), parameter.getTypeSyntax(), parameter, constructorDeclaration);
        collectStatement(constructorDeclaration.getBody(), constructorDeclaration);
    }

    /// Declares a new semantic declaration and adds it to the list of declarations.
    /// @param kind The kind of the declaration (e.g., `FUNCTION`, `VARIABLE`, `CLASS`, etc.).
    /// @param name The name of the declaration.
    /// @param declaredType The ReturnType adapter for the declaration, or `null` if absent.
    /// @param declaredTypeSyntax The parsed source type syntax for the declaration, or `null` if absent.
    /// @param node The AST node that represents the declaration.
    /// @param owner The owner of the declaration, or `null` if the declaration has no owner.
    private void declare(DeclarationKind kind, String name, ReturnType declaredType, TypeSyntax declaredTypeSyntax, AstNode node, AstNode owner) {
        declarations.add(new SemanticDeclaration(kind, name, declaredType, declaredTypeSyntax, node, owner));
    }

    /// Declares a new semantic declaration without a source type.
    /// @param kind The kind of the declaration.
    /// @param name The name of the declaration.
    /// @param declaredType The ReturnType adapter for the declaration, or `null` if absent.
    /// @param node The AST node that represents the declaration.
    /// @param owner The owner of the declaration, or `null` if the declaration has no owner.
    private void declare(DeclarationKind kind, String name, ReturnType declaredType, AstNode node, AstNode owner) {
        declarations.add(new SemanticDeclaration(kind, name, declaredType, node, owner));
    }
}
