package printer;

import java.util.List;

import lexer.token.OperatorToken;
import parser.ast.AstNode;
import parser.ast.nodes.expression.*;
import parser.ast.nodes.expression.access.ArrayAccessExpression;
import parser.ast.nodes.expression.access.MemberAccessExpression;
import parser.ast.nodes.expression.literal.*;
import parser.ast.nodes.statement.*;
import parser.ast.nodes.statement.conditional.*;
import parser.ast.nodes.statement.declaration.*;
import parser.ast.nodes.statement.declaration.object.*;
import parser.ast.visitor.AbstractNodeVisitor;
import token.ReturnType;

/// A utility class for printing the Abstract Syntax Tree (AST) of a program in a readable format.
public class AstPrinter extends AbstractNodeVisitor<Void> {

    private final List<String> spacers;
    private final boolean vLine;

    private AstPrinter(List<String> spacers, boolean vLine) {
        this.spacers = spacers;
        this.vLine = vLine;
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /// Prints the AST starting from the given node with the specified header.
    /// @param node The root node of the AST to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header A string to prefix the root node, typically used for visual structure.
    public static void printASTNode(AstNode node, List<String> spacers, String header) {
        printASTNode(node, spacers, header, false);
    }

    // ─── Core recursive dispatcher ─────────────────────────────────────────────

    /// A recursive helper method that prints the AST node and its children with proper indentation and visual structure.
    /// @param node The AST node to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header A string to prefix the node, typically used for visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for child nodes.
    private static void printASTNode(AstNode node, List<String> spacers, String header, boolean vLine) {

        if (node == null) return;
        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, header + node.getClass().getSimpleName() + " [line " + node.getLine() + "]");
        try {
            node.accept(new AstPrinter(spacers, !header.equals("└─ ")));
        } catch (Exception e) { e.printStackTrace(); }
        spacers.removeLast();
    }

    // ─── Instance helpers ──────────────────────────────────────────────────────

    private void printChild(AstNode node, String header) {
        printASTNode(node, spacers, header, false);
    }

    private void printChild(AstNode node, String header, boolean passVLine) {
        printASTNode(node, spacers, header, passVLine);
    }

    /// A helper method to print an array of child AST nodes with a header and proper indentation.
    /// @param nodes An array of child AST nodes to print.
    /// @param header A string to prefix the child nodes, typically used for visual structure.
    private void printChildNodes(AstNode[] nodes, String header) {
        printChildNodes(nodes, header, false);
    }

    /// A helper method to print an array of child AST nodes with a header and proper indentation, optionally including vertical lines for visual structure.
    /// @param nodes An array of child AST nodes to print.
    /// @param header A string to prefix the child nodes, typically used for visual structure.
    /// @param passVLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes.
    private void printChildNodes(AstNode[] nodes, String header, boolean passVLine) {

        printLine(spacers, header + nodes.length);
        for (int i = 0; i < nodes.length; i++) {

            var childPrefix = (i == nodes.length - 1) ? "└─ " : "├─ ";
            printChild(nodes[i], childPrefix, passVLine);
        }
    }

    // ─── Statement visitors ────────────────────────────────────────────────────

