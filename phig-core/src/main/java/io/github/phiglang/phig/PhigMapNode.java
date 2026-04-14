package io.github.phiglang.phig;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A phig map value (ordered key-value pairs with unique string keys).
 */
public final class PhigMapNode extends PhigNode {

    private final LinkedHashMap<String, PhigNode> entries;

    public PhigMapNode(LinkedHashMap<String, PhigNode> entries) {
        this.entries = new LinkedHashMap<>(entries);
    }

    @Override public boolean isMap() { return true; }

    @Override
    public PhigNode get(String key) {
        return entries.get(key);
    }

    @Override
    public PhigNode path(String key) {
        PhigNode n = entries.get(key);
        return n != null ? n : MissingNode.INSTANCE;
    }

    @Override public int size() { return entries.size(); }

    @Override
    public Iterator<Map.Entry<String, PhigNode>> fields() {
        return Collections.unmodifiableMap(entries).entrySet().iterator();
    }

    public Set<String> fieldNames() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    @Override
    public Object toObject() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(entries.size());
        entries.forEach((k, v) -> result.put(k, v.toObject()));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof PhigMapNode m && entries.equals(m.entries));
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return entries.toString();
    }
}
