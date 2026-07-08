package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
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
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;
import semantic.type.TypeResolution;
import semantic.type.TypeResolver;

import java.util.List;

/// Resolves expression names against semantic scopes and reports unresolved references.
public final class NameResolver {

    private final DiagnosticBag diagnostics = new DiagnosticBag();
    private final TypeResolver typeResolver = new TypeResolver();
    private SemanticScope rootScope;

    /// Resolves a list of statements, building semantic scopes and checking for unresolved names.
    /// @param statements The list of statements to resolve.
    /// @return A list of diagnostics for any unresolved names found.
    public List<Diagnostic> resolve(List<StatementNode> statements) {

        diagnostics.clear();
        rootScope = new SemanticScopeBuilder().build(statements);
        if (statements != null)
            for (var statement : statements) resolveStatement(statement, rootScope);
        return diagnostics.getDiagnostics();
    }

    /// Returns the root semantic scope built during resolution.
    /// @return The root semantic scope.
    public SemanticScope getRootScope() { return rootScope; }

    /// Resolves a single statement, checking for unresolved names and building child scopes as needed.
    /// @param statement The statement to resolve.
    /// @param scope The current semantic scope in which to resolve names.
    private void resolveStatement(StatementNode statement, SemanticScope scope) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> resolveClass(classDeclaration, scope);
            case ClassMethodDeclaration methodDeclaration -> resolveFunction(methodDeclaration, scope, "method " + methodDeclaration.getName());
            case FunctionDeclarationStatement functionDeclaration -> resolveFunction(functionDeclaration, scope, "function " + functionDeclaration.getName());
            case VariableDeclarationStatement variableDeclaration -> {

                resolveType(variableDeclaration.getDeclaredTypeSyntax(), variableDeclaration, scope);
                resolveExpression(variableDeclaration.getInitialValue(), scope);
            }
            case BlockStatement block -> resolveBlock(block, scope);
            case ExpressionStatement expressionStatement -> resolveExpression(expressionStatement.getExpression(), scope);
            case ReturnStatement returnStatement -> resolveExpression(returnStatement.getReturnValue(), scope);
            case IfStatement ifStatement -> {

                resolveExpression(ifStatement.getCondition(), scope);
                resolveBranch(ifStatement.getThenBlock(), scope, "if-then", ifStatement);
                resolveBranch(ifStatement.getElseBlock(), scope, "if-else", ifStatement);
            }
            case WhileStatement whileStatement -> {

                resolveExpression(whileStatement.getCondition(), scope);
                resolveBranch(whileStatement.getBody(), scope, "while", whileStatement);
            }
            case ForStatement forStatement -> {

                var forScope = childScope(scope, "for", forStatement);
                resolveStatement(forStatement.getInitialization(), forScope);
                resolveExpression(forStatement.getCondition(), forScope);
                resolveStatement(forStatement.getIncrement(), forScope);
                resolveBranch(forStatement.getBody(), forScope, "for-body", forStatement);
            }
            case ForEachStatement forEachStatement -> {

                resolveType(forEachStatement.getElementTypeSyntax(), forEachStatement, scope);
                resolveExpression(forEachStatement.getIterable(), scope);
                var forEachScope = childScope(scope, "for-each", forEachStatement);
                resolveBranch(forEachStatement.getBody(), forEachScope, "for-each-body", forEachStatement);
            }
            case SwitchStatement switchStatement -> {

                resolveExpression(switchStatement.getSubject(), scope);
                var switchScope = childScope(scope, "switch", switchStatement);
                for (var switchCase : switchStatement.getCases()) {

                    resolveExpression(switchCase.getValue(), scope);
                    resolveBranch(switchCase.getBody(), switchScope, "switch-case", switchCase);
                }
            }
            case null, default -> {}
        }

    }

    /// Resolves a class declaration, checking for unresolved superclass names and resolving its members in a child scope.
    /// @param classDeclaration The class declaration to resolve.
    /// @param scope The current semantic scope in which to resolve names.
    private void resolveClass(ClassDeclarationStatement classDeclaration, SemanticScope scope) {

        var superClassSyntaxes = classDeclaration.getSuperClassSyntaxes();
        for (var superClassSyntax : superClassSyntaxes)
            report(typeResolver.resolve(superClassSyntax, classDeclaration, scope, "superclass"));

        var classScope = childScope(scope, "class " + classDeclaration.getName(), classDeclaration);

        for (var field : classDeclaration.getFields()) resolveStatement(field, classScope);
        for (var method : classDeclaration.getMethods()) resolveStatement(method, classScope);
        for (var constructor : classDeclaration.getConstructors()) resolveConstructor(constructor, classDeclaration, classScope);
        for (var innerClass : classDeclaration.getInnerClasses()) resolveClass(innerClass, classScope);
    }

    /// Resolves a function or method declaration, checking for unresolved parameter and return types, and resolving its body in a child scope.
    /// @param functionDeclaration The function or method declaration to resolve.
    /// @param scope The current semantic scope in which to resolve names.
    /// @param scopeName The name to use for the child scope of the function or method.
    private void resolveFunction(FunctionDeclarationStatement functionDeclaration, SemanticScope scope, String scopeName) {

        resolveType(functionDeclaration.getDeclaredTypeSyntax(), functionDeclaration, scope);
        var functionScope = childScope(scope, scopeName, functionDeclaration);
        for (var parameter : functionDeclaration.getParameters())
            resolveType(parameter.getTypeSyntax(), parameter, functionScope);
        resolveFunctionBody(functionDeclaration.getBody(), functionScope);
    }

    /// Resolves a class constructor declaration, checking for unresolved parameter types and resolving its body in a child scope.
    /// @param constructorDeclaration The class constructor declaration to resolve.
    /// @param classDeclaration The class declaration that owns the constructor.
    /// @param classScope The semantic scope of the class in which to resolve names.
    private void resolveConstructor(ClassConstructorDeclaration constructorDeclaration, ClassDeclarationStatement classDeclaration, SemanticScope classScope) {

        var constructorScope = childScope(classScope, "constructor " + classDeclaration.getName(), constructorDeclaration);
        for (var parameter : constructorDeclaration.getParameters())
            resolveType(parameter.getTypeSyntax(), parameter, constructorScope);
        resolveFunctionBody(constructorDeclaration.getBody(), constructorScope);
    }

    /// Resolves the body of a function or constructor, which may be a block or a single statement, in the given semantic scope.
    /// @param body The body of the function or constructor to resolve.
    /// @param functionScope The semantic scope in which to resolve names.
    private void resolveFunctionBody(StatementNode body, SemanticScope functionScope) {

        if (body instanceof BlockStatement block)
            for (var statement : block.getStatements()) resolveStatement(statement, functionScope);
        else resolveStatement(body, functionScope);
    }

    /// Resolves a block statement, checking for unresolved names in its statements and creating a child scope for the block.
    /// @param block The block statement to resolve.
    /// @param scope The current semantic scope in which to resolve names.
    private void resolveBlock(BlockStatement block, SemanticScope scope) {

        var blockScope = childScope(scope, "block", block);
        for (var statement : block.getStatements()) resolveStatement(statement, blockScope);
    }

    /// Resolves a branch statement, which may be a block or a single statement, in the given semantic scope.
    /// @param statement The branch statement to resolve.
    /// @param scope The current semantic scope in which to resolve names.
    /// @param scopeName The name to use for the child scope of the branch.
    /// @param owner The AST node that owns the branch statement, used for error reporting.
    private void resolveBranch(StatementNode statement, SemanticScope scope, String scopeName, AstNode owner) {

        if (statement == null) return;
        if (statement instanceof BlockStatement block) resolveBlock(block, scope);
        else resolveStatement(statement, childScope(scope, scopeName, owner));
    }

    /// Resolves an expression, checking for unresolved identifiers and recursively resolving sub-expressions in the given semantic scope.
    /// @param expression The expression to resolve.
    /// @param scope The current semantic scope in which to resolve names.
    private void resolveExpression(ExpressionNode expression, SemanticScope scope) {

        switch (expression) {

            case IdentifierLiteralExpression identifier -> {

                if (findVisible(scope, identifier.getName()).isEmpty())
                    report("Undefined name '" + identifier.getName() + "'", identifier);
            }
            case ObjectCreationExpression objectCreation -> {

                report(typeResolver.resolveClassName(objectCreation.getClassName(), objectCreation, scope, "class"));
                for (var argument : objectCreation.getArguments()) resolveExpression(argument, scope);
            }
            case AssignmentExpression assignment -> {

                resolveExpression(assignment.getTarget(), scope);
                resolveExpression(assignment.getValue(), scope);
            }
            case BinaryExpression binary -> {

                resolveExpression(binary.getLeft(), scope);
                resolveExpression(binary.getRight(), scope);
            }
            case UnaryExpression unary -> resolveExpression(unary.getOperand(), scope);
            case PostfixUnaryExpression postfix -> resolveExpression(postfix.getOperand(), scope);
            case TernaryExpression ternary -> {

                resolveExpression(ternary.getCondition(), scope);
                resolveExpression(ternary.getThenExpr(), scope);
                resolveExpression(ternary.getElseExpr(), scope);
            }
            case CallExpression call -> {

                resolveExpression(call.getCallee(), scope);
                for (var argument : call.getArguments()) resolveExpression(argument, scope);
            }
            case MemberAccessExpression memberAccess -> resolveExpression(memberAccess.getObject(), scope);
            case ArrayAccessExpression arrayAccess -> {

                resolveExpression(arrayAccess.getArray(), scope);
                resolveExpression(arrayAccess.getIndex(), scope);
            }
            case null, default -> {}
        }

    }

    /// Resolves a declared type, checking for unresolved types and resolving any size expressions in the given semantic scope.
    /// @param typeSyntax The parsed source type syntax to resolve.
    /// @param owner The AST node that owns the type, used for error reporting.
    /// @param scope The current semantic scope in which to resolve names.
    private void resolveType(TypeSyntax typeSyntax, AstNode owner, SemanticScope scope) {

        if (typeSyntax == null) return;
        resolveTypeSizeExpressions(typeSyntax, scope);
        report(typeResolver.resolve(typeSyntax, owner, scope));
    }

    /// Resolves expressions used in array type dimensions.
    /// @param typeSyntax The parsed source type syntax.
    /// @param scope The current semantic scope in which to resolve names.
    private void resolveTypeSizeExpressions(TypeSyntax typeSyntax, SemanticScope scope) {

        if (typeSyntax instanceof ArrayTypeSyntax arrayType) {

            for (var size : arrayType.getSizes()) resolveExpression(size, scope);
        }
    }

    /// Finds visible declarations with the given name in the current semantic scope and its parent scopes.
    /// @param scope The current semantic scope in which to search for declarations.
    /// @param name The name of the declaration to find.
    /// @return A list of visible semantic declarations with the given name, or an empty list if none are found.
    private List<SemanticDeclaration> findVisible(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            var local = current.findLocal(name);
            if (!local.isEmpty()) return local;
            current = current.getParent();
        }
        return List.of();
    }

    /// Returns a child semantic scope with the given name and owner, or the current scope if no matching child is found.
    /// @param scope The current semantic scope in which to search for a child scope.
    /// @param name The name of the child scope to find.
    /// @param owner The AST node that owns the child scope, used for matching.
    /// @return The matching child semantic scope, or the current scope if no match is found.
    private SemanticScope childScope(SemanticScope scope, String name, AstNode owner) {

        for (var child : scope.getChildren())
            if (child.getOwner() == owner && child.getName().equals(name)) return child;
        return scope;
    }

    /// Reports a diagnostic for an unresolved name or type.
    /// @param message The diagnostic message to report.
    /// @param node The AST node associated with the unresolved name or type.
    private void report(String message, AstNode node) {
        diagnostics.report(Diagnostic.error(DiagnosticPhase.SEMANTIC, message, node.getLine(), node.getColumn()));
    }

    /// Reports diagnostics produced by semantic type resolution.
    /// @param resolution The type-resolution result.
    private void report(TypeResolution resolution) {
        for (var diagnostic : resolution.diagnostics()) diagnostics.report(diagnostic);
    }
}
