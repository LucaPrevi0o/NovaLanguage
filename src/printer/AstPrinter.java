package src.printer;

import java.util.List;

import src.parser.ast.AstNode;
import src.parser.ast.nodes.expression.AssignmentExpression;
import src.parser.ast.nodes.expression.BinaryExpression;
import src.parser.ast.nodes.expression.CallExpression;
import src.parser.ast.nodes.expression.UnaryExpression;
import src.parser.ast.nodes.expression.access.ArrayAccessExpression;
import src.parser.ast.nodes.expression.access.MemberAccessExpression;
import src.parser.ast.nodes.expression.literal.BoolLiteralExpression;
import src.parser.ast.nodes.expression.literal.CharLiteralExpression;
import src.parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import src.parser.ast.nodes.expression.literal.NumberLiteralExpression;
import src.parser.ast.nodes.expression.literal.StringLiteralExpression;
import src.parser.ast.nodes.statement.BlockStatement;
import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.parser.ast.nodes.statement.ExpressionStatement;
import src.parser.ast.nodes.statement.ReturnStatement;
import src.parser.ast.nodes.statement.conditional.ForStatement;
import src.parser.ast.nodes.statement.conditional.IfStatement;
import src.parser.ast.nodes.statement.conditional.WhileStatement;
import src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import src.token.ReturnType;

/// A utility class for printing the Abstract Syntax Tree (AST) of a program in a readable format.
public class AstPrinter {

    /// Prints the AST starting from the given node with the specified header.
    /// @param node The root node of the AST to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header A string to prefix the root node, typically used for visual structure.
    public static void printASTNode(AstNode node, List<String> spacers, String header) { printASTNode(node, spacers, header, false); }

    /// A recursive helper method that prints the AST node and its children with proper indentation and visual structure.
    /// @param node The AST node to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header A string to prefix the node, typically used for visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for child nodes.
    private static void printASTNode(AstNode node, List<String> spacers, String header, boolean vLine) {

        if (node == null) return;
        var className = node.getClass().getSimpleName();
        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, header + className + " [line " + node.getLine() + "]");

