import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import parser.project.FileSystemStdLibLoader;
import parser.project.ProjectParser;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesMutuallyReferencingClassesAcrossFiles() throws Exception {

        var a = tempDir.resolve("A.nv");
        var b = tempDir.resolve("B.nv");

        Files.writeString(a, "public class A { B b; }");
        Files.writeString(b, "public class B { A a; }");

        var parser = new ProjectParser(new FileSystemStdLibLoader(tempDir.resolve("no-stdlib")), false);
        var result = assertDoesNotThrow(() -> parser.parse(tempDir));
        assertEquals(2, result.astByFile().size());
    }

    @Test
    void loadsStdlibFromSourceModules() throws Exception {

        var stdlibDir = tempDir.resolve("stdlib");
        Files.createDirectories(stdlibDir);
        Files.writeString(stdlibDir.resolve("core.nv"), "void print(string value) { }");
        Files.writeString(tempDir.resolve("main.nv"), "print(\"hello\");");

        var parser = new ProjectParser(new FileSystemStdLibLoader(stdlibDir), false);
        assertDoesNotThrow(() -> parser.parse(tempDir.resolve("main.nv")));
    }
}
