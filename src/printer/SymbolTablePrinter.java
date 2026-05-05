package printer;

import parser.ast.SymbolTable;
import parser.ast.nodes.Symbol;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;

import java.util.List;

/// Utility class for printing the symbol table in a readable format, showing the hierarchy of scopes and symbols.
public class SymbolTablePrinter {

    /// Prints a symbol with its name and type information, formatted with indentation to show hierarchy.
    /// @param symbol The symbol to print.
    /// @param indent The indentation string to use for formatting.
    /// @param isLast Indicates whether this symbol is the last one in its scope, affecting the formatting of the tree structure.
    private static void printSymbol(Symbol symbol, String indent, boolean isLast) {

        var line = new StringBuilder(indent);
        line.append(isLast ? "└─ " : "├─ ");

        switch (symbol) {

            case ClassDeclarationStatement cls -> line.append(cls.getName()).append(" (Class)");
            case ClassFieldDeclaration cfd -> line.append(cfd.getName()).append(" (Class Field: ").append(AstPrinter.buildTypeStringWithSizes(cfd.getDeclaredType())).append(")");
            case ClassMethodDeclaration cmd -> line.append(cmd.getName()).append(" (Class Method: ").append(AstPrinter.buildTypeStringWithSizes(cmd.getDeclaredType())).append(")");
            case FunctionDeclarationStatement fds -> line.append(fds.getName()).append(" (Function: ").append(AstPrinter.buildTypeStringWithSizes(fds.getDeclaredType())).append(")");
            case VariableDeclarationStatement vds -> line.append(vds.getName()).append(" (Variable: ").append(AstPrinter.buildTypeStringWithSizes(vds.getDeclaredType())).append(")");
            case FunctionParameter fp -> line.append(fp.getName()).append(" (Function Parameter: ").append(AstPrinter.buildTypeStringWithSizes(fp.getType())).append(")");
            default -> line.append(symbol.getName()).append(" (").append(symbol.getClass().getSimpleName()).append(")");
        }

        System.out.println(line);
    }

    /// Determines whether a given symbol introduces a new scope (e.g., classes and functions).
    /// @param symbol The symbol to check.
    /// @return true if the symbol introduces a new scope, false otherwise.
    private static boolean introducesScope(Symbol symbol) {
        return symbol instanceof ClassDeclarationStatement || symbol instanceof FunctionDeclarationStatement;
    }

    /// Recursively prints the symbol table and its child scopes, showing the hierarchy of symbols and scopes.
    /// @param scope The symbol table to print.
    /// @param indent The indentation string to use for formatting.
    /// @param isLast Indicates whether this scope is the last one in its parent scope, affecting the formatting of the tree structure.
    /// @param label The label to use for this scope (e.g., "Global Scope", "Scope", "Anonymous Scope").
    private static void printScope(SymbolTable scope, String indent, boolean isLast, String label) {

        System.out.println(indent + (isLast ? "└─ " : "├─ ") + label);
        var childIndent = indent + (isLast ? "   " : "│  ");

        var symbols = scope.symbols();
        var children = scope.children();
        var attachedChildIndex = 0;

        for (var i = 0; i < symbols.size(); i++) {

            var symbol = symbols.get(i);
            var symbolHasScope = introducesScope(symbol) && attachedChildIndex < children.size();
            var hasMoreSymbols = i < symbols.size() - 1;
            var hasAnonymousChildrenAfter = attachedChildIndex + (symbolHasScope ? 1 : 0) < children.size();
            var isLastSymbol = !hasMoreSymbols && !hasAnonymousChildrenAfter;

            printSymbol(symbol, childIndent, isLastSymbol && !symbolHasScope);
            if (symbolHasScope) {

                var symbolIndent = childIndent + (isLastSymbol ? "   " : "│  ");
                printScope(children.get(attachedChildIndex++), symbolIndent, true, "Scope");
            }
        }

        while (attachedChildIndex < children.size()) {

            var isLastAnonymous = attachedChildIndex == children.size() - 1;
            printScope(children.get(attachedChildIndex++), childIndent, isLastAnonymous, "Anonymous Scope");
        }
    }

    /// Prints the symbol table starting from the given node, showing the hierarchy of scopes and symbols.
    /// @param node The root symbol table to print, typically representing the global scope.
    /// @param spacers A list of strings to use as indentation for the root scope, allowing for proper formatting when printing nested scopes.
    public static void printSymbolTableNode(SymbolTable node, List<String> spacers) {

        if (node == null) return;

        var rootIndent = new StringBuilder();
        for (var spacer : spacers) rootIndent.append(spacer);
        printScope(node, rootIndent.toString(), true, "Global Scope");
    }
}
