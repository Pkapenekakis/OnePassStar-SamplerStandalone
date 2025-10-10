package infore.core.Bayessian;

import infore.core.graph.NodeKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conditional Probability Table for one adjacent pair (e.g., B->C).
 * For each left node x, stores a row of (right y, numerator) where:
 *   numerator(x->y) = edgeWeight(x,y) * W(y)
 * Probabilities are returned normalized per row at read time.
 * For slide example
 * Row key (left): A:a1
 * Entries:
 *   (right=B:b1, numerator= W(b1)=9)
 *   (right=B:b2, numerator= W(b2)=15)
 * Row total = 9 + 15
 * P(b1 | a1) = numerator(a1→b1) / rowTotals.get(a1);
 * P(b2 | a1) = numerator(a1→b2) / rowTotals.get(a1);
 */
public class CPTIndex {

    /** Helper Class that contains the group Weight of the specified node, used for mapping */
    //E.g. b1 will have nodeKey: b1 and numerator = W(b1) = 9
    public static final class Entry {
        public final NodeKey nodeKey;
        public final double numerator;
        public Entry(NodeKey nodeKey, double numerator) {
            this.nodeKey = nodeKey;
            this.numerator = numerator;
        }
        @Override public String toString() { return nodeKey + "@" + numerator; }
    }

    private final String joinedStreams;     // e.g., "BC"
    private final String leftLayer;         // e.g., "B"
    private final String rightLayer;        // e.g., "C"

    // HashMap that contains for each node an "Entry" List of its children (E.g.  A:a1 has an "Entry" list of children b1,b2 etc.)
    private final ConcurrentHashMap<NodeKey, List<Entry>> childrenList = new ConcurrentHashMap<>();
    // Maps each parent node (e.g A:a1) with the sum of all the children
    private final ConcurrentHashMap<NodeKey, Double> childrenSum = new ConcurrentHashMap<>();

    public CPTIndex(String joinedStreams, String leftLayer, String rightLayer) {
        this.joinedStreams = joinedStreams;
        this.leftLayer = leftLayer;
        this.rightLayer = rightLayer;
    }

    /** Add (right, numerator) to the parent node; ignores non-positive numerators.
     * When cpt.add(a1,b1,val) is called
     * 1) Appends (b1,val) to childrenList.get(a1)
     * 2) Updates childrenSum of a1 to sum the val with all the other vals it holds
     * After two calls looks like this
     * cpt.add(a1, b1, 9);   // childrenSum.get(a1) = 9
     * cpt.add(a1, b2, 15);  // childrenSum.get(a1) = 9 + 15 = 24
     * */
    public void add(NodeKey parentNode, NodeKey childNode, double numerator) {
        if (!(numerator > 0.0)) return;
        childrenList.computeIfAbsent(parentNode, k -> Collections.synchronizedList(new ArrayList<Entry>()))
                .add(new Entry(childNode, numerator));
        childrenSum.merge(parentNode, numerator, Double::sum);
    }

    /** After we know the final sum of all the Children we locally calculate the probabilities.
     *  This cannot happen in different workers probably.
     **/
    public List<Map.Entry<NodeKey, Double>> calculateChildrenProbabilities(NodeKey leftValue) {
        double total = childrenSum.getOrDefault(leftValue, 0.0);
        if (!(total > 0.0)) return Collections.emptyList();

        //Get a List of all the children entries
        List<Entry> es = childrenList.getOrDefault(leftValue, Collections.emptyList());
        List<Map.Entry<NodeKey, Double>> out = new ArrayList<>(es.size());

        //calculate the probability to choose the child, for each child
        for (Entry e : es) {
            out.add(new AbstractMap.SimpleEntry<>(e.nodeKey, e.numerator / total));
        }
        return out;
    }

    /** Sample y ~ P(y | leftValue). Returns null if the row is empty or total==0. */
    public NodeKey sample(NodeKey leftValue, Random rng) {
        double total = childrenSum.getOrDefault(leftValue, 0.0);
        if (!(total > 0.0)) return null;

        double rand = rng.nextDouble() * total;
        double acc = 0.0;
        List<Entry> es = childrenList.getOrDefault(leftValue, Collections.<Entry>emptyList());
        for (Entry e : es) {
            acc += e.numerator;
            if (rand <= acc) return e.nodeKey;
        }
        return es.isEmpty() ? null : es.get(es.size() - 1).nodeKey; // numeric safety
    }

    public String stream()     { return joinedStreams; }
    public String leftLayer()  { return leftLayer; }
    public String rightLayer() { return rightLayer; }



    /** Convenience: pretty-print a normalized row. */
    public String formatRow(NodeKey left) {
        List<Map.Entry<NodeKey, Double>> ps = calculateChildrenProbabilities(left);
        if (ps.isEmpty()) return joinedStreams + "[" + left + "]: (empty)";
        StringBuilder sb = new StringBuilder(joinedStreams).append("[").append(left).append("]: ");
        for (Map.Entry<NodeKey, Double> e : ps) {
            sb.append(e.getKey()).append("=")
                    .append(String.format(java.util.Locale.ROOT, "%.4f", e.getValue()))
                    .append("  ");
        }
        return sb.toString();
    }

    /** For inspection/testing. */
    public Map<NodeKey, List<Entry>> rows() { return childrenList; }
}

