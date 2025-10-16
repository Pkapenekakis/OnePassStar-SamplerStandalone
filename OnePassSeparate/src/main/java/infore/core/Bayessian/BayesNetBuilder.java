package infore.core.Bayessian;

import infore.core.graph.ChildrenIndex;
import infore.core.graph.NodeKey;
import infore.core.graph.NodeWeightIndex;

import java.util.List;
import java.util.Map;

/**
 * Builds CPTs from the side-stream DAG + computed group weights:
 *   numerator(x->y) = edgeWeight(x,y) * W(y)
 * One CPT per adjacent pair in layersLeftToRight.
 */
public final class BayesNetBuilder {
    private BayesNetBuilder() {}

    public static BayesNet build(List<String> layersLeftToRight,
                                 ChildrenIndex fanout,
                                 NodeWeightIndex weights) {

        BayesNet bn = new BayesNet();

        // For each adjacent pair e.g. A->B (AB, BC, CD, ...)
        for (int i = 0; i < layersLeftToRight.size() - 1; i++) {
            String leftLayer = layersLeftToRight.get(i);
            String rightLayer = layersLeftToRight.get(i + 1);
            String joinedStreams = leftLayer + rightLayer;

            CPTIndex cpt = new CPTIndex(joinedStreams, leftLayer, rightLayer);

            // Iterate all edges parent->child; keep only edges leftLayer->rightLayer
            for (Map.Entry<NodeKey, List<ChildrenIndex.Child>> e : fanout.snapshot().entrySet()) {
                NodeKey parent = e.getKey();
                if (!parent.layer.equals(leftLayer)) continue;

                for (ChildrenIndex.Child ch : e.getValue()) {
                    if (!ch.child.layer.equals(rightLayer)) continue; // enforce adjacency
                    double numer = ch.edgeWeight * weights.get(ch.child);
                    cpt.add(parent, ch.child, numer);
                }
            }

            bn.put(cpt);
        }

        return bn;
    }
}

