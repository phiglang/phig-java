package io.github.phiglang.phig;

/**
 * Thrown when parsing or serializing phig data fails.
 */
public class PhigException extends Exception {

    private final int position;

    public PhigException(String message) {
        super(message);
        this.position = -1;
    }

    public PhigException(String message, int position) {
        super(position >= 0 ? "at position " + position + ": " + message : message);
        this.position = position;
    }

    /**
     * Returns the byte offset where the error occurred, or -1 if not applicable.
     */
    public int getPosition() {
        return position;
    }
}
