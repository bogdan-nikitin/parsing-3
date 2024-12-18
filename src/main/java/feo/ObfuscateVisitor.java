package feo;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ObfuscateVisitor extends ProgramBaseVisitor<Void> {
    private static final int NAME_WIDTH = 8;
    private static final char[] POSSIBLE_CHARS = {'O', 'I', '0', '1'};
    private final static String IDENT = "    ";
    private final static double NEW_VAR_PROBABILITY = 0.3;
    private final static double NEW_STATEMENT_PROBABILITY = 0.5;
    private final BufferedWriter writer;
    private final List<Map<String, String>> scopes = new ArrayList<>();
    private final int baseName;
    private final Set<String> usedNames = new HashSet<>();
    private final Random random = new Random();
    private IOException ioException = null;
    private int ident = 0;

    public ObfuscateVisitor(final BufferedWriter writer) {
        this.writer = writer;
        this.baseName = random.nextInt(1 << (NAME_WIDTH - 1), 1 << NAME_WIDTH);
    }

    private boolean toss(final double probability) {
        return random.nextDouble() <= probability;
    }

    public IOException ioException() {
        return ioException;
    }

    private void write(final char c) {
        try {
            writer.write(c);
        } catch (final IOException e) {
            ioException = e;
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
            ioException = e;
        }
    }

    private void newLine() {
        try {
            writer.newLine();
        } catch (final IOException e) {
            ioException = e;
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
            final String text = child.getText();
            if (text.equals(";") || text.startsWith("#")) {
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
            chars[i] = POSSIBLE_CHARS[random.nextInt(i == 0 ? 1 : 2) * 2 + ((baseName & (1 << i)) == 0 ? 0 : 1)];
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
        newLine();
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

    private void insertNewNumberVariable() {
        if (!toss(NEW_VAR_PROBABILITY)) {
            return;
        }
        final String[] TYPES = {
                "int", "double", "float", "long", "short",
                "long long", "long int", "long double", "unsigned int",
                "unsigned char", "unsigned short", "unsigned long",
                "unsigned long long", "signed int", "signed char",
                "signed short", "signed long", "signed long long"
        };
        ident();
        write(TYPES[random.nextInt(TYPES.length)]);
        write(" ");
        write(newName());
        write(" = ");
        write(String.valueOf(random.nextInt(-128, 127)));
        write(';');
        newLine();
    }

    @Override
    public Void visitScope(ProgramParser.ScopeContext ctx) {
        write('{');
        increaseIdent();
        newLine();
        for (int i = 1; i < ctx.getChildCount() - 1; ++i) {
            insertNewNumberVariable();
            ident();
            final ParseTree child = ctx.getChild(i);
            visit(child);
            newLine();
        }
        decreaseIdent();
        withIdent('}');
        return null;
    }

    @Override
    public Void visitIf(ProgramParser.IfContext ctx) {
        write("if (");
        visit(ctx.expression());
        write(") ");
        visit(ctx.statement(0));
        if (ctx.statement(1) != null) {
            write(" else ");
            visit(ctx.statement(1));
        }
        return null;
    }

    @Override
    public Void visitWhile(ProgramParser.WhileContext ctx) {
        write("while (");
        visit(ctx.expression());
        write(") ");
        visit(ctx.statement());
        return null;
    }

    @Override
    public Void visitFor(ProgramParser.ForContext ctx) {
        write("for (");
        visit(ctx.simpleStatement(0));
        write("; ");
        visit(ctx.simpleStatement(1));
        write("; ");
        visit(ctx.simpleStatement(2));
        write(") ");
        visit(ctx.statement());
        return null;
    }

    @Override
    public Void visitReturn(ProgramParser.ReturnContext ctx) {
        write("return ");
        return visit(ctx.expression());
    }

    @Override
    public Void visitExpression(ProgramParser.ExpressionContext ctx) {
        if (ctx.prefixOperator() != null ||
                ctx.postfixOperator() != null ||
                ctx.primary() != null ||
                ctx.getChild(0).getText().equals("(")) {
            return super.visitExpression(ctx);
        }
        if (ctx.getChild(1).getText().equals("(")) {
            visit(ctx.getChild(0));
            write('(');
            for (int i = 2; !ctx.getChild(i).getText().equals(")"); ++i) {
                visit(ctx.getChild(i));
                if (ctx.getChild(i).getText().equals(",")) {
                    write(' ');
                }
            }
            write(")");
            return null;
        }
        visit(ctx.getChild(0));
        write(' ');
        visit(ctx.getChild(1));
        write(' ');
        visit(ctx.getChild(2));
        return null;
    }

    @Override
    public Void visitAssignment(ProgramParser.AssignmentContext ctx) {
        visit(ctx.getChild(0));
        write(' ');
        visit(ctx.getChild(1));
        write(' ');
        visit(ctx.getChild(2));
        return null;
    }

    @Override
    public Void visitType(ProgramParser.TypeContext ctx) {
        visit(ctx.getChild(0));
        for (int i = 1; i < ctx.getChildCount(); ++i) {
            write(' ');
            visit(ctx.getChild(i));
        }
        return null;
    }
}
