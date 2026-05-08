package parser.ast;

import java.util.List;

/// Interface for AST nodes that can describe themselves for tree printing.
///
/// Every concrete {@link AstNode} should implement this interface, providing:
/// <ul>
///   <li>{@link #toPrintString()} – a one-line label for the node itself.</li>
///   <li>{@link #getPrintEntries()} – the ordered list of informational lines and child
///       subtrees that belong to this node.</li>
/// </ul>
///
/// {@link printer.AstPrinter} uses these two methods to render the whole AST tree,
/// managing indentation depth and branch characters (├─ / └─ / │) centrally.
public interface Printable {

    /// A single entry produced by {@link #getPrintEntries()}.
    ///
    /// Entries come in three shapes:
    /// <ul>
    ///   <li>{@link Info}     – a plain text annotation line (e.g. "Name: foo").</li>
    ///   <li>{@link Child}    – a labeled reference to a single child {@link AstNode}.</li>
    ///   <li>{@link Children} – a labeled reference to an ordered array of {@link AstNode}s.</li>
    /// </ul>
    sealed interface PrintEntry permits PrintEntry.Info, PrintEntry.Child, PrintEntry.Children {

        /// A plain-text annotation line shown directly in the tree.
        record Info(String text) implements PrintEntry {}

        /// A labeled reference to exactly one child node (may be {@code null}).
        record Child(String label, AstNode node) implements PrintEntry {}

        /// A labeled reference to an array of child nodes (shown with a count header).
        record Children(String label, AstNode[] nodes) implements PrintEntry {}
    }

    /// Returns the one-line label shown for this node in the printed tree.
    ///
    /// Implementations typically return something like
    /// {@code "BinaryExpression [line 5]"}.
    ///
    /// @return A non-null, single-line description of this node.
    String toPrintString();

    /// Returns the ordered list of print entries that represent this node's
    /// attributes and child subtrees.
    ///
    /// @return A list of {@link PrintEntry} values; never {@code null}.
    List<PrintEntry> getPrintEntries();
}
