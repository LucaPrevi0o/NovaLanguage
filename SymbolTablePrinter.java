import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.StatementNode;
import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;

import java.util.List;

public class SymbolTablePrinter {

    public static void printSymbolTableGrouped(SymbolTable symbolTable, List<StatementNode> ast) {

        System.out.println("└─ Global Scope");
        printScopeRecursive(symbolTable, 1, true);
    }

    private static void printScopeRecursive(SymbolTable scope, int depth, boolean isRoot) {

        var symbols = scope.getSymbols();
        var children = scope.getChildren();
        if (!symbols.isEmpty()) {

            var symbolsPrefix = (children.isEmpty() && !isRoot) ? "└─" : "├─";
            printLine(depth, symbolsPrefix + " Symbols: " + symbols.size());
            for (var i = 0; i < symbols.size(); i++) {

                var symbol = symbols.get(i);
                var isLastSymbol = (i == symbols.size() - 1) && children.isEmpty();
                var prefix = isLastSymbol ? "└─" : "├─";
                printSymbolInfo(symbol, depth + 1, prefix);
            }
        }

        for (var i = 0; i < children.size(); i++) {

            var childScope = children.get(i);
            var isLast = (i == children.size() - 1);
            var scopeType = determineScopeType(childScope);
            var prefix = isLast ? "└─" : "├─";
            printLine(depth, prefix + " " + scopeType);
            printScopeRecursive(childScope, depth + 1, false);
        }
    }

    private static String determineScopeType(SymbolTable scope) {

        var symbols = scope.getSymbols();
        if (symbols.isEmpty()) return "Block Scope (empty)";
        var hasFields = false;
        var hasMethods = false;
        String className = null;
        for (var symbol : symbols) {

            if (symbol instanceof ClassFieldDeclaration) hasFields = true;
            if (symbol instanceof ClassMethodDeclaration cmd) {

                hasMethods = true;
                if (className == null) className = cmd.getName();
            }
        }
        if (hasFields || hasMethods) return className != null ? "Class Scope (" + className + ")" : "Class Scope";
        for (var symbol : symbols)
            if (symbol instanceof FunctionParameter) return "Function/Method Scope";
        return "Block Scope";
    }

    private static void printSymbolInfo(src.parser.ast.nodes.Symbol symbol, int depth, String prefix) {

        var symbolInfo = getSymbolDescription(symbol);
        printLine(depth, prefix + " " + symbolInfo);
    }

    private static String getSymbolDescription(src.parser.ast.nodes.Symbol symbol) {

        if (symbol instanceof ClassDeclarationStatement cds) {

            var desc = cds.getName() + " (class)";
            if (cds.getSuperClasses() != null) desc += " extends " + cds.getSuperClasses();
            return desc + " [line " + symbol.getLine() + "]";
        }
        if (symbol instanceof ClassMethodDeclaration cmd)
            return cmd.getName() + ": " + AstPrinter.buildTypeStringWithSizes(cmd.getDeclaredType()) +
            " (method, " + cmd.getAccessModifier() + ") [line " + symbol.getLine() + "]";
        if (symbol instanceof ClassFieldDeclaration cfd)
            return cfd.getName() + ": " + AstPrinter.buildTypeStringWithSizes(cfd.getDeclaredType()) +
            " (field, " + cfd.getAccessModifier() + ") [line " + symbol.getLine() + "]";
        if (symbol instanceof FunctionDeclarationStatement fds)
            return fds.getName() + ": " + AstPrinter.buildTypeStringWithSizes(fds.getDeclaredType()) +
            " (function) [line " + symbol.getLine() + "]";
        if (symbol instanceof FunctionParameter fp)
            return fp.getName() + ": " + AstPrinter.buildTypeStringWithSizes(fp.getType()) +
            " (parameter) [line " + symbol.getLine() + "]";
        if (symbol instanceof VariableDeclarationStatement vds)
            return vds.getName() + ": " + AstPrinter.buildTypeStringWithSizes(vds.getDeclaredType()) +
            " (variable) [line " + symbol.getLine() + "]";
        return symbol.getName() + " [line " + symbol.getLine() + "]";
    }

    private static void printLine(int depth, String text) { System.out.println("   ".repeat(depth) + text); }
}