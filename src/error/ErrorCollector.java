package error;

import java.util.ArrayList;
import java.util.List;

/// Collects errors during a process (e.g., parsing) and allows retrieval of all collected errors at the end.
///
/// Errors can happen both in the lexing/parsing phase (e.g., syntax errors) and in the semantic analysis phase
/// (e.g., type errors, undefined variable errors).
/// This class provides a way to accumulate errors without immediately throwing exceptions, enabling the process to
/// continue and report multiple issues in a single run.
/// At the end of the process, the collected errors can be retrieved and handled appropriately (e.g., by throwing a
/// combined exception or by reporting them to the user).
///
/// Members are declared as static synchronized, so that the error collector can be used globally across different
/// components of the system without needing to pass around an instance, while ensuring thread safety.
public class ErrorCollector {

    private static List<Error> errors = new ArrayList<>();

    /// Adds one or more errors to the collector. Null errors are ignored.
    /// @param error One or more {@link Error}s to be collected. Null values will be filtered out and not added to the collection.
    public static synchronized void add(Error error) {

        if (error == null) return;
        var newList = new ArrayList<>(errors);
        newList.add(error);
        errors = List.copyOf(newList);
    }

    /// Clears all collected errors from the collector, resetting it to an empty state.
    public static synchronized void clear() { errors = new ArrayList<>(); }

    /// Checks if any errors have been collected. Returns true if there is at least one error, false otherwise.
    /// @return True if there are collected errors, false if the collector is empty.
    public static synchronized boolean hasErrors() { return !errors.isEmpty(); }

    /// Retrieves the list of collected errors. The returned list is unmodifiable to prevent external modification of the internal state.
    /// @return An unmodifiable list of {@link Error}s that have been collected. If no errors have been collected, this will return an empty list.
    public static synchronized List<Error> getErrors() { return List.copyOf(errors); }
}
