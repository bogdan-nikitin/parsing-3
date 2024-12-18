package feo;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.BufferedWriter;

public class ObfuscateTool {
    public static void obfuscate(final CharStream in, final BufferedWriter out) {
        final ProgramLexer lexer = new ProgramLexer(in);
        lexer.removeErrorListeners();
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ProgramParser parser = new ProgramParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        try {
            final var program = parser.program();
            new ObfuscateVisitor(out).visit(program);
        } catch (final ParseCancellationException e) {
            throw new ObfuscateParseException();
        }
    }
}
