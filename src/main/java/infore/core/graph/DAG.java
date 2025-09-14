package infore.core.graph;

import java.util.*;

/**
 * Layered DAG built from side-stream edges.
 *
 * Validates that every StreamTuple connects ADJACENT layers according to the
 * provided layer order. Stores:
 *  - the declared layer order (left→right), and
 *  - the fanout adjacency (ChildrenIndex), and
 *  - a per-layer set of nodes (nodesByLayer) for convenience.
 */

public final class DAG {
    private final List<String> layersLeftToRight;
    private final ChildrenIndex children;
    private final Map<String, Set<NodeKey>> nodesByLayer;

    private DAG(List<String> ltr, ChildrenIndex ch, Map<String, Set<NodeKey>> nbl) {
        this.layersLeftToRight = Collections.unmodifiableList(new ArrayList<>(ltr));
        this.children = ch;
        this.nodesByLayer = nbl;
    }


    /**
     * Build a layered DAG from StreamTuples.
     *
     * @param layersLeftToRight e.g., ["A","B","C"] in join order
     * @param tuples            edges between adjacent layers (parent→child)
     * @return DAG with adjacency and per-layer node sets
     * @throws IllegalArgumentException if a tuple uses unknown or non-adjacent layers
     */
    public static DAG fromTuples(List<String> layersLeftToRight, Collection<StreamTuple> tuples) {
        // 1) Build position index for fast adjacency checks
        Map<String, Integer> pos = new HashMap<>();
        for (int i = 0; i < layersLeftToRight.size(); i++) {
            String layer = layersLeftToRight.get(i);
            if (pos.put(layer, i) != null) {
                throw new IllegalArgumentException("Duplicate layer in order: " + layer);
            }
        }

        // 2) Build childrenIndex + collect nodes per layer
        ChildrenIndex cIndex = new ChildrenIndex();
        Map<String, Set<NodeKey>> nodesByLayer = new HashMap<>();

        for (StreamTuple t : tuples) {
            NodeKey parent = t.leftLayer; // ("B","b1")
            NodeKey child  = t.rightLayer;  // ("C","c1")

            Integer L = pos.get(parent.layer);
            Integer R = pos.get(child.layer);
            if (L == null || R == null) {
                throw new IllegalArgumentException("Unknown layer(s) in edge: " + t);
            }
            if (R != L + 1) {
                throw new IllegalArgumentException(
                        "Non-adjacent edge " + parent.layer + "->" + child.layer +
                                " for declared order " + layersLeftToRight + " (edge: " + t + ")");
            }

            cIndex.addEdge(parent, child, t.edgeWeight);

            nodesByLayer.computeIfAbsent(parent.layer, k -> new HashSet<>()).add(parent);
            nodesByLayer.computeIfAbsent(child.layer,  k -> new HashSet<>()).add(child);
        }

        return new DAG(layersLeftToRight, cIndex, nodesByLayer);
    }
}
