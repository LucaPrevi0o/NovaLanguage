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
import src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import src.token.ReturnType;
import src.token.family.PrimitiveType;

/**
 * Rewritten AstPrinter that uses a simple depth-based indentation scheme
 * consistent with SymbolTablePrinter. Each indent level is three spaces.
 */
public class AstPrinter {

    public static void printASTNode(AstNode node, List<String> spacers, String header) { printASTNode(node, spacers, header, false); }

    private static void printASTNode(AstNode node, List<String> spacers, String header, boolean vLine) {

        if (node == null) return;
        var className = node.getClass().getSimpleName();
        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, header + className + " [line " + node.getLine() + "]");

        try {

            if (node instanceof ClassDeclarationStatement cds) printClassDeclaration(cds, spacers, !header.equals("└─ "));
            else if (node instanceof FunctionDeclarationStatement fds) printFunctionDeclaration(fds, spacers, !header.equals("└─ "));
            else if (node instanceof ClassMethodDeclaration cmd) printMethodDeclaration(cmd, spacers, !header.equals("└─ "));
            else if (node instanceof ClassConstructorDeclaration ccd) printConstructorDeclaration(ccd, spacers, !header.equals("└─ "));
            else if (node instanceof VariableDeclarationStatement vds) printVariableDeclaration(vds, spacers, !header.equals("└─ "));
            else if (node instanceof ClassFieldDeclaration cfd) printFieldDeclaration(cfd, spacers, !header.equals("└─ "));
            else if (node instanceof ReturnStatement rs) printReturnStatement(rs, spacers);
            else if (node instanceof BlockStatement bs) printBlockStatement(bs, spacers);
            else if (node instanceof IfStatement is) printIfStatement(is, spacers, !header.equals("└─ "));
            else if (node instanceof WhileStatement ws) printWhileStatement(ws, spacers, !header.equals("└─ "));
            else if (node instanceof ForStatement fs) printForStatement(fs, spacers, !header.equals("└─ "));
            else if (node instanceof ExpressionStatement es) printExpressionStatement(es, spacers, !header.equals("└─ "));
            else if (node instanceof AssignmentExpression ae) printAssignmentExpression(ae, spacers, !header.equals("└─ "));
            else if (node instanceof BinaryExpression be) printBinaryExpression(be, spacers);
            else if (node instanceof UnaryExpression ue) printUnaryExpression(ue, spacers);
            else if (node instanceof CallExpression ce) printCallExpression(ce, spacers);
            else if (node instanceof MemberAccessExpression mae) printMemberAccess(mae, spacers);
            else if (node instanceof ArrayAccessExpression aae) printArrayAccess(aae, spacers);
            else if (node instanceof IdentifierLiteralExpression ile) printIdentifier(ile, spacers);
            else if (node instanceof NumberLiteralExpression nle) printNumberLiteral(nle, spacers);
            else if (node instanceof StringLiteralExpression sle) printStringLiteral(sle, spacers);
            else if (node instanceof BoolLiteralExpression ble) printBoolLiteral(ble, spacers);
        } catch (Exception e) { e.printStackTrace(); }
        spacers.remove(spacers.size() - 1);
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

    // ========== Statement Printers ==========
    public static String getTypeString(ReturnType type) { return buildTypeStringWithSizes(type); }

