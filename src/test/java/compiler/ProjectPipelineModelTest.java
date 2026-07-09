package compiler;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.Token;
import lexer.token.family.Special;
import org.junit.jupiter.api.Test;
import semantic.declaration.DeclarationKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for Phase 6 project-pipeline model foundations.
public class ProjectPipelineModelTest {

    @Test
    void sourceFilePreservesIdentityTextAndOrigin() {

        var source = SourceFile.inMemory("main.nv", "public class Main {}");

        assertEquals("main.nv", source.identity());
        assertEquals("public class Main {}", source.source());
        assertEquals(SourceOrigin.MEMORY, source.origin());
    }

    @Test
    void sourceFileRejectsBlankIdentityAndNormalizesNulls() {

        assertThrows(IllegalArgumentException.class, () -> SourceFile.inMemory(" ", "class A {}"));

        var source = new SourceFile("generated", null, null);
        assertEquals("", source.source());
        assertEquals(SourceOrigin.MEMORY, source.origin());
    }

    @Test
    void sourceFileCanUseNormalizedDiskPathIdentity() {

        var source = SourceFile.fromPath(Path.of("src", "..", "src", "main.nv"), "class Main {}");

        assertEquals(Path.of("src", "main.nv").toString(), source.identity());
        assertEquals(SourceOrigin.DISK, source.origin());
    }

    @Test
    void compilationUnitDefensivelyCopiesCollections() {

        var source = SourceFile.inMemory("main.nv", "class Main {}");
        var tokens = new ArrayList<>(List.of(new Token(Special.EOF, 1, 1)));
        var diagnostic = Diagnostic.error(DiagnosticPhase.LEXER, "bad token");
        var lexerDiagnostics = new ArrayList<>(List.of(diagnostic));

        var unit = new CompilationUnit(source, tokens, List.of(), lexerDiagnostics, List.of());
        tokens.clear();
        lexerDiagnostics.clear();

        assertEquals(1, unit.tokens().size());
        assertEquals(1, unit.lexerDiagnostics().size());
        assertThrows(UnsupportedOperationException.class, () -> unit.tokens().add(new Token(Special.EOF, 2, 1)));
        assertThrows(UnsupportedOperationException.class, () -> unit.lexerDiagnostics().clear());
    }

    @Test
    void compilationUnitWrapsSyntaxDiagnosticsWithSourceFile() {

        var source = SourceFile.inMemory("bad.nv", "@");
        var lexerDiagnostic = Diagnostic.error(DiagnosticPhase.LEXER, "Unrecognized token");
        var parserDiagnostic = Diagnostic.error(DiagnosticPhase.PARSER, "Expected declaration");

        var unit = new CompilationUnit(source, List.of(), List.of(), List.of(lexerDiagnostic), List.of(parserDiagnostic));
        var diagnostics = unit.diagnostics();

        assertTrue(unit.hasSyntaxErrors());
        assertEquals(2, diagnostics.size());
        assertSame(source, diagnostics.get(0).sourceFile());
        assertSame(lexerDiagnostic, diagnostics.get(0).diagnostic());
        assertSame(source, diagnostics.get(1).sourceFile());
        assertSame(parserDiagnostic, diagnostics.get(1).diagnostic());
    }

    @Test
    void sourceDiagnosticCanRepresentProjectLevelDiagnostic() {

        var diagnostic = Diagnostic.error(DiagnosticPhase.SEMANTIC, "Duplicate source file identity");
        var sourceDiagnostic = SourceDiagnostic.project(diagnostic);

        assertFalse(sourceDiagnostic.hasSourceFile());
        assertNull(sourceDiagnostic.sourceFile());
        assertSame(diagnostic, sourceDiagnostic.diagnostic());
    }

    @Test
    void compilerProducesUnitsForAllInputsBeforeStoppingOnSyntaxErrors() {

        var good = SourceFile.inMemory("good.nv", "public class Good { }");
        var bad = SourceFile.inMemory("bad.nv", "@");

        var context = new Compiler().compile(List.of(good, bad));

        assertEquals(2, context.units().size());
        assertTrue(context.hasErrors());
        assertTrue(context.hasSyntaxErrors());
        assertNull(context.rootScope());
        assertTrue(context.declarations().all().isEmpty());
        assertTrue(context.diagnostics().stream().anyMatch(diagnostic ->
            diagnostic.hasSourceFile() &&
            diagnostic.sourceFile() == bad &&
            diagnostic.diagnostic().phase() == DiagnosticPhase.LEXER
        ));
    }

    @Test
    void compilerResolvesClassReferencesAcrossTwoFiles() {

        var context = new Compiler().compile(
            SourceFile.inMemory("a.nv", """
                public class A {
                  public B peer;
                }
                """),
            SourceFile.inMemory("b.nv", "public class B { }")
        );

        assertTrue(context.diagnostics().isEmpty(), () -> context.diagnostics().toString());
        assertEquals(2, context.units().size());
        assertEquals(2, context.declarations().byKind(DeclarationKind.CLASS).size());
        assertNotNull(context.rootScope());
        assertEquals(1, context.rootScope().findLocal("A").size());
        assertEquals(1, context.rootScope().findLocal("B").size());
    }

    @Test
    void compilerWrapsSemanticDiagnosticsWithSourceWhenLocationIdentifiesOneUnit() {

        var context = new Compiler().compile(
            SourceFile.inMemory("a.nv", """
                public class A {
                  public Missing field;
                }
                """),
            SourceFile.inMemory("b.nv", "public class B { }")
        );

        var diagnostic = context.diagnostics().stream()
            .filter(sourceDiagnostic -> sourceDiagnostic.diagnostic().phase() == DiagnosticPhase.SEMANTIC)
            .filter(sourceDiagnostic -> sourceDiagnostic.diagnostic().message().contains("Missing"))
            .findFirst()
            .orElseThrow();

        assertTrue(diagnostic.hasSourceFile());
        assertEquals("a.nv", diagnostic.sourceFile().identity());
    }

    @Test
    void compilerReportsDuplicateSourceIdentityAsProjectDiagnostic() {

        var context = new Compiler().compile(
            SourceFile.inMemory("same.nv", "public class A { }"),
            SourceFile.inMemory("same.nv", "public class B { }")
        );

        assertTrue(context.hasErrors());
        assertNull(context.rootScope());
        assertTrue(context.diagnostics().stream().anyMatch(diagnostic ->
            !diagnostic.hasSourceFile() &&
            diagnostic.diagnostic().phase() == DiagnosticPhase.COMPILER &&
            diagnostic.diagnostic().message().contains("Duplicate source file identity")
        ));
    }
}
