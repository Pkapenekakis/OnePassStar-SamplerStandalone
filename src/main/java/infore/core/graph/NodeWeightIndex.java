package infore.core.graph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

/**
 * NodeWeightIndex = thread-safe map { NodeKey -> W(node) } for Phase-1.
 * <p>
 * Why it supports TWO modes:
 *  1) Finalize-locally (partitioned by parent): each worker owns a parent and
 *     computes the final weight directly → use {@link #set(NodeKey, double)} only.
 *     This is the simplest/fastest path when load is balanced by parent.
 * <p>
 *  2) Partial-then-reduce (not partitioned by parent / hot-key salting):
 *     workers accumulate partial sums per parent with {@link #add(NodeKey, double, java.util.function.DoubleBinaryOperator)}
 *     (usually {@code Double::sum}); a reducer merges shard maps with
 *     {@link #mergeFrom(NodeWeightIndex, java.util.function.DoubleBinaryOperator)} and then calls {@link #set}.
 *     This handles skewed/high-degree parents safely.
 * <p>
 * Usage:
 *  <p>
 *  // Mode 1 (finalize locally)
 *  double sum = ...;
 *  for (Child e : children.children(parent)) sum += W.get(e.child);
 *  W.set(parent, prior(parent) + sum + leafBonus(parent));
 * <p>
 *  // Mode 2 (partials + reduce)
 *  add(parentOrSalted, childW, Double::sum);       // on workers, many times
 *  global.mergeFrom(workerPartials, Double::sum);  // on reducer
 *  set(parent, prior + global.get(parent) + leaf); // finalize once
 * <p>
 * Also provides {@link #get(NodeKey)} for lookups (defaults to 0.0) and {@link #snapshot()}
 * for iteration/serialization. Backed by ConcurrentHashMap; ops are thread-safe.
 */
public class NodeWeightIndex {
    private final ConcurrentHashMap<NodeKey, Double> cHashMap = new ConcurrentHashMap<>();

    /**
     * Atomically folds a contribution into the weight for the given node.
     * If the node is new, inserts {@code delta};
     * Otherwise combines the existing value and
     * {@code delta} via the provided operator (e.g., {@code Double::sum}).
     * Useful for accumulating ΣW(child) before writing the final weight with {@link #set}.
     */
    public void add(NodeKey key, double delta, DoubleBinaryOperator op) {
        cHashMap.merge(key, delta, op::applyAsDouble);
    }

    /** Write the final weight for a node in the HashMap*/
    public void set(NodeKey key, double weight) { cHashMap.put(key, weight); }

    /** Read weight (0 if absent). */
    public double get(NodeKey key) { return cHashMap.getOrDefault(key, 0.0); }

    /** Live view for iteration/serialization.
     * Can use snapshot().containsKey(k) to check if a node is missing
     * */
    public Map<NodeKey, Double> snapshot() { return cHashMap; }

    /**
     * Merge all entries from {@code other} into this index.
     * For each key k: if absent, insert; otherwise combine {@code old} and {@code other}
     * via {@code op} (e.g., {@code Double::sum}, {@code Math::max}, or overwrite).
     */
    public void mergeFrom(NodeWeightIndex other, DoubleBinaryOperator op) {
        other.cHashMap.forEach((key, val) -> cHashMap.merge(key, val, op::applyAsDouble)); //op.applyAsDouble(thisV, thatV)
    }
}