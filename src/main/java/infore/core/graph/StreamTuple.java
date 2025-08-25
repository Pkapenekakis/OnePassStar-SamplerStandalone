package infore.core.graph;

import java.util.Objects;

/**
 * Immutable DTO representing one edge between two adjacent layers in the
 * multipartite join graph used by One*-Pass Phase 1.
 * <p>
 * Each tuple encodes a directed edge from a {@code leftLayer:leftValue} node
 * to a {@code rightLayer:rightValue} node and carries an {@code edgeWeight}.
 * Phase 1 ingests these edges to build:
 * <ul>
 *   <li>the Parent→Children fanout (per parent node), and</li>
 *   <li>bottom-up node weights used later for ancestral sampling.</li>
 * </ul>
 *
 * <h3>Semantics</h3>
 * <ul>
 *   <li><b>stream</b> — Logical stream/relation name (e.g., "AB", "BC" or a Kafka topic).</li>
 *   <li><b>leftLayer</b> — The source layer name (e.g., "A", "B"). Must be adjacent to {@code rightLayer} in the DAG.</li>
 *   <li><b>leftValue</b> — The concrete key/value at {@code leftLayer} (e.g., primary or join key).</li>
 *   <li><b>rightLayer</b> — The destination layer name (e.g., "B", "C").</li>
 *   <li><b>rightValue</b> — The concrete key/value at {@code rightLayer}.</li>
 *   <li><b>edgeWeight</b> — Non-negative weight for this edge. Use {@code 1.0} for uniform edges,
 *       or encode tuple weights/selectivities if available.</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * // AB and BC side streams
 * syn.add(new StreamTuple("AB", "A", "a1", "B", "b1", 1));
 * syn.add(new StreamTuple("BC", "B", "b1", "C", "c9", 3.0));
 * }</pre>
 */
public final class StreamTuple {
    /** Logical stream or relation identifier (e.g., "AB"). */
    public final String stream; //May be useless
    public final String leftLayer;
    public final String leftValue;
    public final String rightLayer;
    public final String rightValue;
    public final double edgeWeight; //May be useless


    public StreamTuple(String stream, String leftLayer, String leftValue,
                       String rightLayer, String rightValue, double edgeWeight) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.leftLayer = Objects.requireNonNull(leftLayer, "leftLayer");
        this.leftValue = Objects.requireNonNull(leftValue, "leftValue");
        this.rightLayer = Objects.requireNonNull(rightLayer, "rightLayer");
        this.rightValue = Objects.requireNonNull(rightValue, "rightValue");
        if (!Double.isFinite(edgeWeight) || edgeWeight < 0.0) {
            throw new IllegalArgumentException("edgeWeight must be finite and >= 0, current value: " + edgeWeight);
        }
        this.edgeWeight = edgeWeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stream, leftLayer, leftValue, rightLayer, rightValue);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StreamTuple)) return false;
        StreamTuple t = (StreamTuple) o;
        return stream.equals(t.stream)
                && leftLayer.equals(t.leftLayer)
                && leftValue.equals(t.leftValue)
                && rightLayer.equals(t.rightLayer)
                && rightValue.equals(t.rightValue);
    }

    @Override
    public String toString() {
        return "StreamTuple{" +
                "stream='" + stream + '\'' +
                ", " + leftLayer + ":" + leftValue +
                " -> " + rightLayer + ":" + rightValue +
                ", w=" + edgeWeight +
                '}';
    }

    //UNUSED

    /** Convenience: derive stream label by concatenating layer names (e.g., "BC"). */
    public static StreamTuple of(String leftLayer, String leftValue,
                                 String rightLayer, String rightValue,
                                 double edgeWeight) {
        return new StreamTuple(leftLayer + rightLayer, leftLayer, leftValue, rightLayer, rightValue, edgeWeight);
    }

    /** Convenience: build from NodeKeys. */
    public static StreamTuple from(String stream, NodeKey parent, NodeKey child, double edgeWeight) {
        return new StreamTuple(stream, parent.stream, parent.value, child.stream, child.value, edgeWeight);
    }
}
