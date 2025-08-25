package infore.core.weights;

import infore.core.graph.ChildrenIndex;
import infore.core.graph.NodeKey;
import infore.core.graph.NodeWeightIndex;

import java.util.*;
import java.util.function.Function;

/**
 * Bottom-up computation of group weights per your rule:
 *   W(u) = Wnode(u) + Σ_{v in children(u)} W(v)
 * with a leaf bonus (+leafBase) when a node has no children.
 *
 * The algorithm proceeds right-to-left over the provided layer order.
 */
public class GroupWeightsCalculator {

    /**
     * Compute group weights.
     *
     * @param layersRightToLeft   e.g., ["C","B","A"]
     * @param children            fanouts (parent -> children)
     * @param priorFn             Wnode(u) provider; return 0.0 if you have no priors
     * @param leafBase            bonus for leaves (use 1.0 to match your slides)
     * @param agg                 downstream aggregator (use DownstreamAggregator.SUM_CHILDREN)
     * @return NodeWeightIndex filled with W(u)
     */
    public static NodeWeightIndex compute(
            List<String> layersRightToLeft,
            ChildrenIndex children,
            Function<NodeKey, Double> priorFn,
            double leafBase,
            DownstreamAggregator agg) {

        NodeWeightIndex W = new NodeWeightIndex();

        // Collect nodes per layer from fanouts (parents and children).
        Map<String, Set<NodeKey>> nodesByLayer = new HashMap<>();
        children.snapshot().forEach((p, list) -> {
            nodesByLayer.computeIfAbsent(p.stream, k -> new HashSet<>()).add(p);
            for (ChildrenIndex.Child c : list)
                nodesByLayer.computeIfAbsent(c.child.stream, k -> new HashSet<>()).add(c.child);
        });

        for (String layer : layersRightToLeft) {
            for (NodeKey u : nodesByLayer.getOrDefault(layer, Collections.emptySet())) {
                List<ChildrenIndex.Child> fan = children.children(u);
                double accum = 0.0;
                for (ChildrenIndex.Child ch : fan) {
                    double term = agg.combine(ch, W.get(ch.child));
                    accum = agg.reduce(accum, term);
                }
                double prior = priorFn.apply(u);
                double leaf = fan.isEmpty() ? leafBase : 0.0;
                W.set(u, prior + accum + leaf);
            }
        }
        return W;
    }

    /** Convenience overload when priors are provided as a map. */
    public static NodeWeightIndex compute(
            List<String> layersRightToLeft,
            ChildrenIndex children,
            Map<NodeKey, Double> priors,
            double leafBase,
            DownstreamAggregator agg) {

        return compute(layersRightToLeft, children, k -> priors.getOrDefault(k, 0.0), leafBase, agg);
    }
}