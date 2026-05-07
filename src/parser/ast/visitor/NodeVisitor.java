package parser.ast.visitor;

import parser.ast.nodes.expression.*;
import parser.ast.nodes.expression.access.ArrayAccessExpression;
import parser.ast.nodes.expression.access.MemberAccessExpression;
import parser.ast.nodes.expression.literal.*;
import parser.ast.nodes.statement.*;
import parser.ast.nodes.statement.conditional.*;
import parser.ast.nodes.statement.declaration.*;
import parser.ast.nodes.statement.declaration.object.*;

/// Visitor interface for traversing and transforming every node type in the AST.
///
/// Implementations supply a typed result {@code T} for each node.
/// Use {@link AbstractNodeVisitor} as a convenience base that throws
/// {@link UnsupportedOperationException} for unhandled node types.
///
/// @param <T> The return type produced by each visit method.
public interface NodeVisitor<T> {

    // ─── Statements ────────────────────────────────────────────────────────────

    T visitBlock(BlockStatement node);
    T visitBreak(BreakStatement node);
    T visitContinue(ContinueStatement node);
    T visitExpressionStatement(ExpressionStatement node);
    T visitReturn(ReturnStatement node);
    T visitClassDeclaration(ClassDeclarationStatement node);
    T visitFunctionDeclaration(FunctionDeclarationStatement node);
    T visitVariableDeclaration(VariableDeclarationStatement node);
    T visitClassField(ClassFieldDeclaration node);
    T visitClassMethod(ClassMethodDeclaration node);
    T visitClassConstructor(ClassConstructorDeclaration node);
    T visitIf(IfStatement node);
    T visitWhile(WhileStatement node);
    T visitFor(ForStatement node);
    T visitForEach(ForEachStatement node);
    T visitSwitch(SwitchStatement node);

    // ─── Expressions ───────────────────────────────────────────────────────────

    T visitAssignment(AssignmentExpression node);
    T visitBinary(BinaryExpression node);
    T visitUnary(UnaryExpression node);
    T visitPostfixUnary(PostfixUnaryExpression node);
    T visitCall(CallExpression node);
    T visitObjectCreation(ObjectCreationExpression node);
    T visitMemberAccess(MemberAccessExpression node);
    T visitArrayAccess(ArrayAccessExpression node);
    T visitTernary(TernaryExpression node);
    T visitBoolLiteral(BoolLiteralExpression node);
    T visitCharLiteral(CharLiteralExpression node);
    T visitIdentifierLiteral(IdentifierLiteralExpression node);
    T visitNullLiteral(NullLiteralExpression node);
    T visitNumberLiteral(NumberLiteralExpression node);
    T visitStringLiteral(StringLiteralExpression node);
}
