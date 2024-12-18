package feo;

import org.antlr.v4.runtime.CharStream;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

public class ObfuscateTest extends BaseFileTester {

    @Override
    protected void handleFile(final CharStream stream) {
        final StringWriter stringWriter = new StringWriter();
        final BufferedWriter writer = new BufferedWriter(stringWriter);
        try {
            ObfuscateTool.obfuscate(stream, writer);
            Assertions.assertFalse(stringWriter.toString().isEmpty());
        } catch (final IOException e) {
            Assertions.fail();
        }
    }
}
