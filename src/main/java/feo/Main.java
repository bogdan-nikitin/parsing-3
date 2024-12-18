package feo;

import org.antlr.v4.runtime.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

public class Main {
    public static void main(String[] args) throws IOException {
//        CharStream in = CharStreams.fromString("int main () { 1 }");
        var in = CharStreams.fromFileName("test.cpp");
//        CharStream in = CharStreams.fromString("{ int a = 1; if (1) { int b = 5; } else return 6; }");
        ProgramLexer lexer = new ProgramLexer(in);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ProgramParser parser = new ProgramParser(tokens);
        var prg = parser.program();
        System.out.println(prg.toStringTree());
        System.out.println(tokens.getText());
        var s = new StringWriter();
//        System.out.println(lexer.error);
//        System.out.println(parser.getNumberOfSyntaxErrors());
        BufferedWriter w = new BufferedWriter(s);
        System.out.println("Visiting");
        new ObfuscateVisitor(w).visit(prg);
//        ObfuscateTool.obfuscate(in, w);
        w.flush();
//        System.out.println("Visited");
        System.out.println(s);
    }
}