/// # Printers
///
/// Debug and inspection printers for Nova compiler-front-end structures.
///
/// This package contains utilities that render compiler data in a human-readable form during
/// development and testing.
///
/// ## Typical uses
///
/// - Inspect parsed ASTs.
/// - Inspect scope or symbol-like structures.
/// - Produce readable debugging output while developing parser and semantic passes.
/// - Support regression tests that need stable structural output.
///
/// ## Boundary
///
/// Printers should not define compiler semantics. They are observation tools: changing a
/// printer should not change how the compiler parses, resolves, validates, or lowers Nova
/// programs.
package printer;
