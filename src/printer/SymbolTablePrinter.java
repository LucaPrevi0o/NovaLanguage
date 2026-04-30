package src.printer;

import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.Symbol;
import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.parser.ast.nodes.statement.declaration.VariableDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import src.parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;

import java.util.List;

public class SymbolTablePrinter {

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

    private static boolean introducesScope(Symbol symbol) {
        return symbol instanceof ClassDeclarationStatement || symbol instanceof FunctionDeclarationStatement;
    }

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

    public static void printSymbolTableNode(SymbolTable node, List<String> spacers) {

        if (node == null) return;

        var rootIndent = new StringBuilder();
        for (var spacer : spacers) rootIndent.append(spacer);
        printScope(node, rootIndent.toString(), true, "Global Scope");
    }
}
