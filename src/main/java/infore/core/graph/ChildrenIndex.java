package infore.core.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe fanout index: parent NodeKey -> list of children (with stored edgeWeight).
 * This is the adjacency list Phase-2 will use, and Phase-1 reads during the bottom-up pass.
 */
public class ChildrenIndex {

    /** A child link (child node + stored edgeWeight for future use). */
    public static final class Child {
        public final NodeKey child;
        public final double edgeWeight;
        public Child(NodeKey child, double edgeWeight) {
            this.child = Objects.requireNonNull(child, "child");
            this.edgeWeight = edgeWeight;
        }
    }

    private final ConcurrentHashMap<NodeKey, List<Child>> fanout = new ConcurrentHashMap<>();

    /** Add an edge parent -> child. */
    public void addEdge(NodeKey parent, NodeKey child, double edgeWeight) {
        fanout.computeIfAbsent(parent, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new Child(child, edgeWeight));
    }

    /** All children of a parent (empty list if none). */
    public List<Child> children(NodeKey parent) {
        return fanout.getOrDefault(parent, Collections.emptyList());
    }

    /** Live view for iteration/serialization. */
    public Map<NodeKey, List<Child>> snapshot() { return fanout; }

    /** Merge another fanout index (union of edges). */
    public void mergeFrom(ChildrenIndex other) {
        other.fanout.forEach((p, list) -> list.forEach(c -> addEdge(p, c.child, c.edgeWeight)));
    }
}