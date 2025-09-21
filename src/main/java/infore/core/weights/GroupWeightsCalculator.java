package infore.core.weights;

import infore.core.graph.ChildrenIndex;
import infore.core.graph.NodeKey;
import infore.core.graph.NodeWeightIndex;

import java.util.*;
import java.util.function.Function;

/**
 * Bottom-up computation of group weights:
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
     * @param childrenIndex       fanouts (parent -> childrenIndex)
     * @param priorFn             Wnode(u) provider; return 0.0 if you have no priors
     * @param leafBase            Set as 1.0 based on the thesis Paper
     * @param agg                 downstream aggregator (use DownstreamAggregator.SUM_CHILDREN)
     * @return NodeWeightIndex filled with W(u)
     */
    public static NodeWeightIndex compute(
            List<String> layersRightToLeft,
            ChildrenIndex childrenIndex,
            Function<NodeKey, Double> priorFn,
            double leafBase,
            DownstreamAggregator agg) {

        NodeWeightIndex weightIndex = new NodeWeightIndex();

        //Collect nodes per layer from fanouts (parents and childrenIndex).
        Map<String, Set<NodeKey>> nodesByLayer = new HashMap<>();
        childrenIndex.snapshot().forEach((nKey, list) -> {
            nodesByLayer.computeIfAbsent(nKey.layer, k -> new HashSet<>()).add(nKey);
            for (ChildrenIndex.Child c : list)
                nodesByLayer.computeIfAbsent(c.child.layer, k -> new HashSet<>()).add(c.child);
        });

        for (String layer : layersRightToLeft) {
            for (NodeKey nKey : nodesByLayer.getOrDefault(layer, Collections.emptySet())) {
                List<ChildrenIndex.Child> childList = childrenIndex.getChildren(nKey);
                double nodeTotalWeight = 0.0;

                //For all the children of this parent
                for (ChildrenIndex.Child ch : childList) {
                    //Change the aggregator instead of the loop with this implementation
                    double childWeight = agg.childContribution(ch, weightIndex.get(ch.child));
                    nodeTotalWeight = agg.accumulate(nodeTotalWeight, childWeight);
                }

                double prior = priorFn.apply(nKey);
                double leaf = childList.isEmpty() ? leafBase : 0.0;
                weightIndex.set(nKey, prior + nodeTotalWeight + leaf);
            }
        }
        return weightIndex;
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