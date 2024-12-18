package feo;

import org.antlr.v4.runtime.*;

import java.io.BufferedWriter;

public class ObfuscateTool {
    private static class ErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new ObfuscateParseException("line " + line + ":" + charPositionInLine + " " + msg, e);
        }
    }

    public static void obfuscate(final CharStream in, final BufferedWriter out) {
        final ProgramLexer lexer = new ProgramLexer(in);
        final ErrorListener listener = new ErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ProgramParser parser = new ProgramParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        final var program = parser.program();
        new ObfuscateVisitor(out).visit(program);
    }
}
