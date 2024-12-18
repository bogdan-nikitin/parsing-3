package feo;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

public abstract class BaseFileTester {
    private static final String FILES_DIR = "src/test/java/feo/files";

    @Test
    void testFiles() throws IOException {
        final File baseDir = new File(FILES_DIR);
        for (final File file : Objects.requireNonNull(baseDir.listFiles())) {
            handleFile(CharStreams.fromFileName(file.getPath()));
        }
    }

    protected abstract void handleFile(final CharStream stream);
}
