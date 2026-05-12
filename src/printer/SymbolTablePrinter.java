package printer;

import parser.ast.SymbolTable;
import parser.ast.nodes.Symbol;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;

/// Utility class for printing the symbol table in a readable format, showing the hierarchy of scopes and symbols.
public final class SymbolTablePrinter {

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

    /// Recursively prints the symbol table and its child scopes, showing the hierarchy of symbols and scopes.
    /// @param scope The symbol table to print.
    public static void printSymbolTableNode(SymbolTable scope) {
        printScope(scope, "", true, "Global Scope");
    }

    /// Helper method to recursively print a symbol table and its child scopes with proper indentation and formatting.
    /// @param scope  The symbol table to print.
    /// @param indent The indentation string to use for formatting the current level of the symbol table.
    /// @param label  The label to print for the current scope (e.g., "Global Scope", "Scope").
    private static void printScope(SymbolTable scope, String indent, boolean isLast, String label) {

        System.out.println(indent + "└─ " + label);
        var childIndent = indent + (isLast ? "   " : "│  ");

        var symbols = scope.symbols();
        var ownedScopes = scope.ownedScopes().values();
        var anonymousScopes = scope.children().stream().filter(c -> !ownedScopes.contains(c)).toList();

        for (var i = 0; i < symbols.size(); i++) {

            var symbol = symbols.get(i);
            var ownedScope = scope.getOwnedScope(symbol);

            // Last entry overall only if this is the last symbol AND there are no anonymous scopes after
            var isLastSymbolWithScope = (i == symbols.size() - 1) && anonymousScopes.isEmpty() ;
            printSymbol(symbol, childIndent, isLastSymbolWithScope);
            if (ownedScope != null) {

                var symbolIndent = childIndent + (isLastSymbolWithScope ? "   " : "│  ");
                printScope(ownedScope, symbolIndent, true, "Scope");
            }
        }

        for (var i = 0; i < anonymousScopes.size(); i++) {

            var isLastAnon = (i == anonymousScopes.size() - 1);
            printScope(anonymousScopes.get(i), childIndent, isLastAnon, "[anonymous block]");
        }
    }
}
