package printer;

import java.util.List;

import parser.ast.AstNode;
import parser.ast.Printable;
import parser.ast.Printable.PrintEntry;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;
import lexer.token.ReturnType;

/// A utility class for printing the Abstract Syntax Tree (AST) of a program in a readable format.
///
/// Every concrete {@link AstNode} implements {@link Printable}, supplying:
/// <ul>
///   <li>{@link Printable#toPrintString()} – the one-line label shown for that node.</li>
///   <li>{@link Printable#getPrintEntries()} – the ordered list of informational lines and child
///       subtrees belonging to that node.</li>
/// </ul>
///
/// This class is responsible only for managing indentation depth and the branch characters
/// ({@code ├─}, {@code └─}, {@code │}) used to render the tree.
public final class AstPrinter {

    /// Private constructor to prevent instantiation, since this class only contains static methods.
    private AstPrinter() {}

    // ─── Core recursive printer ────────────────────────────────────────────────

    /// Recursively prints a node and all of its children.
    ///
    /// @param node    The AST node to print.
    /// @param spacers Mutable list of indentation segments accumulated from ancestor nodes.
    /// @param header  Branch prefix for this node ({@code "├─ "} or {@code "└─ "}).
    public static void printASTNode(Printable node, List<String> spacers, String header) {

        if (node == null) return;
        printLine(spacers, header + node.toPrintString());
        printEntries(node.getPrintEntries(), spacers, header.equals("├─ "));
    }

    // ─── Entry iteration ───────────────────────────────────────────────────────

    /// Iterates over a node's {@link PrintEntry} list and prints each one, using
    /// {@code ├─} for non-last entries and {@code └─} for the last entry.
    private static void printEntries(List<PrintEntry> entries, List<String> spacers, boolean vLine) {

        for (var i = 0; i < entries.size(); i++) {

            var entry = entries.get(i);
            var isLast = (i == entries.size() - 1);
            var prefix = isLast ? "└─ " : "├─ ";

            spacers.add(vLine ? "│  " : "   ");
            switch (entry) {

                case PrintEntry.Info info -> printLine(spacers, prefix + info.text());
                case PrintEntry.Child child -> {

                    printLine(spacers, prefix + child.label() + ":");
                    spacers.add(!isLast ? "│  " : "   ");
                    printASTNode((Printable) child.node(), spacers, "└─ ");
                    spacers.removeLast();
                }

                case PrintEntry.Children children -> {

                    printLine(spacers, prefix + children.label() + ": " + children.nodes().length);
                    spacers.add(!isLast ? "│  " : "   ");
                    for (var j = 0; j < children.nodes().length; j++) {

                        var isLastChild = (j == children.nodes().length - 1);
                        printASTNode((Printable) children.nodes()[j], spacers, isLastChild ? "└─ " : "├─ ");
                    }
                    spacers.removeLast();
                }
            }
            spacers.removeLast();
        }
    }

    // ─── Shared helpers ────────────────────────────────────────────────────────

    private static void printLine(List<String> spacers, String text) {

        var prefix = new StringBuilder();
        for (var s : spacers) prefix.append(s);
        System.out.println(prefix + text);
    }

    /// Builds a string representation of a return type, including its base type and any array sizes.
    /// @param type The ReturnType to render.
    /// @return A string such as {@code "int"} or {@code "int[3][2]"}.
    public static String buildTypeStringWithSizes(ReturnType type) {

        if (type == null) return "null";
        var baseTypeStr = type.getTokenClass().token();

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
