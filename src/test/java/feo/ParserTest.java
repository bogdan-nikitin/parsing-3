package feo;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class ParserTest extends BaseFileTester {
    @Override
    protected void handleFile(final CharStream stream) {
        final ProgramLexer lexer = new ProgramLexer(stream);
        final ThrowingListener listener = new ThrowingListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ProgramParser parser = new ProgramParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        parser.program();
    }
}
