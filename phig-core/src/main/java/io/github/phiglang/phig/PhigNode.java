package io.github.phiglang.phig;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Base class for all phig value nodes.
 *
 * <p>Phig has three value types: strings, lists, and maps. This class provides
 * accessor methods for all types; subclasses override the relevant ones.
 *
 * <p>Use {@link #get(String)} or {@link #get(int)} for nullable lookups, and
 * {@link #path(String)} or {@link #path(int)} for safe chaining (returns
 * {@link MissingNode} instead of null).
 */
public abstract class PhigNode implements Iterable<PhigNode> {

    public boolean isString() { return false; }
    public boolean isList() { return false; }
    public boolean isMap() { return false; }
    public boolean isMissing() { return false; }

    /**
     * Returns the string content of this node, or an empty string if not a string node.
     */
    public String asString() { return ""; }

    public int asInt() { return Integer.parseInt(asString()); }
    public long asLong() { return Long.parseLong(asString()); }
    public double asDouble() { return Double.parseDouble(asString()); }

    public boolean asBoolean() {
        String s = asString();
        if ("true".equals(s)) return true;
        if ("false".equals(s)) return false;
        throw new IllegalStateException("not a boolean value: " + s);
    }

    /** Looks up a key in a map node. Returns null if not a map or key is absent. */
    public PhigNode get(String key) { return null; }

    /** Looks up an index in a list node. Returns null if not a list or out of bounds. */
    public PhigNode get(int index) { return null; }

    /** Like {@link #get(String)} but returns {@link MissingNode} instead of null. */
    public PhigNode path(String key) { return MissingNode.INSTANCE; }

    /** Like {@link #get(int)} but returns {@link MissingNode} instead of null. */
    public PhigNode path(int index) { return MissingNode.INSTANCE; }

    public boolean has(String key) { return get(key) != null; }
    public boolean has(int index) { return get(index) != null; }

    /** Returns the number of entries (map) or items (list), or 0. */
    public int size() { return 0; }

    /** Iterates list items. Empty for non-list nodes. */
    public Iterator<PhigNode> elements() { return Collections.emptyIterator(); }

    /** Iterates map entries. Empty for non-map nodes. */
    public Iterator<Map.Entry<String, PhigNode>> fields() { return Collections.emptyIterator(); }

    @Override
    public Iterator<PhigNode> iterator() {
        return elements();
    }

    /**
     * Converts this node to plain Java types:
     * string &rarr; {@link String}, list &rarr; {@link java.util.List},
     * map &rarr; {@link java.util.LinkedHashMap}.
     */
    public abstract Object toObject();
}
