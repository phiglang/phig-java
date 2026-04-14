package io.github.phiglang.phig.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convenience {@link ObjectMapper} pre-configured for phig.
 *
 * <pre>{@code
 * PhigObjectMapper mapper = new PhigObjectMapper();
 * Config cfg = mapper.readValue(phigString, Config.class);
 * String phig = mapper.writeValueAsString(cfg);
 * }</pre>
 *
 * <p>All Jackson annotations ({@code @JsonProperty}, {@code @JsonIgnore},
 * {@code @JsonCreator}, etc.) work as expected.
 */
public class PhigObjectMapper extends ObjectMapper {

    public PhigObjectMapper() {
        super(new PhigFactory());
    }
}
