import src.lexer.Lexer;
import src.lexer.Token;
import src.parser.Parser;
import src.parser.ast.nodes.StatementNode;
import src.parser.ast.AstNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    
    public static void main(String[] args) {
        
        String sourceCode;
        
        // Check if a file path was provided as argument
        if (args.length > 0) {

            String filePath = args[0];
            try {

                sourceCode = readFile(filePath);
                System.out.println("=== Reading from file: " + filePath + " ===\n");
            } catch (IOException e) {

                System.err.println("Error reading file: " + e.getMessage());
                return;
            }
        } else {

            // Use default example code
            sourceCode = """
                int x = 10;
                if (x > 5) {
                    for (int i = 0; i < x; i = i + 1) {
                        x = x + i;
                    }
                    return x * 2;
                } else {
                    return 0;
                }
                """;
            System.out.println("=== Using default example code ===\n");
        }
        
        // Create lexer and tokenize
        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();
        
        // Print tokens
        System.out.println("=== TOKENS ===\n");
        for (Token token : tokens) System.out.println(token);
        
        // Parse tokens into AST
        System.out.println("\n=== PARSING ===\n");
        
        try {

            Parser parser = new Parser(tokens);
            List<StatementNode> ast = parser.parse();
            
            System.out.println("Parsing completed successfully!");
            System.out.println("Total AST nodes: " + ast.size());
            
            System.out.println("\n=== AST STRUCTURE ===\n");
            for (int i = 0; i < ast.size(); i++) {

                StatementNode node = ast.get(i);
                printASTNode(node, 0);
            }

            System.out.println("\n=== SYMBOL TABLE ===\n");
            printSymbolTableGrouped(parser.getSymbolTable(), ast);
            
        } catch (Exception e) {

            System.err.println("\n=== PARSING ERROR ===");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Recursively print AST node structure with indentation.
     */
    private static void printASTNode(AstNode node, int depth) {

        printNodeHeader(node, depth);
        printNodeDetails(node, depth);
    }
    
    /**
     * Print the header line for any AST node.
     */
    private static void printNodeHeader(AstNode node, int depth) {

        String indent = indent(depth);
        String className = node.getClass().getSimpleName();
        System.out.println(indent + "└─ " + className + " [line " + node.getLine() + "]");
    }
    
    /**
     * Print type-specific details for a node.
     */
    private static void printNodeDetails(AstNode node, int depth) {

        try {

            if (node instanceof src.parser.ast.nodes.statement.ClassDeclarationStatement cd) printClassDeclaration(cd, depth);
            else if (node instanceof src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration cmd) printMethodDeclaration(cmd, depth);
            else if (node instanceof src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration cfd) printMemberDeclaration(cfd, depth);
            else if (node instanceof src.parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration ccd) printConstructorDeclaration(ccd, depth);
            else if (node instanceof src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement fds) printFunctionDeclaration(fds, depth);
            else if (node instanceof src.parser.ast.nodes.statement.BlockStatement bs) printBlockStatement(bs, depth);
            else if (node instanceof src.parser.ast.nodes.statement.conditional.IfStatement is) printIfStatement(is, depth);
            else if (node instanceof src.parser.ast.nodes.statement.conditional.WhileStatement ws) printWhileStatement(ws, depth);
            else if (node instanceof src.parser.ast.nodes.statement.conditional.ForStatement fs) printForStatement(fs, depth);
            else if (node instanceof src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement vds) printVariableDeclaration(vds, depth);
            else if (node instanceof src.parser.ast.nodes.statement.ExpressionStatement es) printExpressionStatement(es, depth);
            else if (node instanceof src.parser.ast.nodes.expression.AssignmentExpression ae) printAssignmentExpression(ae, depth);
            else if (node instanceof src.parser.ast.nodes.expression.BinaryExpression be) printBinaryExpression(be, depth);
            else if (node instanceof src.parser.ast.nodes.expression.UnaryExpression ue) printUnaryExpression(ue, depth);
            else if (node instanceof src.parser.ast.nodes.expression.CallExpression ce) printCallExpression(ce, depth);
            else if (node instanceof src.parser.ast.nodes.expression.access.MemberAccessExpression mae) printMemberAccess(mae, depth);
            else if (node instanceof src.parser.ast.nodes.expression.access.ArrayAccessExpression aae) printArrayAccess(aae, depth);
            else if (node instanceof src.parser.ast.nodes.expression.literal.IdentifierLiteralExpression ile) printIdentifier(ile, depth);
            else if (node instanceof src.parser.ast.nodes.expression.literal.NumberLiteralExpression nle) printNumberLiteral(nle, depth);
            else if (node instanceof src.parser.ast.nodes.expression.literal.StringLiteralExpression sle) printStringLiteral(sle, depth);
            else if (node instanceof src.parser.ast.nodes.expression.literal.BoolLiteralExpression ble) printBoolLiteral(ble, depth);
            else if (node instanceof src.parser.ast.nodes.statement.ReturnStatement rs) printReturnStatement(rs, depth);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    // ========== Statement Printers ==========

    private static String getTypeString(src.token.ReturnType type) { return printNonPrimitiveType(type.getBaseType()) + "[]".repeat(type.getSizes().length); }

    private static String printNonPrimitiveType(src.token.TokenFamily type) {
        
        if (type instanceof src.token.family.PrimitiveType) return type.get();
        var classDeclaration = src.token.TypeRegistry.getClassDeclaration(type.get());
        var superClass = classDeclaration.getSuperClass();
        if (superClass != null) return printNonPrimitiveType(src.token.TypeRegistry.getTokenFamilyByName(superClass.getName())) + "." + type.get();
        else return type.get();
    }
    
    private static void printFunctionDeclaration(src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement fds, int depth) {

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

    private static void printClassDeclaration(src.parser.ast.nodes.statement.ClassDeclarationStatement cds, int depth) {

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

    private static void printMemberDeclaration(src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration cfd, int depth) {

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

    private static void printMethodDeclaration(src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration cmd, int depth) {

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

    private static void printConstructorDeclaration(src.parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration ccd, int depth) {

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

    private static void printReturnStatement(src.parser.ast.nodes.statement.ReturnStatement rs, int depth) {

        var returnValue = rs.getReturnValue();
        if (returnValue != null) {

            printLine(depth + 1, "└─ Return Value:");
            printASTNode(returnValue, depth + 2);
        }
    }
    
    private static void printBlockStatement(src.parser.ast.nodes.statement.BlockStatement bs, int depth) {

        var statements = bs.getStatements();
        printLine(depth + 1, "└─ Statements: " + statements.length);
        for (var stmt : statements) printASTNode(stmt, depth + 2);
    }
    
    private static void printIfStatement(src.parser.ast.nodes.statement.conditional.IfStatement is, int depth) {

        printLine(depth + 1, "├─ Condition:");
        printASTNode(is.getCondition(), depth + 2);
        
        printLine(depth + 1, "├─ Then:");
        printASTNode(is.getThenBlock(), depth + 2);
        
        if (is.getElseBlock() != null) {

            printLine(depth + 1, "└─ Else:");
            printASTNode(is.getElseBlock(), depth + 2);
        }
    }
    
    private static void printWhileStatement(src.parser.ast.nodes.statement.conditional.WhileStatement ws, int depth) {

        printLine(depth + 1, "├─ Condition:");
        printASTNode(ws.getCondition(), depth + 2);
        
        printLine(depth + 1, "└─ Body:");
        printASTNode(ws.getBody(), depth + 2);
    }
    
    private static void printForStatement(src.parser.ast.nodes.statement.conditional.ForStatement fs, int depth) {

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
    
    private static void printVariableDeclaration(src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement vds, int depth) {

        var type = vds.getDeclaredType();
        var initializer = vds.getInitialValue();
        
        printLine(depth + 1, "├─ Name: " + vds.getName());
        
        // Build type string with sizes
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
    private static String buildTypeStringWithSizes(src.token.ReturnType type) {

        var baseType = type.getBaseType();
        String baseTypeStr;
        if (baseType instanceof src.token.family.PrimitiveType) baseTypeStr = baseType.get();
        else baseTypeStr = printNonPrimitiveType(baseType);
        var sizes = type.getSizes();
        
        if (sizes.length == 0) return baseTypeStr;
        
        StringBuilder result = new StringBuilder(baseTypeStr);
        for (var size : sizes) {

            result.append("[");
            if (size != null) {

                // Show size value if it's a simple literal
                if (size instanceof src.parser.ast.nodes.expression.literal.NumberLiteralExpression nle) result.append(nle.getValue());
                else result.append("expr");
            }
            result.append("]");
        }
        
        return result.toString();
    }
    
    private static void printExpressionStatement(src.parser.ast.nodes.statement.ExpressionStatement es, int depth) {

        printLine(depth + 1, "└─ Expression:");
        printASTNode(es.getExpression(), depth + 2);
    }
    
    // ========== Expression Printers ==========
    
    private static void printAssignmentExpression(src.parser.ast.nodes.expression.AssignmentExpression ae, int depth) {

        printLine(depth + 1, "├─ Target:");
        printASTNode(ae.getTarget(), depth + 2);
        printLine(depth + 1, "└─ Value:");
        printASTNode(ae.getValue(), depth + 2);
    }
    
    private static void printBinaryExpression(src.parser.ast.nodes.expression.BinaryExpression be, int depth) {

        printLine(depth + 1, "├─ Operator: " + be.getOperator().getType());
        printLine(depth + 1, "├─ Left:");
        printASTNode(be.getLeft(), depth + 2);
        printLine(depth + 1, "└─ Right:");
        printASTNode(be.getRight(), depth + 2);
    }
    
    private static void printUnaryExpression(src.parser.ast.nodes.expression.UnaryExpression ue, int depth) {

        printLine(depth + 1, "├─ Operator: " + ue.getOperator().getType());
        printLine(depth + 1, "└─ Operand:");
        printASTNode(ue.getOperand(), depth + 2);
    }
    
    private static void printCallExpression(src.parser.ast.nodes.expression.CallExpression ce, int depth) {

        var arguments = ce.getArguments();
        printLine(depth + 1, "├─ Callee:");
        printASTNode(ce.getCallee(), depth + 2);
        printLine(depth + 1, "└─ Arguments: " + arguments.length);
        for (var arg : arguments) printASTNode(arg, depth + 2);
    }
    
    private static void printMemberAccess(src.parser.ast.nodes.expression.access.MemberAccessExpression mae, int depth) {

        printLine(depth + 1, "├─ Member: " + mae.getMemberName());
        printLine(depth + 1, "└─ Object:");
        printASTNode(mae.getObject(), depth + 2);
    }
    
    private static void printArrayAccess(src.parser.ast.nodes.expression.access.ArrayAccessExpression aae, int depth) {

        printLine(depth + 1, "├─ Array:");
        printASTNode(aae.getArray(), depth + 2);
        printLine(depth + 1, "└─ Index:");
        printASTNode(aae.getIndex(), depth + 2);
    }
    
    // ========== Literal Printers ==========
    
    private static void printIdentifier(src.parser.ast.nodes.expression.literal.IdentifierLiteralExpression ile, int depth) {
        printLine(depth + 1, "└─ Identifier: " + ile.getName());
    }
    
    private static void printNumberLiteral(src.parser.ast.nodes.expression.literal.NumberLiteralExpression nle, int depth) {
        printLine(depth + 1, "└─ Value: " + nle.getValue());
    }
    
    private static void printStringLiteral(src.parser.ast.nodes.expression.literal.StringLiteralExpression sle, int depth) {
        printLine(depth + 1, "└─ Value: " + sle.getValue());
    }
    
    private static void printBoolLiteral(src.parser.ast.nodes.expression.literal.BoolLiteralExpression ble, int depth) {
        printLine(depth + 1, "└─ Value: " + ble.getValue());
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Generate indentation string for a given depth.
     */
    private static String indent(int depth) { return "   ".repeat(depth); }
    
    /**
     * Print a line with proper indentation.
     */
    private static void printLine(int depth, String text) { System.out.println(indent(depth) + text); }
    
    // ========== Symbol Table Printer ==========
    
    /**
     * Print symbol table with hierarchical scope structure.
     * Shows the actual scope tree with parent-child relationships.
     */
    private static void printSymbolTableGrouped(src.parser.ast.SymbolTable symbolTable, List<StatementNode> ast) {
        System.out.println("└─ Global Scope");
        printScopeRecursive(symbolTable, 1, true);
    }
    
    /**
     * Recursively print a scope and all its children.
     */
    private static void printScopeRecursive(src.parser.ast.SymbolTable scope, int depth, boolean isRoot) {
        var symbols = scope.getSymbols();
        var children = scope.getChildren();
        
        // Print symbols in this scope
        if (!symbols.isEmpty()) {
            String symbolsPrefix = (children.isEmpty() && !isRoot) ? "└─" : "├─";
            printLine(depth, symbolsPrefix + " Symbols: " + symbols.size());
            
            for (int i = 0; i < symbols.size(); i++) {
                var symbol = symbols.get(i);
                boolean isLastSymbol = (i == symbols.size() - 1) && children.isEmpty();
                String prefix = isLastSymbol ? "└─" : "├─";
                printSymbolInfo(symbol, depth + 1, prefix);
            }
        }
        
        // Print child scopes
        for (int i = 0; i < children.size(); i++) {
            var childScope = children.get(i);
            boolean isLast = (i == children.size() - 1);
            
            // Determine scope type based on symbols
            String scopeType = determineScopeType(childScope);
            String prefix = isLast ? "└─" : "├─";
            
            printLine(depth, prefix + " " + scopeType);
            printScopeRecursive(childScope, depth + 1, false);
        }
    }
    
    /**
     * Determine the type of scope based on its symbols.
     */
    private static String determineScopeType(src.parser.ast.SymbolTable scope) {
        var symbols = scope.getSymbols();
        
        if (symbols.isEmpty()) {
            return "Block Scope (empty)";
        }
        
        // Check if this scope contains a class declaration
        for (var symbol : symbols) {
            if (symbol instanceof src.parser.ast.nodes.statement.ClassDeclarationStatement cds) {
                // This is NOT the class scope itself, but the scope of its parent
                // The class scope is the child scope
                break;
            }
        }
        
        // Check if it's a class scope (contains fields/methods but NOT from parent)
        boolean hasFields = false;
        boolean hasMethods = false;
        String className = null;
        
        for (var symbol : symbols) {
            if (symbol instanceof src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration) {
                hasFields = true;
            }
            if (symbol instanceof src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration cmd) {
                hasMethods = true;
                // Try to extract class name from method's fully qualified name
                if (className == null) {
                    String fullName = printNonPrimitiveType(cmd.getDeclaredType().getBaseType());
                    if (fullName.contains(".")) {
                        // Extract class name from something like "MainClass.method"
                        String[] parts = fullName.split("\\.");
                        if (parts.length > 0) {
                            className = parts[parts.length - 2]; // Get second-to-last part
                        }
                    }
                }
            }
        }
        
        if (hasFields || hasMethods) {
            return className != null ? "Class Scope (" + className + ")" : "Class Scope";
        }
        
        // Check if it's a function/method scope (contains parameters)
        for (var symbol : symbols) {
            if (symbol instanceof src.parser.ast.nodes.statement.declaration.FunctionParameter) {
                // Look for function/method name in parent scope
                if (scope.getParent() != null) {
                    var parentSymbols = scope.getParent().getSymbols();
                    
                    // Try to find the function that owns these parameters
                    for (var parentSym : parentSymbols) {
                        if (parentSym instanceof src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement fds) {
                            // Check if this function has these parameters
                            for (var param : fds.getParameters()) {
                                if (param == symbol) {
                                    return "Function Scope (" + fds.getName() + ")";
                                }
                            }
                        }
                        if (parentSym instanceof src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration cmd) {
                            // Check if this method has these parameters
                            for (var param : cmd.getParameters()) {
                                if (param == symbol) {
                                    return "Method Scope (" + cmd.getName() + ")";
                                }
                            }
                        }
                    }
                }
                return "Function/Method Scope";
            }
        }
        
        // Default to block scope
        return "Block Scope";
    }
    
    /**
     * Print information about a single symbol.
     */
    private static void printSymbolInfo(src.parser.ast.nodes.Symbol symbol, int depth, String prefix) {
        String symbolInfo = getSymbolDescription(symbol);
        printLine(depth, prefix + " " + symbolInfo);
    }
    
    /**
     * Get a human-readable description of a symbol.
     */
    private static String getSymbolDescription(src.parser.ast.nodes.Symbol symbol) {
        if (symbol instanceof src.parser.ast.nodes.statement.ClassDeclarationStatement cds) {
            String desc = cds.getName() + " (class)";
            if (cds.getSuperClass() != null) {
                desc += " extends " + cds.getSuperClass().getName();
            }
            return desc + " [line " + symbol.getLine() + "]";
        }
        
        if (symbol instanceof src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration cmd) {
            return cmd.getName() + ": " + getTypeString(cmd.getDeclaredType()) + 
                   " (method, " + cmd.getAccessModifier() + ") [line " + symbol.getLine() + "]";
        }
        
        if (symbol instanceof src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration cfd) {
            return cfd.getName() + ": " + getTypeString(cfd.getDeclaredType()) + 
                   " (field, " + cfd.getAccessModifier() + ") [line " + symbol.getLine() + "]";
        }
        
        if (symbol instanceof src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement fds) {
            return fds.getName() + ": " + getTypeString(fds.getDeclaredType()) + 
                   " (function) [line " + symbol.getLine() + "]";
        }
        
        if (symbol instanceof src.parser.ast.nodes.statement.declaration.FunctionParameter fp) {
            return fp.getName() + ": " + getTypeString(fp.getType()) + 
                   " (parameter) [line " + symbol.getLine() + "]";
        }
        
        if (symbol instanceof src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement vds) {
            return vds.getName() + ": " + getTypeString(vds.getDeclaredType()) + 
                   " (variable) [line " + symbol.getLine() + "]";
        }
        
        return symbol.getName() + " [line " + symbol.getLine() + "]";
    }
    
    /**
     * Recursively collect all inner classes.
     */
    private static void collectInnerClasses(src.parser.ast.nodes.statement.ClassDeclarationStatement cds,
                                           java.util.Map<String, src.parser.ast.nodes.statement.ClassDeclarationStatement> map) {
        for (var inner : cds.getInnerClasses()) {
            map.put(inner.getName(), inner);
            collectInnerClasses(inner, map);
        }
    }
    
    /**
     * Read the entire content of a file.
     * 
     * @param filePath the path to the file to read
     * @return the file content as a string
     * @throws IOException if an error occurs reading the file
     */
    private static String readFile(String filePath) throws IOException {

        Path path = Paths.get(filePath);
        return Files.readString(path);
    }
}