    private static void printFunctionDeclaration(FunctionDeclarationStatement fds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        var returnType = fds.getDeclaredType();
        var params = fds.getParameters();
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(returnType));
        printLine(spacers, "├─ Parameters: " + params.length);
        printParameters(params, spacers);
        printLine(spacers, "├─ Name: " + fds.getName());
        printLine(spacers, "└─ Body:");
        printASTNode(fds.getBody(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printClassDeclaration(ClassDeclarationStatement cds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Name: " + cds.getName());
        if (cds.getSuperClass() != null) printLine(spacers, "├─ Superclass: " + cds.getSuperClass());
        printLine(spacers, "├─ Access Modifier: " + cds.getAccessModifier());
        if (cds.getGenericClassParameter() != null)
            printLine(spacers, "├─ Generic Parameter: " + buildTypeStringWithSizes(cds.getGenericClassParameter()));
        printChildNodes(cds.getConstructors(), spacers, "├─ Constructors: ", true);
        printChildNodes(cds.getInnerClasses(), spacers, "├─ Inner Classes: ", true);
        printChildNodes(cds.getFields(), spacers, "├─ Fields: ", true);
        printChildNodes(cds.getMethods(), spacers, "└─ Methods: ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printFieldDeclaration(ClassFieldDeclaration cfd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Name: " + cfd.getName());
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(cfd.getDeclaredType()));
        if (cfd.getInitialValue() != null) {

            printLine(spacers, "├─ Access Modifier: " + cfd.getAccessModifier());
            printLine(spacers, "└─ Initializer:");
            printASTNode(cfd.getInitialValue(), spacers, "└─ ");
        } else printLine(spacers, "└─ Access Modifier: " + cfd.getAccessModifier());
        spacers.remove(spacers.size() - 1);
    }

    private static void printMethodDeclaration(ClassMethodDeclaration cmd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Name: " + cmd.getName());
        printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(cmd.getDeclaredType()));
        printLine(spacers, "├─ Access Modifier: " + cmd.getAccessModifier());
        var params = cmd.getParameters();
        printLine(spacers, "├─ Parameters: " + params.length);
        printParameters(params, spacers);
        printLine(spacers, "└─ Body:");
        printASTNode(cmd.getBody(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printConstructorDeclaration(ClassConstructorDeclaration ccd, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Access Modifier: " + ccd.getAccessModifier());
        var params = ccd.getParameters();
        printLine(spacers, "├─ Parameters: " + params.length);
        printParameters(params, spacers);
        printLine(spacers, "└─ Body:");
        printASTNode(ccd.getBody(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printReturnStatement(ReturnStatement rs, List<String> spacers) {

        spacers.add("   ");
        if (rs.getReturnValue() != null) {
            
            printLine(spacers, "└─ Return Value:");
            printASTNode(rs.getReturnValue(), spacers, "└─ ");
        }
        spacers.remove(spacers.size() - 1);
    }

    private static void printBlockStatement(BlockStatement bs, List<String> spacers) {

        spacers.add("   ");
        printChildNodes(bs.getStatements(), spacers, "└─ Statements: ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printIfStatement(IfStatement is, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
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
        spacers.remove(spacers.size() - 1);
    }

    private static void printWhileStatement(WhileStatement ws, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Condition:");
        printASTNode(ws.getCondition(), spacers, "└─ ", true);
        printLine(spacers, "└─ Body:");
        printASTNode(ws.getBody(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printForStatement(ForStatement fs, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
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
        spacers.remove(spacers.size() - 1);
    }

    private static void printVariableDeclaration(VariableDeclarationStatement vds, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Name: " + vds.getName());
        if (vds.getInitialValue() != null) {

            printLine(spacers, "├─ Type: " + buildTypeStringWithSizes(vds.getDeclaredType()));
            printLine(spacers, "└─ Initializer:");
            printASTNode(vds.getInitialValue(), spacers, "└─ ");
        } else printLine(spacers, "└─ Type: " + buildTypeStringWithSizes(vds.getDeclaredType()));
        spacers.remove(spacers.size() - 1);
    }

    private static void printExpressionStatement(ExpressionStatement es, List<String> spacers, boolean vLine) {
        
        spacers.add(vLine ? "|  " : "   ");
        if (es.getExpression() != null) {

            printLine(spacers, "└─ Expression:");
            printASTNode(es.getExpression(), spacers, "└─ ");
        }
        spacers.remove(spacers.size() - 1);
    }

    // ========== Expression Printers ==========
    private static void printAssignmentExpression(AssignmentExpression ae, List<String> spacers, boolean vLine) {

        spacers.add(vLine ? "|  " : "   ");
        printLine(spacers, "├─ Target:");
        printASTNode(ae.getTarget(), spacers, "└─ ", true);
        printLine(spacers, "└─ Value:");
        printASTNode(ae.getValue(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printBinaryExpression(BinaryExpression be, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + be.getOperator().getType());
        printLine(spacers, "├─ Left:");
        printASTNode(be.getLeft(), spacers, "└─ ", true);
        printLine(spacers, "└─ Right:");
        printASTNode(be.getRight(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printUnaryExpression(UnaryExpression ue, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Operator: " + ue.getOperator().getType());
        printLine(spacers, "└─ Operand:");
        printASTNode(ue.getOperand(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printCallExpression(CallExpression ce, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Callee:");
        printASTNode(ce.getCallee(), spacers, "└─ ", true);
        printChildNodes(ce.getArguments(), spacers, "└─ Arguments: ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printMemberAccess(MemberAccessExpression mae, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Member: " + mae.getMemberName());
        printLine(spacers, "└─ Object:");
        printASTNode(mae.getObject(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    private static void printArrayAccess(ArrayAccessExpression aae, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "├─ Array:");
        printASTNode(aae.getArray(), spacers, "└─ ", true);
        printLine(spacers, "└─ Index:");
        printASTNode(aae.getIndex(), spacers, "└─ ");
        spacers.remove(spacers.size() - 1);
    }

    // ========== Literal Printers ==========
    private static void printIdentifier(IdentifierLiteralExpression ile, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Identifier: " + ile.getName());
        spacers.remove(spacers.size() - 1);
    }

    private static void printNumberLiteral(NumberLiteralExpression nle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + nle.getValue());
        spacers.remove(spacers.size() - 1);
    }

    private static void printStringLiteral(StringLiteralExpression sle, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + sle.getValue());
        spacers.remove(spacers.size() - 1);
    }

    private static void printBoolLiteral(BoolLiteralExpression ble, List<String> spacers) {

        spacers.add("   ");
        printLine(spacers, "└─ Value: " + ble.getValue());
        spacers.remove(spacers.size() - 1);
    }

    // ========== Parameter printing (reflection-safe) ==========
    private static void printParameters(Object[] params, List<String> spacers) {

        spacers.add("|  ");
        for (int i = 0; i < params.length; i++) {

            var param = params[i];
            Object paramType = null;
            var paramName = "";
            try {

                var mType = param.getClass().getMethod("getType");
                var t = mType.invoke(param);
                paramType = t;
            } catch (Exception ignored) {}
            try {

                var mName = param.getClass().getMethod("getName");
                var n = mName.invoke(param);
                paramName = n != null ? n.toString() : "";
            } catch (Exception ignored) {}

            var typeStr = paramType instanceof ReturnType ? buildTypeStringWithSizes((ReturnType) paramType) : (paramType != null ? paramType.toString() : "unknown");
            var prefix = (i == params.length - 1) ? "└─ " : "├─ ";
            printLine(spacers, prefix + paramName + ": " + typeStr);
        }
        spacers.remove(spacers.size() - 1);
    }

    // ========== Type helpers ==========
    private static String printNonPrimitiveType(Object baseType) {

        if (baseType == null) return "null";
        try {

            var m = baseType.getClass().getMethod("get");
            var res = m.invoke(baseType);
            return res != null ? res.toString() : baseType.toString();
        } catch (Exception e) { return baseType.toString(); }
    }

    public static String buildTypeStringWithSizes(ReturnType type) {

        if (type == null) return "null";
        var baseType = type.getBaseType();
        String baseTypeStr;
        if (baseType instanceof PrimitiveType) baseTypeStr = ((PrimitiveType) baseType).get();
        else baseTypeStr = printNonPrimitiveType(baseType);

        var sizes = type.getSizes();
        if (sizes == null || sizes.length == 0) return baseTypeStr;
        var result = new StringBuilder(baseTypeStr);
        for (var size : sizes) {

            result.append("[");
            if (size != null) {
                
                if (size instanceof NumberLiteralExpression ile) result.append(ile.getValue());
                else result.append(size.toString());
            }
            result.append("]");
        }
        return result.toString();
    }
}