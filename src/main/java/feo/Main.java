package feo;

import org.antlr.v4.runtime.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

public class Main {
    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Specify input file name");
            return;
        }
        final CharStream in;
        try {
            in = CharStreams.fromFileName(args[0]);
        } catch (final IOException e) {
            System.err.println("Error during reading input file: " + e.getMessage());
            return;
        }
        final StringWriter string = new StringWriter();
        final BufferedWriter writer = new BufferedWriter(string);
        try {
            ObfuscateTool.obfuscate(in, writer);
            System.out.println(string);
        } catch (final ObfuscateParseException e) {
            System.err.println("Parsing error: " + e.getMessage());
        } catch (final IOException e) {
            System.err.println("Unexpected IO error: " + e.getMessage());
        }
    }
}