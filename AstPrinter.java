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
import src.token.TokenFamily;
import src.token.TypeRegistry;
import src.token.family.PrimitiveType;

public class AstPrinter {
    public static void printASTNode(AstNode node, int depth) {
        printNodeHeader(node, depth);
        printNodeDetails(node, depth);
    }

    private static void printNodeHeader(AstNode node, int depth) {
        String indent = indent(depth);
        String className = node.getClass().getSimpleName();
        System.out.println(indent + "└─ " + className + " [line " + node.getLine() + "]");
    }

    private static void printNodeDetails(AstNode node, int depth) {
        try {
            if (node instanceof ClassDeclarationStatement cd) printClassDeclaration(cd, depth);
            else if (node instanceof ClassMethodDeclaration cmd) printMethodDeclaration(cmd, depth);
            else if (node instanceof ClassFieldDeclaration cfd) printMemberDeclaration(cfd, depth);
            else if (node instanceof ClassConstructorDeclaration ccd) printConstructorDeclaration(ccd, depth);
            else if (node instanceof FunctionDeclarationStatement fds) printFunctionDeclaration(fds, depth);
            else if (node instanceof VariableDeclarationStatement vds) printVariableDeclaration(vds, depth);
            else if (node instanceof ExpressionStatement es) printExpressionStatement(es, depth);
            else if (node instanceof ReturnStatement rs) printReturnStatement(rs, depth);
            else if (node instanceof BlockStatement bs) printBlockStatement(bs, depth);
            else if (node instanceof IfStatement is) printIfStatement(is, depth);
            else if (node instanceof WhileStatement ws) printWhileStatement(ws, depth);
            else if (node instanceof ForStatement fs) printForStatement(fs, depth);
            else if (node instanceof AssignmentExpression ae) printAssignmentExpression(ae, depth);
            else if (node instanceof BinaryExpression be) printBinaryExpression(be, depth);
            else if (node instanceof UnaryExpression ue) printUnaryExpression(ue, depth);
            else if (node instanceof CallExpression ce) printCallExpression(ce, depth);
            else if (node instanceof MemberAccessExpression mae) printMemberAccess(mae, depth);
            else if (node instanceof ArrayAccessExpression aae) printArrayAccess(aae, depth);
            else if (node instanceof IdentifierLiteralExpression ile) printIdentifier(ile, depth);
            else if (node instanceof NumberLiteralExpression nle) printNumberLiteral(nle, depth);
            else if (node instanceof StringLiteralExpression sle) printStringLiteral(sle, depth);
            else if (node instanceof BoolLiteralExpression ble) printBoolLiteral(ble, depth);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========== Statement Printers ==========
    public static String getTypeString(ReturnType type) {
        return printNonPrimitiveType(type.getBaseType()) + "[]".repeat(type.getSizes().length);
    }

    public static String printNonPrimitiveType(TokenFamily type) {

        if (type instanceof PrimitiveType) return type.get();
        var classDeclaration = TypeRegistry.getClassDeclaration(type.get());
        var superClass = classDeclaration.getSuperClass();
        if (superClass != null) return printNonPrimitiveType(TypeRegistry.getTokenFamilyByName(superClass.getName())) + "." + type.get();
        else return type.get();
    }

    private static void printFunctionDeclaration(FunctionDeclarationStatement fds, int depth) {

        var returnType = fds.getDeclaredType();
        var params = fds.getParameters();
        printLine(depth + 1, "├─ Type: " + getTypeString(returnType));
        printLine(depth + 1, "├─ Parameters: " + params.length);
        for (var param : params) {

            var paramType = param.getType();
            String typeStr = buildTypeStringWithSizes(paramType);
            printLine(depth + 2, "└─ " + param.getName() + ": " + typeStr);
        }
        printLine(depth + 1, "├─ Name: " + fds.getName());
        printLine(depth + 1, "└─ Body:");
        printASTNode(fds.getBody(), depth + 2);
    }

    private static void printClassDeclaration(ClassDeclarationStatement cds, int depth) {

        var methods = cds.getMethods();
        var fields = cds.getFields();
        var superClass = cds.getSuperClass();
        var accessModifier = cds.getAccessModifier();
        var innerClasses = cds.getInnerClasses();
        var constructors = cds.getConstructors();

        printLine(depth + 1, "├─ Name: " + cds.getName());
        if (superClass != null) printLine(depth + 1, "├─ Superclass: " + superClass.getName());
        printLine(depth + 1, "├─ Access Modifier: " + accessModifier);
        printLine(depth + 1, "├─ Constructors: " + constructors.length);
        for (var constructor : constructors) printASTNode(constructor, depth + 2);
        printLine(depth + 1, "├─ Inner Classes: " + innerClasses.length);
        for (var innerClass : innerClasses) printASTNode(innerClass, depth + 2);
        printLine(depth + 1, "├─ Fields: " + fields.length);
        for (var field : fields) printASTNode(field, depth + 2);
        printLine(depth + 1, "└─ Methods: " + methods.length);
        for (var method : methods) printASTNode(method, depth + 2);
    }

    private static void printMemberDeclaration(ClassFieldDeclaration cfd, int depth) {

        var memberType = cfd.getDeclaredType();
        var accessModifier = cfd.getAccessModifier();
        var initializer = cfd.getInitialValue();
        printLine(depth + 1, "├─ Name: " + cfd.getName());
        printLine(depth + 1, "├─ Type: " + getTypeString(memberType));
        if (initializer != null) {

            printLine(depth + 1, "├─ Access Modifier: " + accessModifier);
            printLine(depth + 1, "└─ Initializer:");
            printASTNode(initializer, depth + 2);
        } else printLine(depth + 1, "└─ Access Modifier: " + accessModifier);
    }

    private static void printMethodDeclaration(ClassMethodDeclaration cmd, int depth) {

        var returnType = cmd.getDeclaredType();
        var accessModifier = cmd.getAccessModifier();
        var params = cmd.getParameters();
        printLine(depth + 1, "├─ Name: " + cmd.getName());
        printLine(depth + 1, "├─ Type: " + getTypeString(returnType));
        printLine(depth + 1, "├─ Access Modifier: " + accessModifier);
        printLine(depth + 1, "├─ Parameters: " + params.length);
        for (var param : params) {

            var paramType = param.getType();
            String typeStr = buildTypeStringWithSizes(paramType);
            printLine(depth + 2, "└─ " + param.getName() + ": " + typeStr);
        }
        printLine(depth + 1, "└─ Body:");
        printASTNode(cmd.getBody(), depth + 2);
    }

    private static void printConstructorDeclaration(ClassConstructorDeclaration ccd, int depth) {

        var accessModifier = ccd.getAccessModifier();
        var params = ccd.getParameters();
        printLine(depth + 1, "├─ Access Modifier: " + accessModifier);
        printLine(depth + 1, "├─ Parameters: " + params.length);
        for (var param : params) {

            var paramType = param.getType();
            String typeStr = buildTypeStringWithSizes(paramType);
            printLine(depth + 2, "└─ " + param.getName() + ": " + typeStr);
        }
        printLine(depth + 1, "└─ Body:");
        printASTNode(ccd.getBody(), depth + 2);
    }

    private static void printReturnStatement(ReturnStatement rs, int depth) {

        var returnValue = rs.getReturnValue();
        if (returnValue != null) {

            printLine(depth + 1, "└─ Return Value:");
            printASTNode(returnValue, depth + 2);
        }
    }

    private static void printBlockStatement(BlockStatement bs, int depth) {

        var statements = bs.getStatements();
        printLine(depth + 1, "└─ Statements: " + statements.length);
        for (var stmt : statements) printASTNode(stmt, depth + 2);
    }

    private static void printIfStatement(IfStatement is, int depth) {

        printLine(depth + 1, "├─ Condition:");
        printASTNode(is.getCondition(), depth + 2);
        printLine(depth + 1, "├─ Then:");
        printASTNode(is.getThenBlock(), depth + 2);
        if (is.getElseBlock() != null) {

            printLine(depth + 1, "└─ Else:");
            printASTNode(is.getElseBlock(), depth + 2);
        }
    }

    private static void printWhileStatement(WhileStatement ws, int depth) {

        printLine(depth + 1, "├─ Condition:");
        printASTNode(ws.getCondition(), depth + 2);
        printLine(depth + 1, "└─ Body:");
        printASTNode(ws.getBody(), depth + 2);
    }

    private static void printForStatement(ForStatement fs, int depth) {

        printLine(depth + 1, "├─ Condition:");
        printASTNode(fs.getCondition(), depth + 2);
        if (fs.getInitialization() != null) {

            printLine(depth + 1, "├─ Initializer:");
            printASTNode(fs.getInitialization(), depth + 2);
        }
        if (fs.getIncrement() != null) {

            printLine(depth + 1, "├─ Increment:");
            printASTNode(fs.getIncrement(), depth + 2);
        }
        printLine(depth + 1, "└─ Body:");
        printASTNode(fs.getBody(), depth + 2);
    }

    private static void printVariableDeclaration(VariableDeclarationStatement vds, int depth) {

        var type = vds.getDeclaredType();
        var initializer = vds.getInitialValue();
        printLine(depth + 1, "├─ Name: " + vds.getName());
        String typeStr = buildTypeStringWithSizes(type);
        if (initializer != null) {

            printLine(depth + 1, "├─ Type: " + typeStr);
            printLine(depth + 1, "└─ Initializer:");
            printASTNode(initializer, depth + 2);
        } else printLine(depth + 1, "└─ Type: " + typeStr);
    }

    /**
     * Build a type string including size information for arrays.
     */
    public static String buildTypeStringWithSizes(ReturnType type) {

        var baseType = type.getBaseType();
        String baseTypeStr;
        if (baseType instanceof PrimitiveType) baseTypeStr = baseType.get();
        else baseTypeStr = printNonPrimitiveType(baseType);
        var sizes = type.getSizes();
        if (sizes.length == 0) return baseTypeStr;
        var result = new StringBuilder(baseTypeStr);
        for (var size : sizes) {

            result.append("[");
            if (size != null) {

                if (size instanceof NumberLiteralExpression nle) result.append(nle.getValue());
                else result.append("?");
            }
            result.append("]");
        }
        return result.toString();
    }

    private static void printExpressionStatement(ExpressionStatement es, int depth) {

        printLine(depth + 1, "└─ Expression:");
        printASTNode(es.getExpression(), depth + 2);
    }

    // ========== Expression Printers ==========
    private static void printAssignmentExpression(AssignmentExpression ae, int depth) {

        printLine(depth + 1, "├─ Target:");
        printASTNode(ae.getTarget(), depth + 2);
        printLine(depth + 1, "└─ Value:");
        printASTNode(ae.getValue(), depth + 2);
    }

    private static void printBinaryExpression(BinaryExpression be, int depth) {

        printLine(depth + 1, "├─ Operator: " + be.getOperator().getType());
        printLine(depth + 1, "├─ Left:");
        printASTNode(be.getLeft(), depth + 2);
        printLine(depth + 1, "└─ Right:");
        printASTNode(be.getRight(), depth + 2);
    }

    private static void printUnaryExpression(UnaryExpression ue, int depth) {

        printLine(depth + 1, "├─ Operator: " + ue.getOperator().getType());
        printLine(depth + 1, "└─ Operand:");
        printASTNode(ue.getOperand(), depth + 2);
    }

    private static void printCallExpression(CallExpression ce, int depth) {

        var arguments = ce.getArguments();
        printLine(depth + 1, "├─ Callee:");
        printASTNode(ce.getCallee(), depth + 2);
        printLine(depth + 1, "└─ Arguments: " + arguments.length);
        for (var arg : arguments) printASTNode(arg, depth + 2);
    }

    private static void printMemberAccess(MemberAccessExpression mae, int depth) {

        printLine(depth + 1, "├─ Member: " + mae.getMemberName());
        printLine(depth + 1, "└─ Object:");
        printASTNode(mae.getObject(), depth + 2);
    }

    private static void printArrayAccess(ArrayAccessExpression aae, int depth) {

        printLine(depth + 1, "├─ Array:");
        printASTNode(aae.getArray(), depth + 2);
        printLine(depth + 1, "└─ Index:");
        printASTNode(aae.getIndex(), depth + 2);
    }

    // ========== Literal Printers ==========
    private static void printIdentifier(IdentifierLiteralExpression ile, int depth) {
        printLine(depth + 1, "└─ Identifier: " + ile.getName());
    }

    private static void printNumberLiteral(NumberLiteralExpression nle, int depth) {
        printLine(depth + 1, "└─ Value: " + nle.getValue());
    }

    private static void printStringLiteral(StringLiteralExpression sle, int depth) {
        printLine(depth + 1, "└─ Value: " + sle.getValue());
    }

    private static void printBoolLiteral(BoolLiteralExpression ble, int depth) {
        printLine(depth + 1, "└─ Value: " + ble.getValue());
    }

    // ========== Utility Methods ==========
    private static String indent(int depth) { return "   ".repeat(depth); }
    private static void printLine(int depth, String text) { System.out.println(indent(depth) + text); }
}