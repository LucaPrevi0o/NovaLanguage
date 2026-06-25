package semantic.analysis;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
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
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;
import semantic.scope.SemanticScopeBuilder;
import semantic.type.ArrayTypeSymbol;
import semantic.type.ClassTypeSymbol;
import semantic.type.GenericParameterSymbol;
import semantic.type.TypeResolution;
import semantic.type.TypeResolver;
import semantic.type.TypeSymbol;
import semantic.type.ValueTypeSymbol;

import java.util.Arrays;
import java.util.List;

/// Performs an initial semantic type check for declarations, assignments, and simple expressions.
public final class TypeChecker {

    private static final TypeSymbol BOOL_TYPE = ValueTypeSymbol.builtin("bool");
    private static final TypeSymbol INT_TYPE = ValueTypeSymbol.builtin("int");

    private final DiagnosticBag diagnostics = new DiagnosticBag();
    private final TypeResolver typeResolver = new TypeResolver();
    private SemanticScope rootScope;

    /// Checks a list of statements for type errors and returns a list of diagnostics.
    /// @param statements The list of statements to check.
    /// @return A list of diagnostics for any type errors found.
    public List<Diagnostic> check(List<StatementNode> statements) {

        diagnostics.clear();
        rootScope = new SemanticScopeBuilder().build(statements);
        if (statements != null)
            for (var statement : statements) checkStatement(statement, rootScope);
        return diagnostics.getDiagnostics();
    }

    /// Returns the root semantic scope built during type checking.
    /// @return The root semantic scope.
    public SemanticScope getRootScope() { return rootScope; }

    /// Recursively checks a statement and its nested statements for type errors.
    /// @param statement The statement to check.
    /// @param scope The current semantic scope for type checking.
    private void checkStatement(StatementNode statement, SemanticScope scope) {

        switch (statement) {

            case ClassDeclarationStatement classDeclaration -> checkClass(classDeclaration, scope);
            case ClassMethodDeclaration methodDeclaration -> checkFunction(methodDeclaration, scope, "method " + methodDeclaration.getName());
            case FunctionDeclarationStatement functionDeclaration -> checkFunction(functionDeclaration, scope, "function " + functionDeclaration.getName());
            case VariableDeclarationStatement variableDeclaration -> checkInitializer(variableDeclaration, scope);
            case BlockStatement block -> checkBlock(block, scope);
            case ExpressionStatement expressionStatement -> inferExpression(expressionStatement.getExpression(), scope);
            case ReturnStatement returnStatement -> inferExpression(returnStatement.getReturnValue(), scope);
            case IfStatement ifStatement -> {

                expectBool(ifStatement.getCondition(), scope, "If condition must be bool");
                checkBranch(ifStatement.getThenBlock(), scope, "if-then", ifStatement);
                checkBranch(ifStatement.getElseBlock(), scope, "if-else", ifStatement);
            }
            case WhileStatement whileStatement -> {

                expectBool(whileStatement.getCondition(), scope, "While condition must be bool");
                checkBranch(whileStatement.getBody(), scope, "while", whileStatement);
            }
            case ForStatement forStatement -> {

                var forScope = childScope(scope, "for", forStatement);
                checkStatement(forStatement.getInitialization(), forScope);
                expectBool(forStatement.getCondition(), forScope, "For condition must be bool");
                checkStatement(forStatement.getIncrement(), forScope);
                checkBranch(forStatement.getBody(), forScope, "for-body", forStatement);
            }
            case ForEachStatement forEachStatement -> {

                inferExpression(forEachStatement.getIterable(), scope);
                var forEachScope = childScope(scope, "for-each", forEachStatement);
                checkBranch(forEachStatement.getBody(), forEachScope, "for-each-body", forEachStatement);
            }
            case SwitchStatement switchStatement -> {

                inferExpression(switchStatement.getSubject(), scope);
                var switchScope = childScope(scope, "switch", switchStatement);
                for (var switchCase : switchStatement.getCases()) {
                    inferExpression(switchCase.getValue(), scope);
                    checkBranch(switchCase.getBody(), switchScope, "switch-case", switchCase);
                }
            }
            case null, default -> {}
        }
    }

