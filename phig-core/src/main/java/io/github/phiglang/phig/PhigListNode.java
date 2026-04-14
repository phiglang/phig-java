package io.github.phiglang.phig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A phig list value (ordered sequence of values).
 */
public final class PhigListNode extends PhigNode {

    private final List<PhigNode> items;

    public PhigListNode(List<PhigNode> items) {
        this.items = List.copyOf(items);
    }

    @Override public boolean isList() { return true; }

    @Override
    public PhigNode get(int index) {
        return (index >= 0 && index < items.size()) ? items.get(index) : null;
    }

    @Override
    public PhigNode path(int index) {
        PhigNode n = get(index);
        return n != null ? n : MissingNode.INSTANCE;
    }

    @Override public int size() { return items.size(); }

    @Override
    public Iterator<PhigNode> elements() {
        return items.iterator();
    }

    @Override
    public Object toObject() {
        List<Object> result = new ArrayList<>(items.size());
        for (PhigNode item : items) {
            result.add(item.toObject());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof PhigListNode l && items.equals(l.items));
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
