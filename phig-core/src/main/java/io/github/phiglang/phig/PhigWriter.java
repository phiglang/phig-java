package io.github.phiglang.phig;

import io.github.phiglang.phig.stream.PhigGenerator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * Writes a {@link PhigNode} tree to phig text format.
 */
public final class PhigWriter {

    public static String write(PhigNode node) throws PhigException {
        if (!node.isMap()) {
            throw new PhigException("top-level value must be a map");
        }
        StringWriter sw = new StringWriter();
        try {
            write(node, sw);
        } catch (IOException e) {
            throw new PhigException(e.getMessage());
        }
        return sw.toString();
    }

    public static void write(PhigNode node, Writer writer) throws PhigException, IOException {
        if (!node.isMap()) {
            throw new PhigException("top-level value must be a map");
        }
        PhigGenerator g = new PhigGenerator(writer);
        writeNode(node, g);
    }

    private static void writeNode(PhigNode node, PhigGenerator g) throws IOException {
        if (node.isMap()) {
            g.startMap();
            Iterator<Map.Entry<String, PhigNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, PhigNode> entry = it.next();
                g.key(entry.getKey());
                writeNode(entry.getValue(), g);
            }
            g.endMap();
        } else if (node.isList()) {
            g.startList();
            Iterator<PhigNode> it = node.elements();
            while (it.hasNext()) {
                writeNode(it.next(), g);
            }
            g.endList();
        } else {
            g.string(node.asString());
        }
    }
}