    @Override
    public Void visitBlock(BlockStatement node) {

        spacers.add("   ");
        printChildNodes(node.getStatements(), "└─ Statements: ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitBreak(BreakStatement node) {

        printLine(spacers, "└─ break");
        return null;
    }

    @Override
    public Void visitContinue(ContinueStatement node) {

        printLine(spacers, "└─ continue");
        return null;
    }

    @Override
    public Void visitReturn(ReturnStatement node) {

        spacers.add("   ");
        if (node.getReturnValue() != null) {

            printLine(spacers, "└─ Return Value:");
            printChild(node.getReturnValue(), "└─ ");
        }
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        if (node.getExpression() != null) {

            printLine(spacers, "└─ Expression:");
            printChild(node.getExpression(), "└─ ");
        }
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitClassDeclaration(ClassDeclarationStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Name: " + node.getName());
        if (node.getSuperClasses() != null) {

            var superClasses = node.getSuperClasses();
            var nodes = new AstNode[superClasses.length];
            for (int i = 0; i < superClasses.length; i++)
                nodes[i] = new IdentifierLiteralExpression(node.getLine(), node.getColumn(), superClasses[i].getBaseType().get());
            printChildNodes(nodes, "├─ Superclasses: ", true);
        }
        printLine(spacers, "├─ Access Modifier: " + node.getAccessModifier());
        if (node.getGenericClassParameter() != null)
            printLine(spacers, "├─ Generic Parameter: " + buildTypeStringWithSizes(node.getGenericClassParameter()));
        printChildNodes(node.getConstructors(), "├─ Constructors: ", true);
        printChildNodes(node.getInnerClasses(), "├─ Inner Classes: ", true);
        printChildNodes(node.getFields(), "├─ Fields: ", true);
        printChildNodes(node.getMethods(), "└─ Methods: ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitFunctionDeclaration(FunctionDeclarationStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printFunctionElements(node);
        return null;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Name: " + node.getName());
        if (node.getInitialValue() != null) {

            printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(node.getDeclaredType()));
            printLine(spacers, "└─ Initializer:");
            printChild(node.getInitialValue(), "└─ ");
        } else printLine(spacers, "└─ Type: " + buildTypeStringWithSizes(node.getDeclaredType()));
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitClassField(ClassFieldDeclaration node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Name: " + node.getName());
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(node.getDeclaredType()));
        if (node.getInitialValue() != null) {

            printLine(spacers, "├─ Access Modifier: " + node.getAccessModifier());
            printLine(spacers, "└─ Initializer:");
            printChild(node.getInitialValue(), "└─ ");
        } else printLine(spacers, "└─ Access Modifier: " + node.getAccessModifier());
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitClassMethod(ClassMethodDeclaration node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Access Modifier: " + node.getAccessModifier());
        printFunctionElements(node);
        return null;
    }

    @Override
    public Void visitClassConstructor(ClassConstructorDeclaration node) {

        spacers.add(vLine ? "│  " : "   ");
        var params = node.getParameters();
        printLine(spacers, "├─ Access Modifier: " + node.getAccessModifier());
        printLine(spacers, "├─ Parameters: " + params.length);
        printLine(spacers, "└─ Body:");
        printChild(node.getBody(), "└─ ");
        printParameters(params);
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitIf(IfStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printChild(node.getCondition(), "└─ ", true);
        if (node.getElseBlock() != null) {

            printLine(spacers, "├─ Then:");
            printChild(node.getThenBlock(), "└─ ", true);
            printLine(spacers, "└─ Else:");
            printChild(node.getElseBlock(), "└─ ");
        } else {

            printLine(spacers, "└─ Then:");
            printChild(node.getThenBlock(), "└─ ");
        }
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitWhile(WhileStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printChild(node.getCondition(), "└─ ", true);
        printLine(spacers, "└─ Body:");
        printChild(node.getBody(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitFor(ForStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printChild(node.getCondition(), "└─ ", true);
        if (node.getInitialization() != null) {

            printLine(spacers, "├─ Initializer:");
            printChild(node.getInitialization(), "└─ ", true);
        }
        if (node.getIncrement() != null) {

            printLine(spacers, "├─ Increment:");
            printChild(node.getIncrement(), "└─ ", true);
        }
        printLine(spacers, "└─ Body:");
        printChild(node.getBody(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitForEach(ForEachStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Element Type: " + buildTypeStringWithSizes(node.getElementType()));
        printLine(spacers, "├─ Element Name: " + node.getElementName());
        printLine(spacers, "├─ Iterable:");
        printChild(node.getIterable(), "└─ ", true);
        printLine(spacers, "└─ Body:");
        printChild(node.getBody(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitSwitch(SwitchStatement node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Subject:");
        printChild(node.getSubject(), "└─ ", true);
        var cases = node.getCases();
        printLine(spacers, "└─ Cases: " + cases.length);
        spacers.add("   ");
        for (int i = 0; i < cases.length; i++) {

            var c = cases[i];
            var isLast = (i == cases.length - 1);
            var prefix = isLast ? "└─ " : "├─ ";
            printLine(spacers, prefix + (c.isDefault() ? "default" : "case") + " [line " + c.getLine() + "]");
            spacers.add(isLast ? "   " : "│  ");
            if (!c.isDefault()) {

                printLine(spacers, "├─ Value:");
                printChild(c.getValue(), "└─ ", true);
            }
            printLine(spacers, "└─ Body:");
            printChild(c.getBody(), "└─ ");
            spacers.removeLast();
        }
        spacers.removeLast();
        spacers.removeLast();
        return null;
    }

    // ─── Expression visitors ───────────────────────────────────────────────────

    @Override
    public Void visitAssignment(AssignmentExpression node) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Target:");
        printChild(node.getTarget(), "└─ ", true);
        printLine(spacers, "└─ Value:");
        printChild(node.getValue(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitBinary(BinaryExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + node.getOperator().getType());
        printLine(spacers, "├─ Left:");
        printChild(node.getLeft(), "└─ ", true);
        printLine(spacers, "└─ Right:");
        printChild(node.getRight(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitUnary(UnaryExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + node.getOperator().getType());
        printLine(spacers, "└─ Operand:");
        printChild(node.getOperand(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitPostfixUnary(PostfixUnaryExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + node.getOperator().getType());
        printLine(spacers, "└─ Operand:");
        printChild(node.getOperand(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitTernary(TernaryExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Condition:");
        printChild(node.getCondition(), "└─ ", true);
        printLine(spacers, "├─ Then:");
        printChild(node.getThenExpr(), "└─ ", true);
        printLine(spacers, "└─ Else:");
        printChild(node.getElseExpr(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitCall(CallExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Callee:");
        printChild(node.getCallee(), "└─ ", true);
        printChildNodes(node.getArguments(), "└─ Arguments: ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitObjectCreation(ObjectCreationExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Class: " + node.getClassName());
        printChildNodes(node.getArguments(), "└─ Arguments: ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitMemberAccess(MemberAccessExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Member: " + node.getMemberName());
        printLine(spacers, "└─ Object:");
        printChild(node.getObject(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessExpression node) {

        spacers.add("   ");
        printLine(spacers, "├─ Array:");
        printChild(node.getArray(), "└─ ", true);
        printLine(spacers, "└─ Index:");
        printChild(node.getIndex(), "└─ ");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitIdentifierLiteral(IdentifierLiteralExpression node) {

        spacers.add("   ");
        printLine(spacers, "└─ Identifier: " + node.getName());
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitNumberLiteral(NumberLiteralExpression node) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + node.getValue());
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitStringLiteral(StringLiteralExpression node) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + node.getValue());
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitCharLiteral(CharLiteralExpression node) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: '" + node.getValue() + "'");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitNullLiteral(NullLiteralExpression node) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: null");
        spacers.removeLast();
        return null;
    }

    @Override
    public Void visitBoolLiteral(BoolLiteralExpression node) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + node.getValue());
        spacers.removeLast();
        return null;
    }

    // ─── Shared helpers ────────────────────────────────────────────────────────

    /// A helper method to print the common elements of a function declaration, such as its return type, parameters, name, and body.
    /// This method is used by both regular function declarations and class method declarations to avoid code duplication.
    /// Note: this method removes the spacer that was added by the calling visit method.
    /// @param fds The FunctionDeclarationStatement node representing the function declaration to print.
    private void printFunctionElements(FunctionDeclarationStatement fds) {

        var returnType = fds.getDeclaredType();
        var params = fds.getParameters();
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(returnType));
        printLine(spacers, "├─ Parameters: " + params.length);
        printParameters(params);
        printLine(spacers, "├─ Name: " + fds.getName());
        printLine(spacers, "└─ Body:");
        printChild(fds.getBody(), "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of function parameters, including their names and types.
    /// @param params An array of FunctionParameter objects representing the parameters to print.
    private void printParameters(FunctionParameter[] params) {

        spacers.add("│  ");
        for (int i = 0; i < params.length; i++) {

            var param = params[i];
            var isLast = (i == params.length - 1);
            var prefix = isLast ? "└─ " : "├─ ";
            printLine(spacers, prefix + param.getName() + ": " + buildTypeStringWithSizes(param.getType()));
        }
        spacers.removeLast();
    }

    /// A helper method to print a line of text with the appropriate indentation based on the spacers list.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param text The text to print, which typically includes information about the AST node.
    private static void printLine(List<String> spacers, String text) {

        var prefix = new StringBuilder();
        for (var s : spacers) prefix.append(s);
        System.out.println(prefix + text);
    }

    /// A helper method to build a string representation of a return type, including its base type and any array sizes if applicable.
    /// @param type The ReturnType object representing the return type to build a string for.
    /// @return A string representation of the return type, including its base type and any array sizes if applicable.
    public static String buildTypeStringWithSizes(ReturnType type) {

        if (type == null) return "null";
        var baseType = type.getBaseType();
        var baseTypeStr = baseType.get();

        var sizes = type.getSizes();
        if (sizes == null || sizes.length == 0) return baseTypeStr;
        var result = new StringBuilder(baseTypeStr);
        for (var size : sizes) {

            result.append("[");
            if (size != null) {

                if (size instanceof NumberLiteralExpression ile) result.append(ile.getValue());
                else result.append(size);
            }
            result.append("]");
        }
        return result.toString();
    }
}
