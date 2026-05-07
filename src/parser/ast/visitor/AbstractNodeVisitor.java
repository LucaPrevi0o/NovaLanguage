package parser.ast.visitor;

import parser.ast.nodes.expression.*;
import parser.ast.nodes.expression.access.ArrayAccessExpression;
import parser.ast.nodes.expression.access.MemberAccessExpression;
import parser.ast.nodes.expression.literal.*;
import parser.ast.nodes.statement.*;
import parser.ast.nodes.statement.conditional.*;
import parser.ast.nodes.statement.declaration.*;
import parser.ast.nodes.statement.declaration.object.*;

/// Convenience base class for {@link NodeVisitor} that throws
/// {@link UnsupportedOperationException} for every node type by default.
///
/// Subclasses override only the visit methods they care about.
///
/// @param <T> The return type produced by each visit method.
public abstract class AbstractNodeVisitor<T> implements NodeVisitor<T> {

    private T unhandled(Object node) {
        throw new UnsupportedOperationException("No visitor implementation for " + node.getClass().getSimpleName());
    }

    @Override public T visitBlock(BlockStatement node)                       { return unhandled(node); }
    @Override public T visitBreak(BreakStatement node)                       { return unhandled(node); }
    @Override public T visitContinue(ContinueStatement node)                 { return unhandled(node); }
    @Override public T visitExpressionStatement(ExpressionStatement node)    { return unhandled(node); }
    @Override public T visitReturn(ReturnStatement node)                     { return unhandled(node); }
    @Override public T visitClassDeclaration(ClassDeclarationStatement node) { return unhandled(node); }
    @Override public T visitFunctionDeclaration(FunctionDeclarationStatement node) { return unhandled(node); }
    @Override public T visitVariableDeclaration(VariableDeclarationStatement node) { return unhandled(node); }
    @Override public T visitClassField(ClassFieldDeclaration node)           { return unhandled(node); }
    @Override public T visitClassMethod(ClassMethodDeclaration node)         { return unhandled(node); }
    @Override public T visitClassConstructor(ClassConstructorDeclaration node) { return unhandled(node); }
    @Override public T visitIf(IfStatement node)                             { return unhandled(node); }
    @Override public T visitWhile(WhileStatement node)                       { return unhandled(node); }
    @Override public T visitFor(ForStatement node)                           { return unhandled(node); }
    @Override public T visitForEach(ForEachStatement node)                   { return unhandled(node); }
    @Override public T visitSwitch(SwitchStatement node)                     { return unhandled(node); }
    @Override public T visitAssignment(AssignmentExpression node)            { return unhandled(node); }
    @Override public T visitBinary(BinaryExpression node)                    { return unhandled(node); }
    @Override public T visitUnary(UnaryExpression node)                      { return unhandled(node); }
    @Override public T visitPostfixUnary(PostfixUnaryExpression node)        { return unhandled(node); }
    @Override public T visitCall(CallExpression node)                        { return unhandled(node); }
    @Override public T visitObjectCreation(ObjectCreationExpression node)    { return unhandled(node); }
    @Override public T visitMemberAccess(MemberAccessExpression node)        { return unhandled(node); }
    @Override public T visitArrayAccess(ArrayAccessExpression node)          { return unhandled(node); }
    @Override public T visitTernary(TernaryExpression node)                  { return unhandled(node); }
    @Override public T visitBoolLiteral(BoolLiteralExpression node)          { return unhandled(node); }
    @Override public T visitCharLiteral(CharLiteralExpression node)          { return unhandled(node); }
    @Override public T visitIdentifierLiteral(IdentifierLiteralExpression node) { return unhandled(node); }
    @Override public T visitNullLiteral(NullLiteralExpression node)          { return unhandled(node); }
    @Override public T visitNumberLiteral(NumberLiteralExpression node)      { return unhandled(node); }
    @Override public T visitStringLiteral(StringLiteralExpression node)      { return unhandled(node); }
}
