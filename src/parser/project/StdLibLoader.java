package parser.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/// Abstraction for loading Nova standard-library source files.
public interface StdLibLoader {
    List<Path> loadStdLibFiles() throws IOException;
}
