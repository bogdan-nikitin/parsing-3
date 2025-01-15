package feo;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ObfuscateVisitor extends ProgramBaseVisitor<ObfuscateVisitor.Context> {
    private static final int NAME_WIDTH = 8;
    private static final char ONE = 'I';
    private static final char ZERO = 'O';
    private static final char[] POSSIBLE_CHARS = {ZERO, ONE, '0', '1'};
    private final static String INDENT = "    ";
    private final static double DUMMY_VARIABLE_PROBABILITY = 0.3;
    private final static double DUMMY_STATEMENT_PROBABILITY = 0.4;
    private final static double MODIFY_EXPRESSION_PROBABILITY = 1;
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
    private final static String[] DUMMY_NUMERIC_OPERATORS = {"+", "-", "*"};
    private final static Set<String> NUMERIC_OPERATORS = Set.of("+", "-", "*", "/");
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
    public Context visitProgram(ProgramParser.ProgramContext ctx) {
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
        return Context.DEFAULT;
    }

    private String getVariableType(final ProgramParser.SingleVariableDeclarationContext ctx) {
        final ParseTree parent = ctx.parent;
        if (parent instanceof ProgramParser.ArgumentDeclarationContext that) {
            return that.type().getText() + that.declarators().getText();
        }
        final ProgramParser.VariableDeclarationContext that = ((ProgramParser.VariableDeclarationContext) ctx.parent);
        return that.type().getText() + ctx.declarators().getText();
    }

    private void addToScope(final String name, final String newName) {
        scopes.getLast().put(name, newName);
    }

    @Override
    public Context visitSingleVariableDeclaration(ProgramParser.SingleVariableDeclarationContext ctx) {
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
        return Context.DEFAULT;
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
    public Context visitVariableDeclaration(ProgramParser.VariableDeclarationContext ctx) {
        visit(ctx.type());
        write(' ');
        final int childCount = ctx.getChildCount();
        for (int i = 1; i < childCount; ++i) {
            visit(ctx.getChild(i));
            if (ctx.getChild(i).getText().equals(",")) {
                write(" ");
            }
        }
        return Context.DEFAULT;
    }

    @Override
    public Context visitTerminal(TerminalNode node) {
        write(node.getText());
        return Context.DEFAULT;
    }

    @Override
    public Context visitFunctionDeclaration(ProgramParser.FunctionDeclarationContext ctx) {
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
        return Context.DEFAULT;
    }

    @Override
    public Context visitArguments(ProgramParser.ArgumentsContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree child = ctx.getChild(i);
            visit(child);
            if (child.getText().equals(",")) {
                write(' ');
            }
        }
        return Context.DEFAULT;
    }

    @Override
    public Context visitArgumentDeclaration(ProgramParser.ArgumentDeclarationContext ctx) {
        visit(ctx.type());
        write(' ');
        visit(ctx.singleVariableDeclaration());
        return Context.DEFAULT;
    }

    @Override
    public Context visitName(ProgramParser.NameContext ctx) {
        final String name = lookup(ctx.getText());
        write(name);
        return new Context(numericNames.contains(name));
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
    public Context visitScope(ProgramParser.ScopeContext ctx) {
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
        return Context.DEFAULT;
    }

    @Override
    public Context visitIf(ProgramParser.IfContext ctx) {
        write("if (");
        visit(ctx.expression());
        write(") ");
        visit(ctx.statement(0));
        if (ctx.statement(1) != null) {
            write(" else ");
            visit(ctx.statement(1));
        }
        return Context.DEFAULT;
    }

    @Override
    public Context visitWhile(ProgramParser.WhileContext ctx) {
        write("while (");
        visit(ctx.expression());
        write(") ");
        visit(ctx.statement());
        return Context.DEFAULT;
    }

    @Override
    public Context visitFor(ProgramParser.ForContext ctx) {
        write("for (");
        visit(ctx.simpleStatement(0));
        write("; ");
        visit(ctx.simpleStatement(1));
        write("; ");
        visit(ctx.simpleStatement(2));
        write(") ");
        visit(ctx.statement());
        return Context.DEFAULT;
    }

    @Override
    public Context visitReturn(ProgramParser.ReturnContext ctx) {
        write("return");
        if (ctx.getChildCount() > 1) {
            write(" ");
            visit(ctx.expression());
        }
        return Context.DEFAULT;
    }

    @Override
    public Context visitLiteral(final ProgramParser.LiteralContext ctx) {
        final boolean isNumeric = (ctx.INT() != null);
        if (toss(MODIFY_EXPRESSION_PROBABILITY) && isNumeric) {
            write('(');
            super.visitLiteral(ctx);
            modifyNumericExpression();
            write(')');
        } else {
            super.visitLiteral(ctx);
        }
        return new Context(isNumeric);
    }

    @Override
    public Context visitExpression(ProgramParser.ExpressionContext ctx) {
        if (ctx.prefixOperator() != null) {
            visit(ctx.getChild(0));
            return visit(ctx.getChild(1));
        }
        if (ctx.primary() != null) {
            return visitPrimary(ctx.primary());
        }
        if (ctx.getChild(0).getText().equals("(")) {
            visit(ctx.getChild(0));
            final Context context = visit(ctx.getChild(1));
            visit(ctx.getChild(2));
            return context;
        }
        boolean modifyExpression = toss(MODIFY_EXPRESSION_PROBABILITY);
        if (ctx.getChild(1) != null && ctx.getChild(1).getText().equals("(")) {  // function call
            visit(ctx.getChild(0));
            write('(');
            for (int i = 2; !ctx.getChild(i).getText().equals(")"); ++i) {
                visit(ctx.getChild(i));
                if (ctx.getChild(i).getText().equals(",")) {
                    write(' ');
                }
            }
            write(")");
            return Context.DEFAULT;
        }
        // infix operator
        if (modifyExpression) {
            write("((");
        }
        boolean isNumeric;
        if (ctx.postfixOperator() != null) {
            isNumeric = visit(ctx.getChild(0)).isNumericExpression();
            visit(ctx.getChild(1));
        } else {
            isNumeric = visit(ctx.getChild(0)).isNumericExpression();
            write(' ');
            final ParseTree operator = ctx.getChild(1);
            isNumeric = isNumeric && NUMERIC_OPERATORS.contains(operator.getText());
            visit(operator);
            write(' ');
            isNumeric = visit(ctx.getChild(2)).isNumericExpression() && isNumeric;
        }
        if (modifyExpression) {
            write(')');
            if (isNumeric) {
                modifyNumericExpression();
            }
            write(')');
        }
        return new Context(isNumeric);
    }

    private void modifyNumericExpression() {
        write(' ');
        if (random.nextBoolean()) {
            write(random.nextBoolean() ? '*' : '/');
            write(' ');
            write(ONE);
        } else {
            write(random.nextBoolean() ? '+' : '-');
            write(' ');
            write(ZERO);
        }
    }

    @Override
    public Context visitAssignment(ProgramParser.AssignmentContext ctx) {
        visit(ctx.getChild(0));
        write(' ');
        visit(ctx.getChild(1));
        write(' ');
        visit(ctx.getChild(2));
        return Context.DEFAULT;
    }

    @Override
    public Context visitType(ProgramParser.TypeContext ctx) {
        visit(ctx.getChild(0));
        for (int i = 1; i < ctx.getChildCount(); ++i) {
            write(' ');
            visit(ctx.getChild(i));
        }
        return Context.DEFAULT;
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
        return DUMMY_NUMERIC_OPERATORS[random.nextInt(DUMMY_NUMERIC_OPERATORS.length)];
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

    public record Context(boolean isNumericExpression) {
        public Context() {
            this(false);
        }

        public final static Context DEFAULT = new Context();
    }
}
