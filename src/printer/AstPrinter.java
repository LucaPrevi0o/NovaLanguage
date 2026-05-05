package printer;

import java.util.List;

import parser.ast.AstNode;
import parser.ast.Printable;
import parser.ast.Printable.PrintEntry;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;
import token.ReturnType;

/// A utility class for printing the Abstract Syntax Tree (AST) of a program in a readable format.
///
/// Every concrete {@link AstNode} implements {@link Printable}, supplying:
/// <ul>
///   <li>{@link Printable#toPrintString()} – the one-line label shown for that node.</li>
///   <li>{@link Printable#getPrintEntries()} – the ordered list of informational lines and child
///       sub-trees belonging to that node.</li>
/// </ul>
///
/// This class is responsible only for managing indentation depth and the branch characters
/// ({@code ├─}, {@code └─}, {@code │}) used to render the tree.
public class AstPrinter {

    private AstPrinter() {}

    // ─── Public API ────────────────────────────────────────────────────────────

    /// Prints the AST starting from the given node with the specified header.
    /// @param node    The root node of the AST to print.
    /// @param spacers A list of strings used for indentation and visual structure.
    /// @param header  A string to prefix the root node (e.g. {@code "├─ "} or {@code "└─ "}).
    public static void printASTNode(AstNode node, List<String> spacers, String header) {
        printASTNode(node, spacers, header, false);
    }

    // ─── Core recursive printer ────────────────────────────────────────────────

    /// Recursively prints a node and all of its children.
    ///
    /// @param node    The AST node to print.
    /// @param spacers Mutable list of indentation segments accumulated from ancestor nodes.
    /// @param header  Branch prefix for this node ({@code "├─ "} or {@code "└─ "}).
    /// @param vLine   When {@code true}, a {@code "│  "} spacer is added so that the vertical
    ///                bar continues past this subtree to the next sibling at the parent level.
    ///                When {@code false}, a plain {@code "   "} spacer is added instead.
    private static void printASTNode(AstNode node, List<String> spacers, String header, boolean vLine) {

        if (node == null) return;
        spacers.add(vLine ? "│  " : "   ");
        if (node instanceof Printable p) {
            printLine(spacers, header + p.toPrintString());
            printEntries(p.getPrintEntries(), spacers);
        } else {
            printLine(spacers, header + node.getClass().getSimpleName() + " [line " + node.getLine() + "]");
        }
        spacers.removeLast();
    }

    // ─── Entry iteration ───────────────────────────────────────────────────────

    /// Iterates over a node's {@link PrintEntry} list and prints each one, using
    /// {@code ├─} for non-last entries and {@code └─} for the last entry.
    private static void printEntries(List<PrintEntry> entries, List<String> spacers) {

        for (int i = 0; i < entries.size(); i++) {

            var entry = entries.get(i);
            boolean isLast = (i == entries.size() - 1);
            String prefix = isLast ? "└─ " : "├─ ";
            boolean hasMoreAfter = !isLast;

            switch (entry) {

                case PrintEntry.Info info ->
                    printLine(spacers, prefix + info.text());

                case PrintEntry.Child child -> {
                    printLine(spacers, prefix + child.label() + ":");
                    printASTNode(child.node(), spacers, "└─ ", hasMoreAfter);
                }

                case PrintEntry.Children children -> {
                    printLine(spacers, prefix + children.label() + ": " + children.nodes().length);
                    for (int j = 0; j < children.nodes().length; j++) {
                        boolean isLastChild = (j == children.nodes().length - 1);
                        printASTNode(children.nodes()[j], spacers,
                                isLastChild ? "└─ " : "├─ ",
                                !isLastChild || hasMoreAfter);
                    }
                }
            }
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
        var baseTypeStr = type.getBaseType().get();

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