    /// Recursively checks a class declaration and its members for type errors.
    /// @param classDeclaration The class declaration to check.
    /// @param scope The current semantic scope for type checking.
    private void checkClass(ClassDeclarationStatement classDeclaration, SemanticScope scope) {

        var classScope = childScope(scope, "class " + classDeclaration.getName(), classDeclaration);

        for (var field : classDeclaration.getFields()) checkStatement(field, classScope);
        for (var method : classDeclaration.getMethods()) checkStatement(method, classScope);
        for (var constructor : classDeclaration.getConstructors()) checkConstructor(constructor, classDeclaration, classScope);
        for (var innerClass : classDeclaration.getInnerClasses()) checkClass(innerClass, classScope);
    }

    /// Recursively checks a function declaration and its body for type errors.
    /// @param functionDeclaration The function declaration to check.
    /// @param scope The current semantic scope for type checking.
    /// @param scopeName The name of the scope for error reporting.
    private void checkFunction(FunctionDeclarationStatement functionDeclaration, SemanticScope scope, String scopeName) {

        var functionScope = childScope(scope, scopeName, functionDeclaration);
        checkFunctionBody(functionDeclaration.getBody(), functionScope);
    }

    /// Recursively checks a class constructor declaration and its body for type errors.
    /// @param constructorDeclaration The class constructor declaration to check.
    /// @param classDeclaration The class declaration that owns the constructor.
    /// @param classScope The current semantic scope for type checking.
    private void checkConstructor(ClassConstructorDeclaration constructorDeclaration, ClassDeclarationStatement classDeclaration, SemanticScope classScope) {

        var constructorScope = childScope(classScope, "constructor " + classDeclaration.getName(), constructorDeclaration);
        checkFunctionBody(constructorDeclaration.getBody(), constructorScope);
    }

    /// Recursively checks a function or constructor body for type errors.
    /// @param body The body of the function or constructor to check.
    /// @param functionScope The current semantic scope for type checking.
    private void checkFunctionBody(StatementNode body, SemanticScope functionScope) {

        if (body instanceof BlockStatement block)
            for (var statement : block.getStatements()) checkStatement(statement, functionScope);
        else checkStatement(body, functionScope);
    }

    /// Recursively checks a block statement and its nested statements for type errors.
    /// @param block The block statement to check.
    /// @param scope The current semantic scope for type checking.
    private void checkBlock(BlockStatement block, SemanticScope scope) {

        var blockScope = childScope(scope, "block", block);
        for (var statement : block.getStatements()) checkStatement(statement, blockScope);
    }

    /// Recursively checks a branch statement (if, while, for, etc.) and its nested statements for type errors.
    /// @param statement The branch statement to check.
    /// @param scope The current semantic scope for type checking.
    /// @param scopeName The name of the scope for error reporting.
    /// @param owner The AST node that owns the branch statement.
    private void checkBranch(StatementNode statement, SemanticScope scope, String scopeName, AstNode owner) {

        if (statement == null) return;
        if (statement instanceof BlockStatement block) checkBlock(block, scope);
        else checkStatement(statement, childScope(scope, scopeName, owner));
    }

    /// Checks a variable declaration statement for type errors in its initializer expression.
    /// @param declaration The variable declaration statement to check.
    /// @param scope The current semantic scope for type checking.
    private void checkInitializer(VariableDeclarationStatement declaration, SemanticScope scope) {

        var valueType = inferExpression(declaration.getInitialValue(), scope);
        var targetType = resolveDeclaredType(declaration.getDeclaredType(), declaration, scope);
        if (!isAssignable(targetType, valueType))
            reportTypeMismatch(targetType, valueType, declaration.getInitialValue());
    }

