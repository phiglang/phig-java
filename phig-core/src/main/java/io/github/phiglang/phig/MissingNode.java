package io.github.phiglang.phig;

/**
 * Singleton returned by {@link PhigNode#path} when a key or index is absent.
 * Allows safe chaining without null checks.
 */
public final class MissingNode extends PhigNode {

    static final MissingNode INSTANCE = new MissingNode();

    private MissingNode() {}

    @Override public boolean isMissing() { return true; }
    @Override public PhigNode path(String key) { return this; }
    @Override public PhigNode path(int index) { return this; }
    @Override public Object toObject() { return null; }

    @Override
    public String toString() {
        return "<missing>";
    }
}
