package io.github.phiglang.phig;

import java.util.Objects;

/**
 * A phig string value.
 */
public final class PhigStringNode extends PhigNode {

    private final String value;

    public PhigStringNode(String value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override public boolean isString() { return true; }
    @Override public String asString() { return value; }
    @Override public Object toObject() { return value; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof PhigStringNode s && value.equals(s.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
