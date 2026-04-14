package io.github.phiglang.phig.jackson;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.json.JsonReadContext;
import io.github.phiglang.phig.PhigException;
import io.github.phiglang.phig.stream.PhigParser;
import io.github.phiglang.phig.stream.PhigParser.Event;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Jackson {@link JsonParser} backed by a phig pull parser.
 *
 * <p>All phig values are strings, so this parser only ever produces
 * {@link JsonToken#VALUE_STRING} tokens for values. Jackson's
 * deserializers handle coercion from strings to numbers, booleans, etc.
 */
class PhigJacksonParser extends ParserMinimalBase {

    private final PhigParser _parser;
    private ObjectCodec _codec;
    private boolean _closed;
    private JsonReadContext _parsingContext;
    private String _currentString;

    PhigJacksonParser(String input) {
        super();
        _parser = new PhigParser(input);
        _parsingContext = JsonReadContext.createRootContext(-1, -1, null);
    }

    @Override
    public JsonToken nextToken() throws IOException {
        if (_closed) {
            return null;
        }
        try {
            Event event = _parser.next();
            if (event == null) {
                _currToken = null;
                return null;
            }
            switch (event) {
                case START_MAP -> {
                    _parsingContext =
                        _parsingContext.createChildObjectContext(-1, -1);
                    _currToken = JsonToken.START_OBJECT;
                }
                case END_MAP -> {
                    _parsingContext = _parsingContext.getParent();
                    _currToken = JsonToken.END_OBJECT;
                }
                case KEY -> {
                    _currentString = _parser.getString();
                    _parsingContext.setCurrentName(_currentString);
                    _currToken = JsonToken.FIELD_NAME;
                }
                case START_LIST -> {
                    _parsingContext =
                        _parsingContext.createChildArrayContext(-1, -1);
                    _currToken = JsonToken.START_ARRAY;
                }
                case END_LIST -> {
                    _parsingContext = _parsingContext.getParent();
                    _currToken = JsonToken.END_ARRAY;
                }
                case STRING -> {
                    _currentString = _parser.getString();
                    _currToken = JsonToken.VALUE_STRING;
                }
            }
            return _currToken;
        } catch (PhigException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    // ── text access ─────────────────────────────────────────────────

    @Override
    public String getText() throws IOException {
        if (
            _currToken == JsonToken.FIELD_NAME ||
            _currToken == JsonToken.VALUE_STRING
        ) {
            return _currentString;
        }
        return _currToken != null ? _currToken.asString() : null;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        String text = getText();
        return text != null ? text.toCharArray() : null;
    }

    @Override
    public int getTextLength() throws IOException {
        String text = getText();
        return text != null ? text.length() : 0;
    }

    @Override
    public int getTextOffset() {
        return 0;
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    // ── context / naming ────────────────────────────────────────────

    @Override
    public String getCurrentName() throws IOException {
        return _parsingContext.getCurrentName();
    }

    @Override
    public void overrideCurrentName(String name) {
        try {
            _parsingContext.setCurrentName(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return _parsingContext;
    }

    // ── location (not tracked) ──────────────────────────────────────

    @Override
    public JsonLocation getTokenLocation() {
        return JsonLocation.NA;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return JsonLocation.NA;
    }

    // ── codec ───────────────────────────────────────────────────────

    @Override
    public ObjectCodec getCodec() {
        return _codec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _codec = c;
    }

    // ── lifecycle ───────────────────────────────────────────────────

    @Override
    public void close() throws IOException {
        _closed = true;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    // ── numeric access (phig has no numeric tokens) ─────────────────

    @Override
    public Number getNumberValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public NumberType getNumberType() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public int getIntValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public long getLongValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public float getFloatValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public double getDoubleValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        throw _constructError("phig values are strings, not numbers");
    }

    // ── binary / version ────────────────────────────────────────────

    @Override
    public byte[] getBinaryValue(Base64Variant bv) throws IOException {
        throw _constructError("phig does not support binary values");
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    // ── EOF handling ────────────────────────────────────────────────

    @Override
    protected void _handleEOF() throws JsonParseException {
        throw new JsonParseException(
            this,
            "Unexpected end-of-input in phig document"
        );
    }
}
