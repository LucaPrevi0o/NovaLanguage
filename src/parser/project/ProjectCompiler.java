package parser.project;

import backend.BackendPipeline;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/// High-level project compilation facade: parse -> semantic -> backend.
public class ProjectCompiler {

    private final ProjectParser projectParser;
    private final BackendPipeline backendPipeline;

    public ProjectCompiler() {

        this.projectParser = new ProjectParser();
        this.backendPipeline = new BackendPipeline();
    }

    public Map<Path, String> compileToPseudoAssembly(Path rootOrFile) throws java.io.IOException {

        var parseResult = projectParser.parse(rootOrFile);
        var output = new LinkedHashMap<Path, String>();
        for (var entry : parseResult.astByFile().entrySet())
            output.put(entry.getKey(), backendPipeline.compileToPseudoAssembly(entry.getValue()));
        return output;
    }
}
