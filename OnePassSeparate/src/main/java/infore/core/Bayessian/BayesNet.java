package infore.core.Bayessian;

import infore.core.graph.NodeKey;

import java.util.*;

/**
 * Holds CPTs for all adjacent pairs in the chain (e.g., "AB", "BC", "CD", ...).
 */
public class BayesNet {
    private final Map<String, CPTIndex> byStream = new LinkedHashMap<>();

    /** Register/replace a CPT under its stream label (e.g., "BC"). */
    public void put(CPTIndex cpt) { byStream.put(cpt.stream(), cpt); }

    /** Get CPT by stream label ("BC"), or null if not present. */
    public CPTIndex get(String stream) { return byStream.get(stream); }

    /** List all CPTs in insertion order, probably not needed */
    public List<CPTIndex> cpts() { return new ArrayList<>(byStream.values()); }

    /** Pretty-print a normalized row for quick debugging. */
    public String formatRow(String stream, NodeKey left) {
        CPTIndex cpt = get(stream);
        if (cpt == null) return "(no CPT " + stream + ")";
        List<Map.Entry<NodeKey, Double>> ps = cpt.calculateChildrenProbabilities(left);
        if (ps.isEmpty()) return stream + "[" + left + "]: (empty)";
        StringBuilder sb = new StringBuilder(stream).append("[").append(left).append("]: ");
        for (Map.Entry<NodeKey, Double> e : ps) {
            sb.append(e.getKey()).append("=")
                    .append(String.format(java.util.Locale.ROOT, "%.4f", e.getValue()))
                    .append("  ");
        }
        return sb.toString();
    }
}

