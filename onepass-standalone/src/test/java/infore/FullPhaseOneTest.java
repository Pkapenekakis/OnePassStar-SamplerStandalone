package infore;
import org.junit.jupiter.api.Test;

import infore.core.Bayessian.BayesNet;
import infore.core.Bayessian.BayesNetBuilder;
import infore.core.Bayessian.CPTIndex;
import infore.core.graph.*;
import infore.core.weights.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FullPhaseOneTest {

    private static StreamTuple e(String L, String lVal, String R, String rVal) {
        return new StreamTuple(new NodeKey(L, lVal), new NodeKey(R, rVal), 1.0);
    }

    @Test
    void slideExample_EndToEnd() {
        // ---- Step 0: input (from the slide) ----
        List<String> order = Arrays.asList("A","B","C");
        List<StreamTuple> edges = Arrays.asList(
                e("A","a1","B","b1"),
                e("A","a1","B","b2"),
                e("B","b1","C","c1"),
                e("B","b2","C","c2"),
                e("B","b2","C","c3")
        );

        Map<NodeKey, Double> priors = new HashMap<>();
        priors.put(new NodeKey("A","a1"), 1.0);
        priors.put(new NodeKey("B","b1"), 3.0);
        priors.put(new NodeKey("B","b2"), 5.0);
        priors.put(new NodeKey("C","c1"), 5.0);
        priors.put(new NodeKey("C","c2"), 7.0);
        priors.put(new NodeKey("C","c3"), 1.0);

        double leafBase = 1.0;

        // ---- Step 1: DAG ----
        DAG dag = DAG.fromTuples(order, edges);

        // ---- Step 2: Group weights (right -> left) ----
        NodeWeightIndex W = GroupWeightsCalculator.compute(
                dag.layersRightToLeft(),
                dag.children(),
                k -> priors.getOrDefault(k, 0.0),
                leafBase,
                DownstreamAggregator.SUM_CHILDREN
        );

        // Assert weights match the presentation slides
        assertEquals(6.0,  W.get(new NodeKey("C","c1")), 1e-9);
        assertEquals(8.0,  W.get(new NodeKey("C","c2")), 1e-9);
        assertEquals(2.0,  W.get(new NodeKey("C","c3")), 1e-9);
        assertEquals(9.0,  W.get(new NodeKey("B","b1")), 1e-9);
        assertEquals(15.0, W.get(new NodeKey("B","b2")), 1e-9);
        assertEquals(25.0, W.get(new NodeKey("A","a1")), 1e-9);

        // ---- Step 3: Bayesian Network (CPTs) ----
        BayesNet bn = BayesNetBuilder.build(dag.layersLeftToRight(), dag.children(), W);

        // BC row for b2, b1: P(c2|b2)=8/(8+2)=0.8, P(c3|b2)=0.2, P(c1|b1)=1
        CPTIndex bc = bn.get("BC");
        Map<NodeKey, Double> bcRow = toMap(bc.calculateChildrenProbabilities(new NodeKey("B","b2")));
        Map<NodeKey, Double> bcRow1 = toMap(bc.calculateChildrenProbabilities(new NodeKey("B","b1")));
        assertEquals(0.8, bcRow.get(new NodeKey("C","c2")), 1e-9);
        assertEquals(0.2, bcRow.get(new NodeKey("C","c3")), 1e-9);
        assertEquals(1, bcRow1.get(new NodeKey("C","c1")), 1e-9);

        // AB row for a1: P(b1|a1)=9/(9+15)=0.375, P(b2|a1)=0.625
        CPTIndex ab = bn.get("AB");
        Map<NodeKey, Double> abRow = toMap(ab.calculateChildrenProbabilities(new NodeKey("A","a1")));
        assertEquals(9.0 / (9.0 + 15.0),  abRow.get(new NodeKey("B","b1")), 1e-9);
        assertEquals(15.0 / (9.0 + 15.0), abRow.get(new NodeKey("B","b2")), 1e-9);
    }

    private static Map<NodeKey, Double> toMap(List<Map.Entry<NodeKey, Double>> entries) {
        Map<NodeKey, Double> m = new HashMap<>();
        for (Map.Entry<NodeKey, Double> e : entries) m.put(e.getKey(), e.getValue());
        return m;
    }
}
