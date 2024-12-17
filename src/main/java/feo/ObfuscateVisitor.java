package feo;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ObfuscateVisitor extends ProgramBaseVisitor<Void> {
    private static final int NAME_WIDTH = 10;
    private final BufferedWriter writer;
    private IOException exception = null;
    private final List<Map<String, String>> scopes = new ArrayList<>();
    private final int baseName;
    private Set<String> usedNames = new HashSet<>();
    private final Random random = new Random();
    private static final char[] POSSIBLE_CHARS = {'0', '1', 'O', 'I'};
    private int ident = 0;
    private final static String IDENT = "    ";

    public ObfuscateVisitor(final BufferedWriter writer) {
        this.writer = writer;
        this.baseName = random.nextInt(1 << (NAME_WIDTH - 1), 1 << NAME_WIDTH);
    }

    private void write(final char c) {
        try {
            writer.write(c);
        } catch (final IOException e) {
            exception = e;
        }
    }

    private void withIdent(final String string) {
        ident();
        write(string);
    }

    private void withIdent(final char c) {
        ident();
        write(c);
    }

    private void ident() {
        for (int i = 0; i < ident; ++i) {
            write(IDENT);
        }
    }

    private void increaseIdent() {
        ++ident;
    }

    private void decreaseIdent() {
        --ident;
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

    private void enterScope() {
        scopes.add(new HashMap<>());
    }

    private void exitScope() {
        usedNames.removeAll(scopes.getLast().values());
        scopes.removeLast();
    }

    @Override
    public Void visitProgram(ProgramParser.ProgramContext ctx) {
        enterScope();
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree child = ctx.getChild(i);
            visit(child);
            if (child.getText().equals(";")) {
                newLine();
            }
        }
        exitScope();
        return null;
    }

    @Override
    public Void visitSingleVariableDeclaration(ProgramParser.SingleVariableDeclarationContext ctx) {
        visit(ctx.declarators());
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
        char[] chars = new char[NAME_WIDTH];
        for (int i = 0; i < NAME_WIDTH; ++i) {
            chars[i] = POSSIBLE_CHARS[random.nextInt(i == NAME_WIDTH - 1 ? 1 : 2) * 2 + ((baseName & (1 << i)) == 0 ? 0 : 1)];
        }
        final String result = String.valueOf(chars);
        return usedNames.add(result) ? result : newName();
    }

    private String lookup(final String variable) {
        return scopes
                .reversed()
                .stream()
                .map(names -> names.get(variable))
                .dropWhile(Objects::isNull)
                .findFirst()
                .orElse(variable);
    }

    @Override
    public Void visitVariableDeclaration(ProgramParser.VariableDeclarationContext ctx) {
        visit(ctx.type());
        write(' ');
        final int childCount = ctx.getChildCount();
        for (int i = 1; i < childCount; ++i) {
            visit(ctx.getChild(i));
            if (ctx.getChild(i).getText().equals(",")) {
                write(" ");
            }
        }
        return null;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        write(node.getText());
        return null;
    }

    @Override
    public Void visitFunctionDeclaration(ProgramParser.FunctionDeclarationContext ctx) {
        enterScope();
        visit(ctx.type());
        write(' ');
        visit(ctx.IDENT());
        write('(');
        visit(ctx.arguments());
        write(") ");
        visit(ctx.scope());
        newLine();
        exitScope();
        return null;
    }

    @Override
    public Void visitArguments(ProgramParser.ArgumentsContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree child = ctx.getChild(i);
            visit(child);
            if (child.getText().equals(",")) {
                write(' ');
            }
        }
        return null;
    }

    @Override
    public Void visitArgumentDeclaration(ProgramParser.ArgumentDeclarationContext ctx) {
        visit(ctx.type());
        write(' ');
        visit(ctx.singleVariableDeclaration());
        return null;
    }

    @Override
    public Void visitName(ProgramParser.NameContext ctx) {
        write(lookup(ctx.getText()));
        return null;
    }

    @Override
    public Void visitScope(ProgramParser.ScopeContext ctx) {
        withIdent('{');
        increaseIdent();
        newLine();
        for (int i = 1; i < ctx.getChildCount() - 1; ++i) {
            visit(ctx.getChild(i));
        }
        decreaseIdent();
        withIdent('}');
        newLine();
        return null;
    }

    @Override
    public Void visitStatement(ProgramParser.StatementContext ctx) {
        ident();
        super.visitStatement(ctx);
        if (ctx.oneLineStatement() != null) {
            newLine();
        }
        return null;
    }
}
