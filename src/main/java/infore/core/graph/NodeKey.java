package infore.core.graph;

import java.io.Serializable;
import java.util.Objects;

/** Canonical identity of a node: (layer, value). */
public final class NodeKey implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String stream;
    public final String value;
    private final int hash; // cached

    public NodeKey(String layer, String value){
        this.stream = Objects.requireNonNull(layer, "stream").trim();
        this.value = Objects.requireNonNull(value, "value").trim();
        if (this.stream.isEmpty() || this.value.isEmpty())
            throw new IllegalArgumentException("layer/value must be non-empty");
        this.hash = 31 * this.stream.hashCode() + this.value.hashCode();
    }

    /** Convenience: build a composite value from parts (safe, collision-free). */
    public static NodeKey ofParts(String stream, Object... parts){
        return new NodeKey(stream, encode(parts));
    }

    @Override public int hashCode(){ return hash; }

    @Override public boolean equals(Object o){
        if (!(o instanceof NodeKey)) return false;
        NodeKey k = (NodeKey) o;
        return stream.equals(k.stream) && value.equals(k.value);
    }

    @Override public String toString(){ return stream + ":" + value; }

    //If we need a composite key we need to encode in order to avoid delimiter bugs
    private static String encode(Object... parts){
        final char SEP = '\u001F'; // rarely present in data
        StringBuilder sb = new StringBuilder(parts.length * 8);
        for (int i = 0; i < parts.length; i++) {
            String s = String.valueOf(parts[i]).trim();
            sb.append(s.length()).append(':').append(s);
            if (i + 1 < parts.length) sb.append(SEP);
        }
        return sb.toString();
    }
}
