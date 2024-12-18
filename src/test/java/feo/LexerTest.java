package feo;


import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;


class LexerTest {
    final Random random = new Random();
    final static int MAX_SPACE = 16;
    final char[] SPACES = {' ', '\t', '\r', '\n'};

    String space() {
        final int length = random.nextInt(1, MAX_SPACE);
        char[] spaces = new char[length];
        for (int i = 0; i < length; ++i) {
            spaces[i] = SPACES[random.nextInt(SPACES.length)];
        }
        return String.valueOf(spaces);
    }

    String withSpaces(final String... tokens) {
        final StringBuilder builder = new StringBuilder();
        for (final String token : tokens) {
            builder.append(space());
            builder.append(token);
        }
        builder.append(space());
        return builder.toString();
    }

    void testTokens(final List<Token> tokens, final int... types) {
        for (int i = 0; i < types.length; ++i) {
            Assertions.assertEquals(types[i], tokens.get(i).getType());
        }
        Assertions.assertEquals(ProgramLexer.EOF, tokens.get(types.length).getType());
    }

    void testTypes(final String text, final int... types) {
        testTokens(getTokens(text), types);
    }

    void testUnnamed(final String... tokens) {
        Assertions.assertEquals(tokens.length + 1, getTokens(withSpaces(tokens)).size());
    }

    void testThrows(final String ... tokens) {
        Assertions.assertThrows(ObfuscateParseException.class, () -> {
            testUnnamed(tokens);
        });
    }

    List<Token> getTokens(final String text) {
        final ProgramLexer lexer = new ProgramLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingListener());
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        return tokens.getTokens();
    }

    @Test
    void testSimple() {
        testTypes("");
        testTypes("123", ProgramLexer.INT);
        testTypes("\"Hello World\"", ProgramLexer.STRING);
        testTypes("'c'", ProgramLexer.CHAR);
        testTypes("// hello world");
        testTypes("/* hello world */");
        testTypes("#include <stdio.h>", ProgramLexer.PREPROCESSOR);
        testTypes("foo", ProgramLexer.IDENT);
    }

    @Test
    void testComplicated() {
        testTypes("0", ProgramLexer.INT);
        testTypes("9", ProgramLexer.INT);
        testTypes("123456789", ProgramLexer.INT);
        testTypes("\"a \\\"b\ta \\n 'c' 42 \\r a\\0ba\"", ProgramLexer.STRING);
        final char[] ESCAPE = {'0', '\'', 'n', 'r', 't', '"'};
        for (final char c : ESCAPE) {
            testTypes("'\\" + c + "'", ProgramLexer.CHAR);
        }
        testTypes("/*hello \n world \r\n foo \r\n bar*/");
        testTypes("FooBar", ProgramLexer.IDENT);
        testTypes("_foobar_", ProgramLexer.IDENT);
        testTypes("foo42", ProgramLexer.IDENT);
        testTypes("FOO42", ProgramLexer.IDENT);
        testTypes("42foo", ProgramLexer.INT, ProgramLexer.IDENT);
    }

    @Test
    void testMany() {
        testTypes("123 foo \"string\" 'h' 'i' /* abc */ // hello \n 42",
                ProgramLexer.INT, ProgramLexer.IDENT, ProgramLexer.STRING,
                ProgramLexer.CHAR, ProgramLexer.CHAR, ProgramLexer.INT);
    }

    @Test
    void testSpaces() {
        testTypes(withSpaces("foo", "bar", "42", "\"foo\"", "'9'",
                        "/*\n\r\t*/", "// comment\n", "#include <lib.h>"),
                ProgramLexer.IDENT, ProgramLexer.IDENT, ProgramLexer.INT, ProgramLexer.STRING,
                ProgramLexer.CHAR, ProgramLexer.PREPROCESSOR);
    }

    @Test
    void testGrammar() {
        testUnnamed("+", "-", "*", "/", "+=", "-=", "*=", "/=", "++", "--", "(", ")",
        "{", "}", "==", "!=", "<", ">", "<=", ">=", "&", "if", "return", "for", "while", "else",
                ",", ";");
    }

    @Test
    void testThrows() {
        testThrows("$");
        testThrows("@");
        testThrows("!");
        testThrows("`foo`");
    }
}