    /// Infers the type of an expression and checks for type errors.
    /// @param expression The expression to infer the type of.
    /// @param scope The current semantic scope for type checking.
    /// @return The inferred semantic type of the expression, or `null` if the type could not be determined.
    private TypeSymbol inferExpression(ExpressionNode expression, SemanticScope scope) {

        return switch (expression) {

            case BoolLiteralExpression _ -> BOOL_TYPE;
            case CharLiteralExpression _ -> ValueTypeSymbol.builtin("char");
            case StringLiteralExpression _ -> ValueTypeSymbol.builtin("string");
            case NullLiteralExpression _ -> null;
            case NumberLiteralExpression number -> ValueTypeSymbol.builtin(number.getTypeToken().getType().token());
            case IdentifierLiteralExpression identifier -> {

                var declaration = firstVisible(scope, identifier.getName());
                yield declaration != null ? resolveDeclaredType(declaration.getDeclaredType(), declaration.getNode(), scope) : null;
            }
            case ObjectCreationExpression objectCreation -> {

                for (var argument : objectCreation.getArguments()) inferExpression(argument, scope);
                yield resolveClassName(objectCreation.getClassName(), objectCreation, scope);
            }
            case AssignmentExpression assignment -> {

                var targetType = inferExpression(assignment.getTarget(), scope);
                var valueType = inferExpression(assignment.getValue(), scope);
                if (!isAssignable(targetType, valueType))
                    reportTypeMismatch(targetType, valueType, assignment.getValue());
                yield targetType;
            }
            case BinaryExpression binary -> {

                var leftType = inferExpression(binary.getLeft(), scope);
                var rightType = inferExpression(binary.getRight(), scope);
                var operator = binary.getOperator().getType();
                if (operator == Operator.LESS_THAN || operator == Operator.GREATER_THAN ||
                        operator == Operator.LESS_EQUAL || operator == Operator.GREATER_EQUAL ||
                        operator == Operator.EQUAL || operator == Operator.NOT_EQUAL ||
                        operator == Operator.LOGICAL_AND || operator == Operator.LOGICAL_OR)
                    yield BOOL_TYPE;
                yield sameType(leftType, rightType) ? leftType : null;
            }
            case UnaryExpression unary -> inferExpression(unary.getOperand(), scope);
            case PostfixUnaryExpression postfix -> inferExpression(postfix.getOperand(), scope);
            case TernaryExpression ternary -> {

                expectBool(ternary.getCondition(), scope, "Ternary condition must be bool");
                var thenType = inferExpression(ternary.getThenExpr(), scope);
                var elseType = inferExpression(ternary.getElseExpr(), scope);
                yield sameType(thenType, elseType) ? thenType : null;
            }
            case CallExpression call -> inferCall(call, scope);
            case MemberAccessExpression memberAccess -> {

                var member = resolveMemberAccess(memberAccess, scope);
                yield member != null ? resolveDeclaredType(member.getDeclaredType(), member.getNode(), scope) : null;
            }
            case ArrayAccessExpression arrayAccess -> inferArrayAccess(arrayAccess, scope);
            case null, default -> null;
        };
    }

