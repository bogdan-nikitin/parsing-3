package feo;

import org.antlr.v4.runtime.*;

import java.io.BufferedWriter;
import java.io.IOException;

public class ObfuscateTool {
    public static void obfuscate(final CharStream in, final BufferedWriter out) throws IOException {
        final ProgramLexer lexer = new ProgramLexer(in);
        final ThrowingListener listener = new ThrowingListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ProgramParser parser = new ProgramParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        final var program = parser.program();
        new ObfuscateVisitor(out).visit(program);
        out.flush();
    }

}