        try {

            switch (node) {

                case ClassDeclarationStatement cds -> printClassDeclaration(cds, spacers, !header.equals("└─ "));
                case ClassMethodDeclaration cmd -> printMethodDeclaration(cmd, spacers, !header.equals("└─ "));
                case FunctionDeclarationStatement fds -> printFunctionDeclaration(fds, spacers, !header.equals("└─ "));
                case ClassConstructorDeclaration ccd -> printConstructorDeclaration(ccd, spacers, !header.equals("└─ "));
                case ClassFieldDeclaration cfd -> printFieldDeclaration(cfd, spacers, !header.equals("└─ "));
                case VariableDeclarationStatement vds -> printVariableDeclaration(vds, spacers, !header.equals("└─ "));
                case ReturnStatement rs -> printReturnStatement(rs, spacers);
                case BlockStatement bs -> printBlockStatement(bs, spacers);
                case IfStatement is -> printIfStatement(is, spacers, !header.equals("└─ "));
                case WhileStatement ws -> printWhileStatement(ws, spacers, !header.equals("└─ "));
                case ForStatement fs -> printForStatement(fs, spacers, !header.equals("└─ "));
                case ExpressionStatement es -> printExpressionStatement(es, spacers, !header.equals("└─ "));
                case AssignmentExpression ae -> printAssignmentExpression(ae, spacers, !header.equals("└─ "));
                case BinaryExpression be -> printBinaryExpression(be, spacers);
                case UnaryExpression ue -> printUnaryExpression(ue, spacers);
                case CallExpression ce -> printCallExpression(ce, spacers);
                case MemberAccessExpression mae -> printMemberAccess(mae, spacers);
                case ArrayAccessExpression aae -> printArrayAccess(aae, spacers);
                case IdentifierLiteralExpression ile -> printIdentifier(ile, spacers);
                case NumberLiteralExpression nle -> printNumberLiteral(nle, spacers);
                case StringLiteralExpression sle -> printStringLiteral(sle, spacers);
                case CharLiteralExpression cle -> printCharLiteral(cle, spacers);
                case BoolLiteralExpression ble -> printBoolLiteral(ble, spacers);
                default -> {}
            }
        } catch (Exception e) { e.printStackTrace(); }
        spacers.removeLast();
    }

    /// A helper method to print a line of text with the appropriate indentation based on the spacers list.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param text The text to print, which typically includes information about the AST node.
    private static void printLine(List<String> spacers, String text) {

        var prefix = new StringBuilder();
        for (var s: spacers) prefix.append(s);
        System.out.println(prefix + text);
    }

    /// A helper method to print an array of child AST nodes with a header and proper indentation.
    /// @param nodes An array of child AST nodes to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header A string to prefix the child nodes, typically used for visual structure.
    private static void printChildNodes(AstNode[] nodes, List<String> spacers, String header) { printChildNodes(nodes, spacers, header, false); }

    /// A helper method to print an array of child AST nodes with a header and proper indentation, optionally including vertical lines for visual structure.
    /// @param nodes An array of child AST nodes to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header A string to prefix the child nodes, typically used for visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes.
    private static void printChildNodes(AstNode[] nodes, List<String> spacers, String header, boolean vLine) {

        printLine(spacers, header + nodes.length);
        for (int i = 0; i < nodes.length; i++) {

            var childPrefix = (i == nodes.length - 1) ? "└─ " : "├─ ";
            printASTNode(nodes[i], spacers, childPrefix, vLine);
        }
    }

    /// A helper method to print the details of a function declaration, including its return type, parameters, name, and body.
    /// @param fds The FunctionDeclarationStatement node representing the function declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the function declaration.
    private static void printFunctionDeclaration(FunctionDeclarationStatement fds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printFunctionElements(fds, spacers);
    }

    /// A helper method to print the common elements of a function declaration, such as its return type, parameters, name, and body.
    /// This method is used by both regular function declarations and class method declarations to avoid code duplication.
    /// @param fds The FunctionDeclarationStatement node representing the function declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printFunctionElements(FunctionDeclarationStatement fds, List<String> spacers) {

        var returnType = fds.getDeclaredType();
        var params = fds.getParameters();
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(returnType));
        printLine(spacers, "├─ Parameters: " + params.length);
        printParameters(params, spacers);
        printLine(spacers, "├─ Name: " + fds.getName());
        printLine(spacers, "└─ Body:");
        printASTNode(fds.getBody(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a class declaration, including its name, superclasses, access modifier,
    /// generic parameter, constructors, inner classes, fields, and methods.
    /// @param cds The ClassDeclarationStatement node representing the class declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the class declaration.
    private static void printClassDeclaration(ClassDeclarationStatement cds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Name: " + cds.getName());
        if (cds.getSuperClasses() != null) {

            var superClasses = cds.getSuperClasses();
            var nodes = new AstNode[superClasses.length];
            for (int i = 0; i < superClasses.length; i++)
                nodes[i] = new IdentifierLiteralExpression(cds.getLine(), cds.getColumn(), superClasses[i].getBaseType().get());
            printChildNodes(nodes, spacers, "├─ Superclasses: ", true);
        }
        printLine(spacers, "├─ Access Modifier: " + cds.getAccessModifier());
        if (cds.getGenericClassParameter() != null)
            printLine(spacers, "├─ Generic Parameter: " + buildTypeStringWithSizes(cds.getGenericClassParameter()));
        printChildNodes(cds.getConstructors(), spacers, "├─ Constructors: ", true);
        printChildNodes(cds.getInnerClasses(), spacers, "├─ Inner Classes: ", true);
        printChildNodes(cds.getFields(), spacers, "├─ Fields: ", true);
        printChildNodes(cds.getMethods(), spacers, "└─ Methods: ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a class field declaration, including its name, type, access modifier, and initializer if present.
    /// @param cfd The ClassFieldDeclaration node representing the class field declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the class field declaration.
    private static void printFieldDeclaration(ClassFieldDeclaration cfd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Name: " + cfd.getName());
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(cfd.getDeclaredType()));
        if (cfd.getInitialValue() != null) {

            printLine(spacers, "├─ Access Modifier: " + cfd.getAccessModifier());
            printLine(spacers, "└─ Initializer:");
            printASTNode(cfd.getInitialValue(), spacers, "└─ ");
        } else printLine(spacers, "└─ Access Modifier: " + cfd.getAccessModifier());
        spacers.removeLast();
    }

    /// A helper method to print the details of a class method declaration, including its return type, parameters, name, body, and access modifier.
    /// @param cmd The ClassMethodDeclaration node representing the class method declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the class method declaration.
    private static void printMethodDeclaration(ClassMethodDeclaration cmd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Access Modifier: " + cmd.getAccessModifier());
        printFunctionElements(cmd, spacers);
    }

    /// A helper method to print the details of a class constructor declaration, including its parameters, body, and access modifier.
    /// @param ccd The ClassConstructorDeclaration node representing the class constructor declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the class constructor declaration.
    private static void printConstructorDeclaration(ClassConstructorDeclaration ccd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        var params = ccd.getParameters();
        printLine(spacers, "├─ Access Modifier: " + ccd.getAccessModifier());
        printLine(spacers, "├─ Parameters: " + params.length);
        printLine(spacers, "└─ Body:");
        printASTNode(ccd.getBody(), spacers, "└─ ");
        printParameters(params, spacers);
    }

    /// A helper method to print the details of a return statement, including its return value if present.
    /// @param rs The ReturnStatement node representing the return statement to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printReturnStatement(ReturnStatement rs, List<String> spacers) {

        spacers.add("   ");
        if (rs.getReturnValue() != null) {

            printLine(spacers, "└─ Return Value:");
            printASTNode(rs.getReturnValue(), spacers, "└─ ");
        }
        spacers.removeLast();
    }

    /// A helper method to print the details of a block statement, including its child statements.
    /// @param bs The BlockStatement node representing the block statement to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printBlockStatement(BlockStatement bs, List<String> spacers) {

        spacers.add("   ");
        printChildNodes(bs.getStatements(), spacers, "└─ Statements: ");
        spacers.removeLast();
    }

    /// A helper method to print the details of an if statement, including its condition, then block, and else block if present.
    /// @param is The IfStatement node representing the if statement to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the if statement.
    private static void printIfStatement(IfStatement is, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printASTNode(is.getCondition(), spacers, "└─ ", true);
        if (is.getElseBlock() != null) {

            printLine(spacers, "├─ Then:");
            printASTNode(is.getThenBlock(), spacers, "└─ ", true);
            printLine(spacers, "└─ Else:");
            printASTNode(is.getElseBlock(), spacers, "└─ ");
        } else {

            printLine(spacers, "└─ Then:");
            printASTNode(is.getThenBlock(), spacers, "└─ ");
        }
        spacers.removeLast();
    }

    /// A helper method to print the details of a while statement, including its condition and body.
    /// @param ws The WhileStatement node representing the while statement to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the while statement.
    private static void printWhileStatement(WhileStatement ws, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printASTNode(ws.getCondition(), spacers, "└─ ", true);
        printLine(spacers, "└─ Body:");
        printASTNode(ws.getBody(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a for statement, including its condition, initializer, increment, and body.
    /// @param fs The ForStatement node representing the for statement to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the for statement.
    private static void printForStatement(ForStatement fs, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printASTNode(fs.getCondition(), spacers, "└─ ", true);
        if (fs.getInitialization() != null) {

            printLine(spacers, "├─ Initializer:");
            printASTNode(fs.getInitialization(), spacers, "└─ ", true);
        }
        if (fs.getIncrement() != null) {

            printLine(spacers, "├─ Increment:");
            printASTNode(fs.getIncrement(), spacers, "└─ ", true);
        }
        printLine(spacers, "└─ Body:");
        printASTNode(fs.getBody(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a variable declaration statement, including its name, type, and initializer if present.
    /// @param vds The VariableDeclarationStatement node representing the variable declaration to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the variable declaration statement.
    private static void printVariableDeclaration(VariableDeclarationStatement vds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Name: " + vds.getName());
        if (vds.getInitialValue() != null) {

            printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(vds.getDeclaredType()));
            printLine(spacers, "└─ Initializer:");
            printASTNode(vds.getInitialValue(), spacers, "└─ ");
        } else printLine(spacers, "└─ Type: " + buildTypeStringWithSizes(vds.getDeclaredType()));
        spacers.removeLast();
    }

    /// A helper method to print the details of an expression statement, including its expression if present.
    /// @param es The ExpressionStatement node representing the expression statement to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the expression statement.
    private static void printExpressionStatement(ExpressionStatement es, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        if (es.getExpression() != null) {

            printLine(spacers, "└─ Expression:");
            printASTNode(es.getExpression(), spacers, "└─ ");
        }
        spacers.removeLast();
    }

    /// A helper method to print the details of an assignment expression, including its target and value.
    /// @param ae The AssignmentExpression node representing the assignment expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param vLine A boolean indicating whether to include vertical lines in the visual structure for the child nodes of the assignment expression.
    private static void printAssignmentExpression(AssignmentExpression ae, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Target:");
        printASTNode(ae.getTarget(), spacers, "└─ ", true);
        printLine(spacers, "└─ Value:");
        printASTNode(ae.getValue(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a binary expression, including its operator, left operand, and right operand.
    /// @param be The BinaryExpression node representing the binary expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printBinaryExpression(BinaryExpression be, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + be.getOperator().getType());
        printLine(spacers, "├─ Left:");
        printASTNode(be.getLeft(), spacers, "└─ ", true);
        printLine(spacers, "└─ Right:");
        printASTNode(be.getRight(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a unary expression, including its operator and operand.
    /// @param ue The UnaryExpression node representing the unary expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printUnaryExpression(UnaryExpression ue, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + ue.getOperator().getType());
        printLine(spacers, "└─ Operand:");
        printASTNode(ue.getOperand(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a call expression, including its callee and arguments.
    /// @param ce The CallExpression node representing the call expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printCallExpression(CallExpression ce, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Callee:");
        printASTNode(ce.getCallee(), spacers, "└─ ", true);
        printChildNodes(ce.getArguments(), spacers, "└─ Arguments: ");
        spacers.removeLast();
    }

    /// A helper method to print the details of a member access expression, including the member name and the object being accessed.
    /// @param mae The MemberAccessExpression node representing the member access expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printMemberAccess(MemberAccessExpression mae, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Member: " + mae.getMemberName());
        printLine(spacers, "└─ Object:");
        printASTNode(mae.getObject(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of an array access expression, including the array being accessed and the index.
    /// @param aae The ArrayAccessExpression node representing the array access expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printArrayAccess(ArrayAccessExpression aae, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Array:");
        printASTNode(aae.getArray(), spacers, "└─ ", true);
        printLine(spacers, "└─ Index:");
        printASTNode(aae.getIndex(), spacers, "└─ ");
        spacers.removeLast();
    }

    /// A helper method to print the details of an identifier literal expression, including its name.
    /// @param ile The IdentifierLiteralExpression node representing the identifier literal expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printIdentifier(IdentifierLiteralExpression ile, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Identifier: " + ile.getName());
        spacers.removeLast();
    }

    /// A helper method to print the details of a number literal expression, including its value.
    /// @param nle The NumberLiteralExpression node representing the number literal expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printNumberLiteral(NumberLiteralExpression nle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + nle.getValue());
        spacers.removeLast();
    }

    /// A helper method to print the details of a string literal expression, including its value.
    /// @param sle The StringLiteralExpression node representing the string literal expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printStringLiteral(StringLiteralExpression sle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + sle.getValue());
        spacers.removeLast();
    }

    /// A helper method to print the details of a character literal expression, including its value.
    /// @param cle The CharLiteralExpression node representing the character literal expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printCharLiteral(CharLiteralExpression cle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: '" + cle.getValue() + "'");
        spacers.removeLast();
    }

    /// A helper method to print the details of a boolean literal expression, including its value.
    /// @param ble The BoolLiteralExpression node representing the boolean literal expression to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printBoolLiteral(BoolLiteralExpression ble, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + ble.getValue());
        spacers.removeLast();
    }

    /// A helper method to print the details of function parameters, including their names and types.
    /// @param params An array of FunctionParameter objects representing the parameters to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    private static void printParameters(FunctionParameter[] params, List<String> spacers) {

        spacers.add("│  ");
        for (int i = 0; i < params.length; i++) {

            var param = params[i];
            var isLast = (i == params.length - 1);
            var prefix = isLast ? "└─ " : "├─ ";
            printLine(spacers, prefix + param.getName() + ": " + buildTypeStringWithSizes(param.getType()));
        }
        spacers.removeLast();
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