    /// Infers the type of an array access expression and checks for type errors.
    /// @param arrayAccess The array access expression to infer the type of.
    /// @param scope The current semantic scope for type checking.
    /// @return The inferred type of the array access expression, or `null` if the type could not be determined or if there was a type error.
    private TypeSymbol inferArrayAccess(ArrayAccessExpression arrayAccess, SemanticScope scope) {

        var arrayType = inferExpression(arrayAccess.getArray(), scope);
        var indexType = inferExpression(arrayAccess.getIndex(), scope);

        if (indexType != null && !sameType(INT_TYPE, indexType)) diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Array index must be int, got " + typeName(indexType),
            arrayAccess.getIndex().getLine(),
            arrayAccess.getIndex().getColumn()
        ));

        if (arrayType == null) return null;
        if (!(arrayType instanceof ArrayTypeSymbol arrayTypeSymbol)) {

            diagnostics.report(Diagnostic.error(
                DiagnosticPhase.SEMANTIC,
                "Cannot index non-array type " + typeName(arrayType),
                arrayAccess.getLine(),
                arrayAccess.getColumn()
            ));
            return null;
        }

        if (arrayTypeSymbol.getDimensions() <= 1) return arrayTypeSymbol.getElementType();
        return new ArrayTypeSymbol(
            arrayTypeSymbol.getElementType(),
            Arrays.copyOfRange(arrayTypeSymbol.getSizes(), 1, arrayTypeSymbol.getSizes().length)
        );
    }

    /// Resolves a member access expression to its corresponding semantic declaration and checks for type errors.
    /// @param memberAccess The member access expression to resolve.
    /// @param scope The current semantic scope for type checking.
    /// @return The semantic declaration corresponding to the member access, or `null` if the member could not be resolved or if there was a type error.
    private SemanticDeclaration resolveMemberAccess(MemberAccessExpression memberAccess, SemanticScope scope) {

        var objectType = inferExpression(memberAccess.getObject(), scope);
        if (objectType == null) return null;

        if (!(objectType instanceof ClassTypeSymbol classType)) {

            diagnostics.report(Diagnostic.error(
                DiagnosticPhase.SEMANTIC,
                "Cannot access member '" + memberAccess.getMemberName() + "' on non-class type " + typeName(objectType),
                memberAccess.getLine(),
                memberAccess.getColumn()
            ));
            return null;
        }

        var classDeclaration = classType.declaration().getNode() instanceof ClassDeclarationStatement declaration
                ? declaration
                : visibleClass(scope, classType.getName());
        if (classDeclaration == null) return null;

        var member = classMember(classDeclaration, memberAccess.getMemberName());
        if (member != null) return member;

        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Undefined member '" + memberAccess.getMemberName() + "' on type '" + classDeclaration.getName() + "'",
            memberAccess.getLine(),
            memberAccess.getColumn()
        ));
        return null;
    }

    /// Resolves a member name within a class declaration to its corresponding semantic declaration.
    /// @param classDeclaration The class declaration to search for the member.
    /// @param memberName The name of the member to resolve.
    /// @return The semantic declaration corresponding to the member, or `null` if the member could not be found.
    private SemanticDeclaration classMember(ClassDeclarationStatement classDeclaration, String memberName) {

        for (var field : classDeclaration.getFields())
            if (field.getName().equals(memberName)) return declaration(DeclarationKind.FIELD, field.getName(), field.getDeclaredType(), field);

        for (var method : classDeclaration.getMethods())
            if (method.getName().equals(memberName)) return declaration(DeclarationKind.METHOD, method.getName(), method.getDeclaredType(), method);

        return null;
    }

    /// Creates a new semantic declaration with the specified kind, name, declared type, and AST node.
    /// @param kind The kind of the declaration (e.g., field, method).
    /// @param name The name of the declaration.
    /// @param declaredType The declared type of the declaration.
    /// @param node The AST node corresponding to the declaration.
    /// @return A new semantic declaration.
    private SemanticDeclaration declaration(DeclarationKind kind, String name, ReturnType declaredType, AstNode node) {
        return new SemanticDeclaration(kind, name, declaredType, node, null);
    }

    /// Infers the type of a function or method call expression and checks for type errors in the arguments.
    /// @param call The call expression to infer the type of.
    /// @param scope The current semantic scope for type checking.
    /// @return The inferred return type of the call expression, or `null` if the type could not be determined or if there was a type error.
    private TypeSymbol inferCall(CallExpression call, SemanticScope scope) {

        var argumentTypes = inferArguments(call.getArguments(), scope);
        if (call.getCallee() instanceof IdentifierLiteralExpression identifier) {

            var declarations = firstVisibleDeclarations(scope, identifier.getName());
            if (declarations.isEmpty()) return null;

            var callable = firstCallable(declarations);
            if (callable == null) {

                diagnostics.report(Diagnostic.error(
                    DiagnosticPhase.SEMANTIC,
                    "Cannot call non-function '" + identifier.getName() + "'",
                    call.getLine(),
                    call.getColumn()
                ));
                return null;
            }

            if (callable.getNode() instanceof FunctionDeclarationStatement functionDeclaration)
                checkCallArguments(call, functionDeclaration, argumentTypes, callableKind(callable), scope);

            return resolveDeclaredType(callable.getDeclaredType(), callable.getNode(), scope);
        }

        if (call.getCallee() instanceof MemberAccessExpression memberAccess) {

            var member = resolveMemberAccess(memberAccess, scope);
            if (member == null) return null;

            if (member.getKind() != DeclarationKind.METHOD) {

                diagnostics.report(Diagnostic.error(
                    DiagnosticPhase.SEMANTIC,
                    "Cannot call non-function member '" + member.getName() + "'",
                    call.getLine(),
                    call.getColumn()
                ));
                return null;
            }

            if (member.getNode() instanceof FunctionDeclarationStatement functionDeclaration)
                checkCallArguments(call, functionDeclaration, argumentTypes, callableKind(member), scope);
            return resolveDeclaredType(member.getDeclaredType(), member.getNode(), scope);
        }

        return inferExpression(call.getCallee(), scope);
    }

    /// Infers the types of the arguments in a function or method call expression.
    /// @param arguments The array of argument expressions to infer the types of.
    /// @param scope The current semantic scope for type checking.
    /// @return An array of inferred types corresponding to the argument expressions.
    private TypeSymbol[] inferArguments(ExpressionNode[] arguments, SemanticScope scope) {

        var argumentTypes = new TypeSymbol[arguments.length];
        for (var i = 0; i < arguments.length; i++)
            argumentTypes[i] = inferExpression(arguments[i], scope);
        return argumentTypes;
    }

    /// Checks the arguments of a function or method call against the expected parameters and reports type errors if there are mismatches.
    /// @param call The call expression being checked.
    /// @param functionDeclaration The function declaration corresponding to the call.
    /// @param argumentTypes The array of inferred types for the call's arguments.
    /// @param callableKind A string representing the kind of callable (e.g., "function" or "method") for error reporting.
    private void checkCallArguments(CallExpression call, FunctionDeclarationStatement functionDeclaration, TypeSymbol[] argumentTypes, String callableKind, SemanticScope scope) {

        var parameters = functionDeclaration.getParameters();
        if (parameters.length != argumentTypes.length) diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            callableKind + " '" + functionDeclaration.getName() + "' expects " + parameters.length +
                " arguments but got " + argumentTypes.length,
            call.getLine(),
            call.getColumn()
        ));

        var checkedCount = Math.min(parameters.length, argumentTypes.length);
        for (var i = 0; i < checkedCount; i++)
            checkArgument(functionDeclaration, parameters[i], argumentTypes[i], call.getArguments()[i], i + 1, callableKind, scope);
    }

    /// Checks a single argument against its corresponding parameter in a function or method call and reports a type error if there is a mismatch.
    /// @param functionDeclaration The function declaration corresponding to the call.
    /// @param parameter The parameter to check against.
    /// @param argumentType The inferred semantic type of the argument.
    /// @param argument The argument expression being checked.
    /// @param argumentNumber The position of the argument in the call (1-based index)
    /// @param callableKind A string representing the kind of callable (e.g., "function" or "method") for error reporting.
    private void checkArgument(FunctionDeclarationStatement functionDeclaration, FunctionParameter parameter, TypeSymbol argumentType, ExpressionNode argument, int argumentNumber, String callableKind, SemanticScope scope) {

        var parameterType = resolveDeclaredType(parameter.getType(), parameter, scope);
        if (isAssignable(parameterType, argumentType)) return;
        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Argument " + argumentNumber + " for " + callableKind.toLowerCase() + " '" + functionDeclaration.getName() +
                "' expects " + typeName(parameterType) + " but got " + typeName(argumentType),
            argument.getLine(),
            argument.getColumn()
        ));
    }

    /// Checks if an expression is of boolean type and reports a type error if it is not.
    /// @param expression The expression to check.
    /// @param scope The current semantic scope for type checking.
    /// @param message The error message to report if the expression is not of boolean type.
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

    /// Finds the first visible semantic declaration with the specified name in the given scope and its parent scopes.
    /// @param scope The semantic scope to start the search from.
    /// @param name The name of the declaration to find.
    /// @return The first visible semantic declaration with the specified name, or `null` if no such declaration is found.
    private SemanticDeclaration firstVisible(SemanticScope scope, String name) {

        var declarations = firstVisibleDeclarations(scope, name);
        return declarations.isEmpty() ? null : declarations.getFirst();
    }

    /// Finds all visible semantic declarations with the specified name in the given scope and its parent scopes.
    /// @param scope The semantic scope to start the search from.
    /// @param name The name of the declarations to find.
    /// @return A list of all visible semantic declarations with the specified name, or an empty list if no such declarations are found.
    private List<SemanticDeclaration> firstVisibleDeclarations(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            var local = current.findLocal(name);
            if (!local.isEmpty()) return local;
            current = current.getParent();
        }
        return List.of();
    }

    /// Finds the first callable semantic declaration (function or method) in a list of declarations.
    /// @param declarations The list of semantic declarations to search.
    /// @return The first callable semantic declaration found, or `null` if no callable declarations are found.
    private SemanticDeclaration firstCallable(List<SemanticDeclaration> declarations) {

        for (var declaration : declarations)
            if (declaration.getKind() == DeclarationKind.FUNCTION || declaration.getKind() == DeclarationKind.METHOD) return declaration;
        return null;
    }

    /// Returns a string representing the kind of callable (function or method) for error reporting.
    /// @param declaration The semantic declaration to check.
    /// @return A string representing the kind of callable ("Function" or "Method").
    private String callableKind(SemanticDeclaration declaration) {
        return declaration.getKind() == DeclarationKind.METHOD ? "Method" : "Function";
    }

    /// Finds the first visible class declaration with the specified name in the given scope and its parent scopes.
    /// @param scope The semantic scope to start the search from.
    /// @param name The name of the class declaration to find.
    /// @return The first visible class declaration with the specified name, or `null` if no such class declaration is found.
    private ClassDeclarationStatement visibleClass(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            for (var declaration : current.findLocal(name))
                if (declaration.getKind() == DeclarationKind.CLASS &&
                    declaration.getNode() instanceof ClassDeclarationStatement classDeclaration)
                    return classDeclaration;
            current = current.getParent();
        }
        return null;
    }

    /// Checks if a value type can be assigned to a target type, considering nullability and type compatibility.
    /// @param targetType The target type to assign to.
    /// @param valueType The value type to assign from.
    /// @return `true` if the value type can be assigned to the target type, `false` otherwise.
    private boolean isAssignable(TypeSymbol targetType, TypeSymbol valueType) {
        return targetType == null || valueType == null ||
                !targetType.isResolved() || !valueType.isResolved() ||
                sameType(targetType, valueType);
    }

    /// Checks if two semantic types are the same.
    /// @param left The first semantic type to compare.
    /// @param right The second semantic type to compare.
    /// @return `true` if the two semantic types are the same, `false` otherwise.
    private boolean sameType(TypeSymbol left, TypeSymbol right) {

        if (left == null || right == null) return false;
        if (!left.isResolved() || !right.isResolved()) return false;

        if (left instanceof ArrayTypeSymbol leftArray && right instanceof ArrayTypeSymbol rightArray)
            return leftArray.getDimensions() == rightArray.getDimensions() &&
                    sameType(leftArray.getElementType(), rightArray.getElementType());

        if (left instanceof ValueTypeSymbol leftValue && right instanceof ValueTypeSymbol rightValue)
            return leftValue.getName().equals(rightValue.getName());

        if (left instanceof ClassTypeSymbol leftClass && right instanceof ClassTypeSymbol rightClass)
            return leftClass.getName().equals(rightClass.getName());

        if (left instanceof GenericParameterSymbol leftGeneric && right instanceof GenericParameterSymbol rightGeneric)
            return leftGeneric.getName().equals(rightGeneric.getName());

        return false;
    }

    /// Creates a child semantic scope with the specified name and owner node, or returns the current scope if no matching child scope is found.
    /// @param scope The current semantic scope to search for a child scope.
    /// @param name The name of the child scope to find.
    /// @param owner The AST node that owns the child scope.
    /// @return The child semantic scope with the specified name and owner, or the current scope if no matching child scope is found.
    private SemanticScope childScope(SemanticScope scope, String name, AstNode owner) {

        for (var child : scope.getChildren())
            if (child.getOwner() == owner && child.getName().equals(name)) return child;
        return scope;
    }

    /// Reports a type mismatch error when a value type cannot be assigned to a target type.
    /// @param targetType The target type to assign to.
    /// @param valueType The value type to assign from.
    /// @param node The AST node where the type mismatch occurred.
    private void reportTypeMismatch(TypeSymbol targetType, TypeSymbol valueType, AstNode node) {

        if (node == null) return;
        diagnostics.report(Diagnostic.error(
            DiagnosticPhase.SEMANTIC,
            "Type mismatch: cannot assign " + typeName(valueType) + " to " + typeName(targetType),
            node.getLine(),
            node.getColumn()
        ));
    }

    /// Returns a string representation of a semantic type for error reporting.
    /// @param type The semantic type to represent as a string.
    /// @return A string representation of the semantic type, or "<unknown>" if the type is null.
    private String typeName(TypeSymbol type) {
        return type != null ? type.getName() : "<unknown>";
    }

    /// Resolves a declared ReturnType adapter to a semantic type symbol for type checking.
    /// @param type The declared ReturnType adapter.
    /// @param owner The AST node that owns the declaration.
    /// @param scope The semantic scope to resolve from.
    /// @return The resolved semantic type, or {@code null} if the type is absent.
    private TypeSymbol resolveDeclaredType(ReturnType type, AstNode owner, SemanticScope scope) {

        var resolution = typeResolver.resolve(type, owner, scope);
        report(resolution);
        return resolution.type();
    }

    /// Resolves a class name to a semantic type symbol for object construction.
    /// @param className The class name.
    /// @param owner The AST node that owns the construction expression.
    /// @param scope The semantic scope to resolve from.
    /// @return The resolved class type, or an unknown type placeholder.
    private TypeSymbol resolveClassName(String className, AstNode owner, SemanticScope scope) {

        var resolution = typeResolver.resolveClassName(className, owner, scope, "class");
        report(resolution);
        return resolution.type();
    }

    /// Reports diagnostics produced by semantic type resolution.
    /// @param resolution The type-resolution result.
    private void report(TypeResolution resolution) {
        for (var diagnostic : resolution.diagnostics()) diagnostics.report(diagnostic);
    }
}
