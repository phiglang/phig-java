package io.github.phiglang.phig.stream;

import io.github.phiglang.phig.PhigException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Pull-based parser for the phig configuration language.
 *
 * <p>Each call to {@link #next()} returns the next parse {@link Event},
 * or {@code null} when the input is fully consumed.
 *
 * <p>Parser states correspond to positions in the grammar:
 * <pre>
 *   toplevel = [BOM] _ [pairs] _ EOF
 *   value    = map | list | string
 *   map      = '{' _ [pairs] _ '}'
 *   pairs    = pair { PAIRSEP _ pair }
 *   pair     = string [HSPACE] value [HSPACE] [COMMENT]
 *   list     = '[' _ [items] _ ']'
 *   items    = value { _ [';'] _ value }
 * </pre>
 */
public final class PhigParser {

    public enum Event {
        START_MAP, END_MAP, KEY, START_LIST, END_LIST, STRING
    }

    private final String src;
    private int pos;
    private String currentString;

    private enum State {
        /** toplevel: before BOM / first pair. */
        DOC_START,
        /** pairs: expecting a key or end of container. */
        BEFORE_PAIR,
        /** pair: key parsed, expecting value. */
        AFTER_KEY,
        /** pairs: pair done, expecting PAIRSEP or end of container. */
        BETWEEN_PAIRS,
        /** items: expecting a value or ']'. */
        BEFORE_ITEM,
        /** items: item done, expecting separator or ']'. */
        BETWEEN_ITEMS,
        /** Document fully consumed. */
        DONE
    }

    private State state = State.DOC_START;

    private static final class Frame {

        final boolean isList;
        final int closing;
        final int openPos;
        final Set<String> seen;

        Frame(boolean isList, int closing, int openPos) {
            this.isList = isList;
            this.closing = closing;
            this.openPos = openPos;
            this.seen = isList ? null : new HashSet<>();
        }
    }

    private final Deque<Frame> stack = new ArrayDeque<>();

    public PhigParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    /**
     * Returns the string associated with the most recent {@link Event#KEY}
     * or {@link Event#STRING} event.
     */
    public String getString() {
        return currentString;
    }

    /**
     * Returns the next parse event, or {@code null} when the input is
     * fully consumed.
     */
    public Event next() throws PhigException {
        while (true) {
            switch (state) {

                // toplevel = [BOM] _ [pairs] _ EOF
                case DOC_START -> {
                    if (pos < src.length() && src.charAt(pos) == '\uFEFF') {
                        pos++;
                    }
                    wsc();
                    stack.push(new Frame(false, -1, -1));
                    state = State.BEFORE_PAIR;
                    return Event.START_MAP;
                }

                // pairs = pair { PAIRSEP _ pair }
                //         ^--- expecting a key (start of pair) or end of map
                case BEFORE_PAIR -> {
                    if (atMapEnd()) {
                        return closeMap();
                    }
                    return parseKey();
                }

                // pair = string [HSPACE] value [HSPACE] [COMMENT]
                //                        ^--- key parsed, expecting value
                case AFTER_KEY -> {
                    return parseValue(State.BETWEEN_PAIRS);
                }

                // pairs = pair { PAIRSEP _ pair }
                //              ^--- pair done, expecting PAIRSEP or end of map
                case BETWEEN_PAIRS -> {
                    hspace();    // [HSPACE] trailing the pair
                    comment();   // [COMMENT] trailing the pair
                    if (atMapEnd()) {
                        state = State.BEFORE_PAIR;
                        continue;
                    }
                    if (!pairsep()) {
                        throw errAt(
                            "expected newline or ';' after value",
                            pos
                        );
                    }
                    wsc();       // _ before next pair
                    state = State.BEFORE_PAIR;
                }

                // items = value { _ [';'] _ value }
                //         ^--- expecting a value or ']'
                case BEFORE_ITEM -> {
                    if (atListEnd()) {
                        return closeList();
                    }
                    return parseValue(State.BETWEEN_ITEMS);
                }

                // items = value { _ [';'] _ value }
                //              ^--- item done, expecting separator or ']'
                case BETWEEN_ITEMS -> {
                    if (atEnd() || peekIs(']')) {
                        state = State.BEFORE_ITEM;
                        continue;
                    }
                    wsc();
                    if (peekIs(';')) advance();
                    wsc();
                    state = State.BEFORE_ITEM;
                }

                case DONE -> {
                    return null;
                }
            }
        }
    }

    // ── grammar-level helpers ──────────────────────────────────────

    /**
     * Parses a key at the start of a pair, checks for duplicates,
     * consumes trailing HSPACE, and returns {@link Event#KEY}.
     *
     * <p>pair = string [HSPACE] value ...
     *           ^--- here
     */
    private Event parseKey() throws PhigException {
        Frame frame = stack.peek();
        int start = pos;
        String key = stringVal();
        if (key == null) {
            int cp = peek();
            throw errAt(
                "unexpected '" + codePointToString(cp) + "'",
                pos
            );
        }
        if (frame.seen.contains(key)) {
            throw errAt("duplicate key '" + key + "'", start);
        }
        frame.seen.add(key);
        hspace();    // [HSPACE] between key and value
        currentString = key;
        state = State.AFTER_KEY;
        return Event.KEY;
    }

    /**
     * Parses a value (map, list, or string) and returns the
     * corresponding event.
     *
     * <p>value = map | list | string
     *
     * @param afterString state to transition to when the value is a
     *                    plain string (for maps/lists the state is set
     *                    to the container-interior state instead)
     */
    private Event parseValue(State afterString) throws PhigException {
        int cp = peek();
        if (cp == '{') {
            int open = pos;
            advance();
            wsc();
            stack.push(new Frame(false, '}', open));
            state = State.BEFORE_PAIR;
            return Event.START_MAP;
        }
        if (cp == '[') {
            int open = pos;
            advance();
            wsc();
            stack.push(new Frame(true, ']', open));
            state = State.BEFORE_ITEM;
            return Event.START_LIST;
        }
        String s = stringVal();
        if (s == null) {
            String msg = (afterString == State.BETWEEN_PAIRS)
                ? "expected value after key"
                : "unexpected '" + codePointToString(cp) + "'";
            throw errAt(msg, pos);
        }
        currentString = s;
        state = afterString;
        return Event.STRING;
    }

    /** True when the current position is at the end of the innermost map. */
    private boolean atMapEnd() {
        Frame frame = stack.peek();
        if (frame.closing == -1) {
            return atEnd();   // top-level: end at EOF
        }
        return atEnd() || peekIs(frame.closing);
    }

    /** True when the current position is at the end of the innermost list. */
    private boolean atListEnd() {
        return atEnd() || peekIs(']');
    }

    /**
     * Closes the current map: consumes '}' (or verifies EOF for
     * top-level), pops the frame, and returns {@link Event#END_MAP}.
     */
    private Event closeMap() throws PhigException {
        Frame frame = stack.peek();
        if (frame.closing != -1) {
            if (!peekIs(frame.closing)) {
                throw errAt("unclosed '{'", frame.openPos);
            }
            advance(); // skip }
        } else {
            wsc();
            if (!atEnd()) {
                int cp = src.codePointAt(pos);
                throw errAt(
                    "unexpected '" + codePointToString(cp) + "'",
                    pos
                );
            }
        }
        stack.pop();
        afterContainer();
        return Event.END_MAP;
    }

    /**
     * Closes the current list: consumes ']', pops the frame,
     * and returns {@link Event#END_LIST}.
     */
    private Event closeList() throws PhigException {
        Frame frame = stack.peek();
        if (!peekIs(']')) {
            throw errAt("unclosed '['", frame.openPos);
        }
        advance(); // skip ]
        stack.pop();
        afterContainer();
        return Event.END_LIST;
    }

    /** Sets the state appropriate for the parent context after closing a container. */
    private void afterContainer() {
        if (stack.isEmpty()) {
            state = State.DONE;
        } else {
            Frame parent = stack.peek();
            state = parent.isList
                ? State.BETWEEN_ITEMS
                : State.BETWEEN_PAIRS;
        }
    }

    // ── low-level scanning ──────────────────────────────────────────

    private boolean atEnd() {
        return pos >= src.length();
    }

    private int peek() {
        if (pos >= src.length()) return -1;
        return src.codePointAt(pos);
    }

    private boolean peekIs(int ch) {
        return peek() == ch;
    }

    private int advance() {
        if (pos >= src.length()) return -1;
        int cp = src.codePointAt(pos);
        pos += Character.charCount(cp);
        return cp;
    }

    /** Consumes horizontal whitespace (space and tab only). */
    private boolean hspace() {
        int start = pos;
        while (true) {
            int cp = peek();
            if (cp != ' ' && cp != '\t') break;
            advance();
        }
        return pos > start;
    }

    /** Consumes a pair separator: one or more newlines, or a semicolon. */
    private boolean pairsep() {
        if (peekIs(';')) {
            advance();
            return true;
        }
        int start = pos;
        while (true) {
            int cp = peek();
            if (cp == '\n') {
                advance();
            } else if (cp == '\r') {
                advance();
                if (peekIs('\n')) advance();
            } else {
                break;
            }
        }
        return pos > start;
    }

    /** Consumes a # comment to end of line. */
    private boolean comment() {
        if (!peekIs('#')) return false;
        while (true) {
            int cp = peek();
            if (cp == -1 || cp == '\n') break;
            advance();
        }
        return true;
    }

    /** Consumes structural whitespace (space, tab, CR, LF) and comments. */
    private void wsc() {
        while (true) {
            int cp = peek();
            if (cp == '#') {
                comment();
            } else if (
                cp == ' ' || cp == '\t' || cp == '\n' || cp == '\r'
            ) {
                advance();
            } else {
                break;
            }
        }
    }

    /** Parses a double-quoted string with escape sequences. */
    private String qstring() throws PhigException {
        int open = pos;
        advance(); // skip "
        StringBuilder sb = new StringBuilder();
        while (true) {
            int cp = peek();
            if (cp == -1) throw errAt("unterminated string", open);
            if (cp == '"') {
                advance();
                return sb.toString();
            }
            if (cp == '\\') {
                int escStart = pos;
                advance(); // skip backslash
                int esc = peek();
                if (esc == -1) throw errAt("unterminated escape", escStart);
                switch (esc) {
                    case 'n' -> {
                        sb.append('\n');
                        advance();
                    }
                    case 'r' -> {
                        sb.append('\r');
                        advance();
                    }
                    case 't' -> {
                        sb.append('\t');
                        advance();
                    }
                    case '\\' -> {
                        sb.append('\\');
                        advance();
                    }
                    case '"' -> {
                        sb.append('"');
                        advance();
                    }
                    case '0' -> {
                        sb.append('\0');
                        advance();
                    }
                    case '\r' -> {
                        advance();
                        if (peekIs('\n')) {
                            advance();
                        } else {
                            throw errAt(
                                "expected LF after CR in line continuation",
                                escStart
                            );
                        }
                    }
                    case '\n' -> advance(); // line continuation
                    case 'u' -> {
                        advance();
                        if (!peekIs('{')) throw errAt(
                            "expected '{' after \\u",
                            escStart
                        );
                        advance(); // skip {
                        StringBuilder hex = new StringBuilder();
                        while (true) {
                            int h = peek();
                            if (h == -1) throw errAt(
                                "invalid unicode escape",
                                escStart
                            );
                            if (h == '}') break;
                            if (!isHexDigit(h)) throw errAt(
                                "invalid unicode escape",
                                escStart
                            );
                            hex.appendCodePoint(h);
                            advance();
                        }
                        String hexStr = hex.toString();
                        if (
                            hexStr.isEmpty() || hexStr.length() > 6
                        ) throw errAt(
                            "invalid unicode escape",
                            escStart
                        );
                        advance(); // skip }
                        int codepoint;
                        try {
                            codepoint = Integer.parseInt(hexStr, 16);
                        } catch (NumberFormatException e) {
                            throw errAt(
                                "invalid unicode escape",
                                escStart
                            );
                        }
                        if (
                            !Character.isValidCodePoint(codepoint) ||
                            (codepoint >= 0xD800 && codepoint <= 0xDFFF)
                        ) throw errAt(
                            "unicode codepoint out of range",
                            escStart
                        );
                        sb.appendCodePoint(codepoint);
                    }
                    default -> {
                        advance();
                        throw errAt(
                            "invalid escape '\\" +
                            codePointToString(esc) +
                            "'",
                            escStart
                        );
                    }
                }
                continue;
            }
            advance();
            sb.appendCodePoint(cp);
        }
    }

    /** Parses a single-quoted raw string (no escapes). */
    private String qrstring() throws PhigException {
        int open = pos;
        advance(); // skip '
        int start = pos;
        while (pos < src.length()) {
            int cp = src.codePointAt(pos);
            if (cp == '\'') {
                String content = src.substring(start, pos);
                pos += Character.charCount(cp);
                return content;
            }
            pos += Character.charCount(cp);
        }
        throw errAt("unterminated raw string", open);
    }

    /** Parses a bare (unquoted) string. Returns null if no characters consumed. */
    private String bare() {
        int start = pos;
        while (pos < src.length()) {
            int cp = src.codePointAt(pos);
            if (
                isUnicodeWhitespace(cp) ||
                cp == '{' ||
                cp == '}' ||
                cp == '[' ||
                cp == ']' ||
                cp == '"' ||
                cp == '#' ||
                cp == '\'' ||
                cp == ';'
            ) {
                break;
            }
            pos += Character.charCount(cp);
        }
        if (pos == start) return null;
        return src.substring(start, pos);
    }

    /** Parses a string value (quoted, raw, or bare). Returns null if none found. */
    private String stringVal() throws PhigException {
        int cp = peek();
        if (cp == -1) return null;
        if (cp == '"') return qstring();
        if (cp == '\'') return qrstring();
        return bare();
    }

    // ── utility ─────────────────────────────────────────────────────

    public static boolean isUnicodeWhitespace(int cp) {
        if (cp <= 0x0020) {
            return (
                cp == 0x0009 ||
                cp == 0x000A ||
                cp == 0x000B ||
                cp == 0x000C ||
                cp == 0x000D ||
                cp == 0x0020
            );
        }
        if (cp == 0x0085 || cp == 0x00A0 || cp == 0x1680) return true;
        if (cp >= 0x2000 && cp <= 0x200A) return true;
        return (
            cp == 0x2028 ||
            cp == 0x2029 ||
            cp == 0x202F ||
            cp == 0x205F ||
            cp == 0x3000
        );
    }

    private static boolean isHexDigit(int cp) {
        return (
            (cp >= '0' && cp <= '9') ||
            (cp >= 'a' && cp <= 'f') ||
            (cp >= 'A' && cp <= 'F')
        );
    }

    private static String codePointToString(int cp) {
        return new String(Character.toChars(cp));
    }

    private PhigException errAt(String msg, int position) {
        return new PhigException(msg, position);
    }
}
