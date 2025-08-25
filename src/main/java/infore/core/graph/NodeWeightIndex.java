package infore.core.graph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

/**
 * Thread-safe map: NodeKey -> computed group weight W(u).
 * Use set(...) for final writes; add(...)/mergeFrom(...) for partial sums in parallel paths.
 */
public class NodeWeightIndex {
    private final ConcurrentHashMap<NodeKey, Double> w = new ConcurrentHashMap<>();

    /** Accumulate a delta using a primitive double combiner (adapts to BiFunction under the hood). */
    public void add(NodeKey k, double delta, DoubleBinaryOperator op) {
        w.merge(k, delta, op::applyAsDouble);
    }

    /** Overwrite with a finalized weight. */
    public void set(NodeKey k, double weight) { w.put(k, weight); }

    /** Read weight (0.0 if absent). */
    public double get(NodeKey k) { return w.getOrDefault(k, 0.0); }

    /** Live view for iteration/serialization. */
    public Map<NodeKey, Double> snapshot() { return w; }

    /** Merge another index using the given combiner (e.g., Double::sum, Math::max, overwrite). */
    public void mergeFrom(NodeWeightIndex other, DoubleBinaryOperator op) {
        other.w.forEach((k, v) -> w.merge(k, v, op::applyAsDouble)); //op.applyAsDouble(thisV, thatV)
    }
}