package io.github.phiglang.phig.jackson;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import io.github.phiglang.phig.stream.PhigGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Jackson {@link JsonGenerator} that streams phig text directly to a
 * {@link Writer} via {@link PhigGenerator}.
 */
class PhigJacksonGenerator extends JsonGenerator {

    private final Writer _output;
    private final PhigGenerator _writer;
    private ObjectCodec _codec;
    private boolean _closed;
    private int _features;
    private final JsonWriteContext _writeContext;

    PhigJacksonGenerator(Writer output) {
        _output = output;
        _writer = new PhigGenerator(output);
        _writeContext = JsonWriteContext.createRootContext(null);
    }

    @Override
    public void writeStartObject() throws IOException {
        _writer.startMap();
    }

    @Override
    public void writeEndObject() throws IOException {
        _writer.endMap();
    }

    @Override
    public void writeStartArray() throws IOException {
        _writer.startList();
    }

    @Override
    public void writeEndArray() throws IOException {
        _writer.endList();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        _writer.key(name);
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException {
        _writer.key(name.getValue());
    }

    @Override
    public void writeString(String text) throws IOException {
        _writer.string(text != null ? text : "");
    }

    @Override
    public void writeString(char[] buffer, int offset, int len)
        throws IOException {
        writeString(new String(buffer, offset, len));
    }

    @Override
    public void writeString(SerializableString text) throws IOException {
        writeString(text.getValue());
    }

    @Override
    public void writeNumber(int v) throws IOException {
        _writer.string(Integer.toString(v));
    }

    @Override
    public void writeNumber(long v) throws IOException {
        _writer.string(Long.toString(v));
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        _writer.string(v.toString());
    }

    @Override
    public void writeNumber(double v) throws IOException {
        _writer.string(Double.toString(v));
    }

    @Override
    public void writeNumber(float v) throws IOException {
        _writer.string(Float.toString(v));
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        _writer.string(v.toPlainString());
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        _writer.string(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        _writer.string(Boolean.toString(state));
    }

    @Override
    public void writeNull() throws IOException {
        _writer.string("");
    }

    @Override
    public void writeRaw(String text) throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRaw(char c) throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRawValue(String text, int offset, int len)
        throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len)
        throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length)
        throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length)
        throws IOException {
        throw _unsupported();
    }

    @Override
    public void writeBinary(Base64Variant bv, byte[] data, int offset, int len)
        throws IOException {
        throw _unsupported();
    }

    @Override
    public int writeBinary(Base64Variant bv, InputStream data, int dataLength)
        throws IOException {
        throw _unsupported();
    }

    private IOException _unsupported() {
        return new IOException("phig does not support raw or binary content");
    }

    @Override
    public void flush() throws IOException {
        _output.flush();
    }

    @Override
    public void close() throws IOException {
        if (!_closed) {
            flush();
            _closed = true;
            _output.close();
        }
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    @Override
    public JsonStreamContext getOutputContext() {
        return _writeContext;
    }

    @Override
    public ObjectCodec getCodec() {
        return _codec;
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec oc) {
        _codec = oc;
        return this;
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        return this;
    }

    @Override
    public JsonGenerator enable(Feature f) {
        _features |= f.getMask();
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        _features &= ~f.getMask();
        return this;
    }

    @Override
    public boolean isEnabled(Feature f) {
        return (_features & f.getMask()) != 0;
    }

    @Override
    public int getFeatureMask() {
        return _features;
    }

    @Deprecated
    @Override
    public JsonGenerator setFeatureMask(int values) {
        _features = values;
        return this;
    }

    @Override
    public void writeTree(TreeNode rootNode) throws IOException {
        if (rootNode == null) {
            writeNull();
        } else if (_codec != null) {
            _codec.writeTree(this, rootNode);
        } else {
            throw new IllegalStateException("No ObjectCodec set");
        }
    }

    @Override
    public void writeObject(Object value) throws IOException {
        if (value == null) {
            writeNull();
        } else if (_codec != null) {
            _codec.writeValue(this, value);
        } else {
            throw new IllegalStateException("No ObjectCodec set");
        }
    }
}
