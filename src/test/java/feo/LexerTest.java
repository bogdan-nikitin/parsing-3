package feo;


import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;


class LexerTest {
    final Random random = new Random();

    void withSpaces(final String ... tokens) {

    }

    void test(final String text, int ... types) {
        final ProgramLexer lexer = new ProgramLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingListener());
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        for (int i = 0; i < types.length; ++i) {
            Assertions.assertEquals(types[i], tokens.get(i).getType());
        }
        Assertions.assertEquals(ProgramLexer.EOF, tokens.get(types.length).getType());
    }

    @Test
    void testSimple() {
        test("123", ProgramLexer.INT);
        test("\"Hello World\"", ProgramLexer.STRING);
        test("'c'", ProgramLexer.CHAR);
        test("// hello world");
        test("/* hello world */");
        test("#include <stdio.h>", ProgramLexer.PREPROCESSOR);
    }

    @Test
    void testComplicated() {
        test("0", ProgramLexer.INT);
        test("9", ProgramLexer.INT);
        test("123456789", ProgramLexer.INT);
        test("\"a \\\"b\ta \\n 'c' \\r a\\0ba\"", ProgramLexer.STRING);
        final char[] ESCAPE = {'0', '\'', 'n', 'r', 't', '"'};
        for (final char c : ESCAPE) {
            test("'\\" + c + "'", ProgramLexer.CHAR);
        }
        test("/*hello \n world \r\n foo \r\n bar*/");
    }
}