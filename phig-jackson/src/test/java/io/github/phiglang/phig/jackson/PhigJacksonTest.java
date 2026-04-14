package io.github.phiglang.phig.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhigJacksonTest {

    private final ObjectMapper mapper = new PhigObjectMapper();

    // -- basic reading --

    record ServerConfig(String host, int port) {}

    @Test
    void readSimpleRecord() throws Exception {
        ServerConfig cfg = mapper.readValue("host 0.0.0.0\nport 8080", ServerConfig.class);
        assertEquals("0.0.0.0", cfg.host());
        assertEquals(8080, cfg.port());
    }

    // -- nested structures --

    record Tls(String cert, String key) {}
    record Server(String host, int port, Tls tls) {}
    record Pool(int min, int max) {}
    record Db(String url, Pool pool) {}
    record AppConfig(String name, List<String> tags, Server server, Db db) {}

    @Test
    void readNestedConfig() throws Exception {
        String input = """
                name "My App"
                tags [web production v2]
                server {
                  host 0.0.0.0
                  port 8080
                  tls {
                    cert /etc/ssl/cert.pem
                    key /etc/ssl/key.pem
                  }
                }
                db {
                  url "postgres://localhost/primary"
                  pool { min 2; max 10 }
                }
                """;
        AppConfig cfg = mapper.readValue(input, AppConfig.class);
        assertEquals("My App", cfg.name());
        assertEquals(List.of("web", "production", "v2"), cfg.tags());
        assertEquals("0.0.0.0", cfg.server().host());
        assertEquals(8080, cfg.server().port());
        assertEquals("/etc/ssl/cert.pem", cfg.server().tls().cert());
        assertEquals(10, cfg.db().pool().max());
    }

    // -- Jackson tree model --

    @Test
    void readTree() throws Exception {
        JsonNode node = mapper.readTree("name foo\ntags [a b c]");
        assertEquals("foo", node.get("name").asText());
        assertTrue(node.get("tags").isArray());
        assertEquals("a", node.get("tags").get(0).asText());
        assertEquals(3, node.get("tags").size());
    }

    // -- Jackson annotations --

    record AnnotatedConfig(
            @JsonProperty("app_name") String appName,
            @JsonIgnore String secret,
            int port
    ) {}

    @Test
    void jacksonAnnotationsRead() throws Exception {
        AnnotatedConfig cfg = mapper.readValue(
                "app_name myapp\nport 3000", AnnotatedConfig.class);
        assertEquals("myapp", cfg.appName());
        assertNull(cfg.secret());
        assertEquals(3000, cfg.port());
    }

    @Test
    void jacksonAnnotationsWrite() throws Exception {
        AnnotatedConfig cfg = new AnnotatedConfig("myapp", "supersecret", 3000);
        String output = mapper.writeValueAsString(cfg);
        assertTrue(output.contains("app_name myapp"));
        assertFalse(output.contains("secret"));
        assertFalse(output.contains("supersecret"));
        assertTrue(output.contains("port 3000"));
    }

    // -- roundtrip --

    @Test
    void roundtrip() throws Exception {
        AppConfig original = new AppConfig(
                "My App",
                List.of("web", "prod"),
                new Server("0.0.0.0", 8080, new Tls("/cert.pem", "/key.pem")),
                new Db("postgres://localhost/db", new Pool(2, 10))
        );
        String phig = mapper.writeValueAsString(original);
        AppConfig restored = mapper.readValue(phig, AppConfig.class);
        assertEquals(original, restored);
    }

    // -- type coercion --

    record TypesConfig(int count, long big, double rate, boolean debug, String name) {}

    @Test
    void typeCoercion() throws Exception {
        TypesConfig cfg = mapper.readValue(
                "count 42\nbig 9999999999\nrate 3.14\ndebug true\nname hello",
                TypesConfig.class);
        assertEquals(42, cfg.count());
        assertEquals(9999999999L, cfg.big());
        assertEquals(3.14, cfg.rate(), 0.001);
        assertTrue(cfg.debug());
        assertEquals("hello", cfg.name());
    }

    // -- Map --

    @Test
    @SuppressWarnings("unchecked")
    void readAsMap() throws Exception {
        Map<String, Object> map = mapper.readValue("name foo\ntags [a b]", Map.class);
        assertEquals("foo", map.get("name"));
        List<Object> tags = (List<Object>) map.get("tags");
        assertEquals(List.of("a", "b"), tags);
    }

    // -- writing --

    @Test
    void writeQuotedStrings() throws Exception {
        record Msg(String msg) {}
        String output = mapper.writeValueAsString(new Msg("hello world"));
        assertTrue(output.contains("\"hello world\""));
    }

    @Test
    void writeEscapeSequences() throws Exception {
        record Msg(String msg) {}
        String output = mapper.writeValueAsString(new Msg("line1\nline2"));
        assertTrue(output.contains("\"line1\\nline2\""));
    }

    @Test
    void writeBooleans() throws Exception {
        record Flags(boolean debug, boolean verbose) {}
        String output = mapper.writeValueAsString(new Flags(true, false));
        assertTrue(output.contains("debug true"));
        assertTrue(output.contains("verbose false"));
    }

    @Test
    void writeList() throws Exception {
        record Tags(List<String> tags) {}
        String output = mapper.writeValueAsString(new Tags(List.of("a", "b", "c")));
        assertTrue(output.contains("[a b c]"));
    }

    @Test
    void writeNestedMap() throws Exception {
        record Inner(String x) {}
        record Outer(Inner inner) {}
        String output = mapper.writeValueAsString(new Outer(new Inner("hello")));
        assertTrue(output.contains("inner"));
        assertTrue(output.contains("x hello"));
    }

    // -- Map writing --

    @Test
    void writeMap() throws Exception {
        Map<String, String> m = Map.of("a", "1", "b", "2");
        String output = mapper.writeValueAsString(m);
        assertTrue(output.contains("a 1"));
        assertTrue(output.contains("b 2"));
    }
}
