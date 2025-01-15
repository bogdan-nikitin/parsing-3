package feo;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ObfuscateVisitor extends ProgramBaseVisitor<Void> {
    private static final int NAME_WIDTH = 8;
    private static final char ONE = 'I';
    private static final char ZERO = 'O';
    private static final char[] POSSIBLE_CHARS = {ZERO, ONE, '0', '1'};
    private final static String INDENT = "    ";
    private final static double DUMMY_VARIABLE_PROBABILITY = 0.3;
    private final static double DUMMY_STATEMENT_PROBABILITY = 0.4;
    private final BufferedWriter writer;
    private final List<Map<String, String>> scopes = new ArrayList<>();
    private final int baseName;
    private final Set<String> usedNames = new HashSet<>();
    private final Set<String> numericNames = new HashSet<>();
    private final Set<String> dummyVariables = new HashSet<>();
    private final Random random = new Random();
    private IOException ioException = null;
    private int indent = 0;
    private final static List<String> NUMERIC_TYPES = List.of(
            "int", "double", "float", "long", "short",
            "long long", "long int", "long double",
            "unsigned int", "unsigned char", "unsigned short", "unsigned long", "unsigned long long",
            "signed int", "signed char", "signed short", "signed long", "signed long long"
    );
    private final static String[] NUMERIC_OPERATORS = {"+", "-", "*"};
    private final static int MAX_RANDOM_EXPRESSION_LENGTH = 5;

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

    private void withIndent(final String string) {
        indent();
        write(string);
    }

    private void withIndent(final char c) {
        indent();
        write(c);
    }

    private void indent() {
        for (int i = 0; i < indent; ++i) {
            write(INDENT);
        }
    }

    private void increaseIndent() {
        ++indent;
    }

    private void decreaseIndent() {
        --indent;
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
        final Collection<String> scopeVariables = scopes.getLast().values();
        usedNames.removeAll(scopeVariables);
        numericNames.removeAll(scopeVariables);
        dummyVariables.removeAll(scopeVariables);
        scopes.removeLast();
    }

    private void writeDefine(final Object name, final Object value) {
        write("#define %s %s".formatted(name, value));
        newLine();
    }

    @Override
    public Void visitProgram(ProgramParser.ProgramContext ctx) {
        writeDefine(ZERO, 0);
        writeDefine(ONE, 1);
        final String zero = String.valueOf(ZERO);
        final String one = String.valueOf(ONE);
        numericNames.add(zero);
        numericNames.add(one);
        newLine();
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
        numericNames.remove(zero);
        numericNames.remove(one);
        return null;
    }

    private String getVariableType(final ProgramParser.SingleVariableDeclarationContext ctx) {
        final ParseTree parent = ctx.parent;
        if (parent instanceof ProgramParser.ArgumentDeclarationContext that) {
            return that.getText() + that.declarators();
        }
        final ProgramParser.VariableDeclarationContext that = ((ProgramParser.VariableDeclarationContext) ctx.parent);
        return that.getText() + ctx.declarators();
    }

    private void addToScope(final String name, final String newName) {
        scopes.getLast().put(name, newName);
    }

    @Override
    public Void visitSingleVariableDeclaration(ProgramParser.SingleVariableDeclarationContext ctx) {
        visit(ctx.declarators());
        final String name = newName();
        addToScope(ctx.IDENT().getText(), name);
        if (NUMERIC_TYPES.contains(getVariableType(ctx))) {
            numericNames.add(name);
        }
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
        visit(ctx.declarators());
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

    private void insertDummyVariable() {
        if (!toss(DUMMY_VARIABLE_PROBABILITY)) {
            return;
        }
        final String name = newName();
        dummyVariables.add(name);
        numericNames.add(name);
        addToScope(name, name);
        indent();
        write(NUMERIC_TYPES.get(random.nextInt(NUMERIC_TYPES.size())));
        write(" ");
        write(name);
        write(" = ");
        write(String.valueOf(random.nextInt(-128, 127)));
        write(';');
        newLine();
    }

    @Override
    public Void visitScope(ProgramParser.ScopeContext ctx) {
        write('{');
        increaseIndent();
        newLine();
        for (int i = 1; i < ctx.getChildCount() - 1; ++i) {
            insertDummyVariable();
            insertDummyStatement();
            indent();
            final ParseTree child = ctx.getChild(i);
            visit(child);
            newLine();
        }
        decreaseIndent();
        withIndent('}');
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
        write("return");
        if (ctx.getChildCount() > 1) {
            write(" ");
            visit(ctx.expression());
        }
        return null;
    }

    @Override
    public Void visitExpression(ProgramParser.ExpressionContext ctx) {
        if (ctx.prefixOperator() != null ||
                ctx.postfixOperator() != null ||
                ctx.primary() != null ||
                ctx.getChild(0).getText().equals("(")) {
            return super.visitExpression(ctx);
        }
        if (ctx.getChild(1).getText().equals("(")) {  // function call
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
        // infix operator
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

    private void insertDummyStatement() {
        if (!toss(DUMMY_STATEMENT_PROBABILITY)) {
            return;
        }
        final String dest = randomElement(dummyVariables);
        if (dest == null) {
            return;
        }
        indent();
        write(dest);
        write(" ");
        if (random.nextBoolean()) {
            write(randomNumericOperator());
        }
        write("= ");
        write(randomExpression());
        write(";");
        newLine();
    }

    private <T> T randomElement(final Set<T> set) {
        if (set.isEmpty()) {
            return null;
        }
        return set.stream().skip(random.nextInt(set.size())).findFirst().orElse(null);
    }

    private String randomNumericOperator() {
        return NUMERIC_OPERATORS[random.nextInt(NUMERIC_OPERATORS.length)];
    }

    private String randomExpression() {
        final int operands = random.nextInt(1, MAX_RANDOM_EXPRESSION_LENGTH);
        final String[] expression = new String[2 * operands - 1];
        for (int i = 0; i < expression.length; ++i) {
            if (i % 2 == 0) {
                expression[i] = randomElement(numericNames);
            } else {
                expression[i] = randomNumericOperator();
            }
        }
        return String.join(" ", expression);
    }
}
