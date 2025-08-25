package infore.core.weights;

import infore.core.graph.ChildrenIndex;

/**
 * Strategy for how a parent combines a child's contribution during the bottom-up pass.
 * For Phase-1 rule, use SUM_CHILDREN: combine(edge, childW) = childW; reduce = sum.
 */
public interface DownstreamAggregator {
    /** How an edge contributes from child to parent (given child's already-computed weight). */
    double combine(ChildrenIndex.Child edgeToChild, double childWeight);

    /** How to aggregate multiple children (default: sum). */
    default double reduce(double a, double b) { return a + b; }

    /** Aggregator for your slide: parent weight sums children's weights (ignore edgeWeight). */
    DownstreamAggregator SUM_CHILDREN = new DownstreamAggregator() {
        @Override public double combine(ChildrenIndex.Child e, double childWeight) { return childWeight; }
    };
}