package io.github.phiglang.phig.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Jackson {@link JsonFactory} for the phig configuration language.
 *
 * <p>Use with {@link PhigObjectMapper} or pass to any Jackson
 * {@code ObjectMapper} constructor:
 *
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper(new PhigFactory());
 * Config cfg = mapper.readValue(phigInput, Config.class);
 * }</pre>
 */
public class PhigFactory extends JsonFactory {

    private static final long serialVersionUID = 1L;

    public PhigFactory() {
        super();
    }

    @Override
    public String getFormatName() {
        return "PHIG";
    }

    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt)
        throws IOException {
        return _doParse(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt)
        throws IOException {
        return _doParse(r);
    }

    @Override
    protected JsonParser _createParser(
        char[] data,
        int offset,
        int len,
        IOContext ctxt,
        boolean recyclable
    ) throws IOException {
        return _doParse(new String(data, offset, len));
    }

    @Override
    protected JsonParser _createParser(
        byte[] data,
        int offset,
        int len,
        IOContext ctxt
    ) throws IOException {
        return _doParse(new String(data, offset, len, StandardCharsets.UTF_8));
    }

    private JsonParser _doParse(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return _doParse(sb.toString());
    }

    private JsonParser _doParse(String input) {
        PhigJacksonParser parser = new PhigJacksonParser(input);
        parser.setCodec(getCodec());
        return parser;
    }

    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt)
        throws IOException {
        return new PhigJacksonGenerator(out);
    }

    @Override
    protected JsonGenerator _createUTF8Generator(
        OutputStream out,
        IOContext ctxt
    ) throws IOException {
        return new PhigJacksonGenerator(
            new OutputStreamWriter(out, StandardCharsets.UTF_8)
        );
    }
}
