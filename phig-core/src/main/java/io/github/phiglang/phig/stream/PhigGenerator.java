package io.github.phiglang.phig.stream;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Event-based writer that serializes phig structure to text.
 *
 * <p>Writes formatted phig output to a {@link Writer}. The first
 * {@code startMap} call is treated as the top-level document
 * (no surrounding braces).
 */
public final class PhigGenerator {

    private Writer out;
    private int indent;
    private final Deque<Context> stack = new ArrayDeque<>();
    private boolean rootStarted;

    public PhigGenerator(Writer out) {
        this.out = out;
    }

    private sealed interface Context {}

    private static final class MapContext implements Context {

        final boolean topLevel;
        boolean hasEntries;

        MapContext(boolean topLevel) {
            this.topLevel = topLevel;
        }
    }

    private static final class ListContext implements Context {

        final Writer savedOut;
        final int savedIndent;
        final List<String> items = new ArrayList<>();
        boolean hasCompound;
        StringWriter itemBuffer;

        ListContext(Writer savedOut, int savedIndent) {
            this.savedOut = savedOut;
            this.savedIndent = savedIndent;
        }
    }

    public void startMap() throws IOException {
        if (!rootStarted) {
            rootStarted = true;
            stack.push(new MapContext(true));
        } else {
            markCompoundIfInList();
            stack.push(new MapContext(false));
        }
    }

    public void endMap() throws IOException {
        MapContext ctx = (MapContext) stack.pop();
        if (!ctx.topLevel) {
            if (ctx.hasEntries) {
                indent--;
                writeIndent();
                out.write('}');
            } else {
                out.write("{}");
            }
            afterValue();
        }
    }

    public void key(String name) throws IOException {
        MapContext ctx = (MapContext) stack.peek();
        if (!ctx.topLevel && !ctx.hasEntries) {
            out.write("{\n");
            indent++;
            ctx.hasEntries = true;
        }
        writeIndent();
        writeString(name);
        out.write(' ');
    }

    public void startList() throws IOException {
        markCompoundIfInList();
        ListContext ctx = new ListContext(out, indent);
        stack.push(ctx);
        indent++;
        ctx.itemBuffer = new StringWriter();
        out = ctx.itemBuffer;
    }

    public void endList() throws IOException {
        ListContext ctx = (ListContext) stack.pop();
        out = ctx.savedOut;
        indent = ctx.savedIndent;

        if (ctx.items.isEmpty()) {
            out.write("[]");
        } else if (!ctx.hasCompound) {
            out.write('[');
            for (int i = 0; i < ctx.items.size(); i++) {
                if (i > 0) out.write(' ');
                out.write(ctx.items.get(i));
            }
            out.write(']');
        } else {
            int inner = indent + 1;
            out.write("[\n");
            for (String item : ctx.items) {
                writeIndent(inner);
                out.write(item);
                out.write('\n');
            }
            writeIndent();
            out.write(']');
        }
        afterValue();
    }

    public void string(String value) throws IOException {
        writeString(value);
        afterValue();
    }

    private void afterValue() throws IOException {
        if (stack.isEmpty()) return;
        Context top = stack.peek();
        if (top instanceof MapContext) {
            out.write('\n');
        } else if (top instanceof ListContext lc) {
            lc.items.add(lc.itemBuffer.toString());
            lc.itemBuffer = new StringWriter();
            out = lc.itemBuffer;
        }
    }

    private void markCompoundIfInList() {
        if (!stack.isEmpty() && stack.peek() instanceof ListContext lc) {
            lc.hasCompound = true;
        }
    }

    private void writeString(String s) throws IOException {
        if (canBeBare(s)) {
            out.write(s);
            return;
        }
        out.write('"');
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            switch (cp) {
                case '"' -> out.write("\\\"");
                case '\\' -> out.write("\\\\");
                case '\n' -> out.write("\\n");
                case '\r' -> out.write("\\r");
                case '\t' -> out.write("\\t");
                case 0 -> out.write("\\0");
                default -> {
                    if (Character.isISOControl(cp)) {
                        out.write("\\u{" + Integer.toHexString(cp) + "}");
                    } else {
                        out.write(Character.toChars(cp));
                    }
                }
            }
            i += Character.charCount(cp);
        }
        out.write('"');
    }

    private static boolean canBeBare(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (
                PhigParser.isUnicodeWhitespace(cp) ||
                cp == '{' ||
                cp == '}' ||
                cp == '[' ||
                cp == ']' ||
                cp == '"' ||
                cp == '#' ||
                cp == '\'' ||
                cp == ';'
            ) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    private void writeIndent() throws IOException {
        writeIndent(indent);
    }

    private void writeIndent(int level) throws IOException {
        for (int i = 0; i < level; i++) {
            out.write("  ");
        }
    }
}
