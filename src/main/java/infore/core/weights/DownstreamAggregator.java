package infore.core.weights;

import infore.core.graph.ChildrenIndex;

/**
 * Strategy for how a parent combines a child's contribution during the bottom-up pass.
 * For Phase-1 rule, use SUM_CHILDREN: combine(edge, childW) = childW; reduce = sum.
 */
public interface DownstreamAggregator {
    /** How much this child contributes to its parent (given the child's already-computed weight). */
    double childContribution(ChildrenIndex.Child edgeToChild, double childWeight);

    /** Accumulate many child contributions into one total (default: sum). */
    default double accumulate(double total, double nextContribution) { return total + nextContribution; }

    /** W(parent) = Wnode + Σ Weight(child) */
    DownstreamAggregator SUM_CHILDREN = new DownstreamAggregator() {
        @Override public double childContribution(ChildrenIndex.Child ch, double childWeight) { return childWeight; }
    };
}