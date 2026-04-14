package io.github.phiglang.phig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhigReaderTest {

    @Test
    void readTree() throws Exception {
        PhigNode tree = PhigReader.parse("name foo\ntags [a b c]");
        assertTrue(tree.isMap());
        assertEquals("foo", tree.path("name").asString());
        assertEquals("a", tree.path("tags").path(0).asString());
        assertEquals(3, tree.path("tags").size());
    }

    @Test
    void pathChaining() throws Exception {
        PhigNode tree = PhigReader.parse("a { b { c hello } }");
        assertEquals("hello", tree.path("a").path("b").path("c").asString());
        assertTrue(tree.path("x").path("y").isMissing());
    }

    @Test
    void parseErrors() {
        String[] invalid = {
                "x \"unterminated",
                "x [unclosed",
                "x {unclosed",
                "a 1; a 2",
                "x \"\\q\"",
        };
        for (String input : invalid) {
            assertThrows(PhigException.class, () -> PhigReader.parse(input),
                    "expected error for: " + input);
        }
    }
}
