package feo;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ObfuscateVisitor extends ProgramBaseVisitor<Void> {
    private static final int NAME_WIDTH = 30;
    private final BufferedWriter writer;
    private IOException exception = null;
    private final List<Map<String, String>> scopes = new ArrayList<>();
    private final int baseName;
    private Set<String> usedNames = new HashSet<>();
    private final Random random = new Random();
    private static final char[] POSSIBLE_CHARS = {'1', '0', 'I', 'O'};
    private static final int BEGIN_CHARS = 2;

    public ObfuscateVisitor(final BufferedWriter writer) {
        this.writer = writer;
        this.baseName = random.nextInt(1 << (NAME_WIDTH - 1), 1 << NAME_WIDTH);
    }

    private void write(final String string) {
        try {
            writer.write(string);
        } catch (final IOException e) {
            exception = e;
        }
    }

    private void newLine() {
        try {
            writer.newLine();
        } catch (final IOException e) {
            exception = e;
        }
    }

    @Override
    public Void visitProgram(ProgramParser.ProgramContext ctx) {
        scopes.add(new HashMap<>());
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree child = ctx.getChild(i);
            visit(child);
            if (child.getText().equals(";")) {
                write(";");
            }
            newLine();
        }
        scopes.removeLast();
        return null;
    }

    @Override
    public Void visitSingleVariableDeclaration(ProgramParser.SingleVariableDeclarationContext ctx) {
        final String name = newName();
        scopes.getLast().put(ctx.IDENT().getText(), name);
        write(name);
        final ParseTree expression = ctx.expression();
        if (expression != null) {
            write(" = ");
            visit(expression);
        }
        return null;
    }

    private String newName() {
        char[] result = new char[NAME_WIDTH];
        for (int i = 0; i < NAME_WIDTH; ++i) {
            if ((baseName & (1 << i)) == 0) {
                result[i] = POSSIBLE_CHARS[random.nextInt(i == NAME_WIDTH - 1 ? BEGIN_CHARS : POSSIBLE_CHARS.length)];
            }
        }
        return String.valueOf(result);
    }

    private String lookup(final String variable) {
        return scopes
                .reversed()
                .stream()
                .map(names -> names.get(variable))
                .dropWhile(Objects::isNull)
                .findFirst()
                .orElse(null);
    }
}
