package io.github.phiglang.phig;

import io.github.phiglang.phig.stream.PhigParser;
import io.github.phiglang.phig.stream.PhigParser.Event;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Reads phig text into a {@link PhigMapNode} tree.
 *
 * <p>Delegates to {@link PhigParser} and reconstructs the tree
 * from the event stream.
 */
public final class PhigReader {

    private PhigReader() {}

    public static PhigMapNode parse(String input) throws PhigException {
        PhigParser parser = new PhigParser(input);
        Deque<Object> stack = new ArrayDeque<>();
        Deque<String> keyStack = new ArrayDeque<>();
        String[] currentKey = { null };
        PhigMapNode[] result = { null };

        Event event;
        while ((event = parser.next()) != null) {
            switch (event) {
                case START_MAP -> {
                    keyStack.push(currentKey[0] != null ? currentKey[0] : "");
                    stack.push(new LinkedHashMap<String, PhigNode>());
                }
                case END_MAP -> {
                    @SuppressWarnings("unchecked")
                    var map = (LinkedHashMap<String, PhigNode>) stack.pop();
                    PhigMapNode node = new PhigMapNode(map);
                    currentKey[0] = keyStack.pop();
                    if (stack.isEmpty()) {
                        result[0] = node;
                    } else {
                        addNode(stack, currentKey[0], node);
                    }
                }
                case KEY -> currentKey[0] = parser.getString();
                case START_LIST -> {
                    keyStack.push(currentKey[0] != null ? currentKey[0] : "");
                    stack.push(new ArrayList<PhigNode>());
                }
                case END_LIST -> {
                    @SuppressWarnings("unchecked")
                    var list = (List<PhigNode>) stack.pop();
                    PhigListNode node = new PhigListNode(list);
                    currentKey[0] = keyStack.pop();
                    addNode(stack, currentKey[0], node);
                }
                case STRING -> addNode(
                    stack,
                    currentKey[0],
                    new PhigStringNode(parser.getString())
                );
            }
        }
        return result[0];
    }

    public static PhigMapNode parse(Reader reader) throws PhigException, IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return parse(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private static void addNode(Deque<Object> stack, String key, PhigNode node) {
        Object top = stack.peek();
        if (top instanceof LinkedHashMap) {
            ((LinkedHashMap<String, PhigNode>) top).put(key, node);
        } else {
            ((List<PhigNode>) top).add(node);
        }
    }
}
