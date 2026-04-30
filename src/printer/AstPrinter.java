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

/**
 * Rewritten src.printer.AstPrinter that uses a simple depth-based indentation scheme
 * consistent with src.printer.SymbolTablePrinter. Each indent level is three spaces.
 */
public class AstPrinter {

    public static void printASTNode(AstNode node, List<String> spacers, String header) { printASTNode(node, spacers, header, false); }

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
                case BoolLiteralExpression ble -> printBoolLiteral(ble, spacers);
                default -> {}
            }
        } catch (Exception e) { e.printStackTrace(); }
        spacers.removeLast();
    }

    private static void printLine(List<String> spacers, String text) {

        var prefix = new StringBuilder();
        for (var s: spacers) prefix.append(s);
        System.out.println(prefix + text);
    }

    private static void printChildNodes(AstNode[] nodes, List<String> spacers, String header) { printChildNodes(nodes, spacers, header, false); }

    private static void printChildNodes(AstNode[] nodes, List<String> spacers, String header, boolean vLine) {

        printLine(spacers, header + nodes.length);
        for (int i = 0; i < nodes.length; i++) {

            var childPrefix = (i == nodes.length - 1) ? "└─ " : "├─ ";
            printASTNode(nodes[i], spacers, childPrefix, vLine);
        }
    }

    private static void printFunctionDeclaration(FunctionDeclarationStatement fds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printFunctionElements(fds, spacers);
    }

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

    private static void printMethodDeclaration(ClassMethodDeclaration cmd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Access Modifier: " + cmd.getAccessModifier());
        printFunctionElements(cmd, spacers);
    }

    private static void printConstructorDeclaration(ClassConstructorDeclaration ccd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        var params = ccd.getParameters();
        printLine(spacers, "├─ Access Modifier: " + ccd.getAccessModifier());
        printLine(spacers, "├─ Parameters: " + params.length);
        printLine(spacers, "└─ Body:");
        printASTNode(ccd.getBody(), spacers, "└─ ");
        printParameters(params, spacers);
    }

    private static void printReturnStatement(ReturnStatement rs, List<String> spacers) {

        spacers.add("   ");
        if (rs.getReturnValue() != null) {

            printLine(spacers, "└─ Return Value:");
            printASTNode(rs.getReturnValue(), spacers, "└─ ");
        }
        spacers.removeLast();
    }

    private static void printBlockStatement(BlockStatement bs, List<String> spacers) {

        spacers.add("   ");
        printChildNodes(bs.getStatements(), spacers, "└─ Statements: ");
        spacers.removeLast();
    }

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

    private static void printWhileStatement(WhileStatement ws, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Condition:");
        printASTNode(ws.getCondition(), spacers, "└─ ", true);
        printLine(spacers, "└─ Body:");
        printASTNode(ws.getBody(), spacers, "└─ ");
        spacers.removeLast();
    }

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

    private static void printExpressionStatement(ExpressionStatement es, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        if (es.getExpression() != null) {

            printLine(spacers, "└─ Expression:");
            printASTNode(es.getExpression(), spacers, "└─ ");
        }
        spacers.removeLast();
    }

    private static void printAssignmentExpression(AssignmentExpression ae, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "│  " : "   ");
        printLine(spacers, "├─ Target:");
        printASTNode(ae.getTarget(), spacers, "└─ ", true);
        printLine(spacers, "└─ Value:");
        printASTNode(ae.getValue(), spacers, "└─ ");
        spacers.removeLast();
    }

    private static void printBinaryExpression(BinaryExpression be, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + be.getOperator().getType());
        printLine(spacers, "├─ Left:");
        printASTNode(be.getLeft(), spacers, "└─ ", true);
        printLine(spacers, "└─ Right:");
        printASTNode(be.getRight(), spacers, "└─ ");
        spacers.removeLast();
    }

    private static void printUnaryExpression(UnaryExpression ue, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + ue.getOperator().getType());
        printLine(spacers, "└─ Operand:");
        printASTNode(ue.getOperand(), spacers, "└─ ");
        spacers.removeLast();
    }

    private static void printCallExpression(CallExpression ce, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Callee:");
        printASTNode(ce.getCallee(), spacers, "└─ ", true);
        printChildNodes(ce.getArguments(), spacers, "└─ Arguments: ");
        spacers.removeLast();
    }

    private static void printMemberAccess(MemberAccessExpression mae, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Member: " + mae.getMemberName());
        printLine(spacers, "└─ Object:");
        printASTNode(mae.getObject(), spacers, "└─ ");
        spacers.removeLast();
    }

    private static void printArrayAccess(ArrayAccessExpression aae, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Array:");
        printASTNode(aae.getArray(), spacers, "└─ ", true);
        printLine(spacers, "└─ Index:");
        printASTNode(aae.getIndex(), spacers, "└─ ");
        spacers.removeLast();
    }

    private static void printIdentifier(IdentifierLiteralExpression ile, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Identifier: " + ile.getName());
        spacers.removeLast();
    }

    private static void printNumberLiteral(NumberLiteralExpression nle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + nle.getValue());
        spacers.removeLast();
    }

    private static void printStringLiteral(StringLiteralExpression sle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + sle.getValue());
        spacers.removeLast();
    }

    private static void printBoolLiteral(BoolLiteralExpression ble, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + ble.getValue());
        spacers.removeLast();
    }

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

    public static String buildTypeStringWithSizes(ReturnType type) {

        if (type == null) return "null";
        var baseType = type.getBaseType();
        String baseTypeStr = baseType.get();

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