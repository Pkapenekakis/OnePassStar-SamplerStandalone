package infore.core.graph;

import java.util.Objects;

public final class StreamTuple {
    public final NodeKey leftLayer;
    public final NodeKey rightLayer;
    public final double edgeWeight; //May be useless, could be used in the Bayesian network


    public StreamTuple(NodeKey parent, NodeKey child, double edgeWeight) {
        this.leftLayer = Objects.requireNonNull(parent, "Left Layer must not be null");
        this.rightLayer  = Objects.requireNonNull(child, "Right Layer must not be null");
        if (!Double.isFinite(edgeWeight) || edgeWeight < 0.0)
            throw new IllegalArgumentException("edgeWeight must be finite and >= 0");
        this.edgeWeight = edgeWeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftLayer, rightLayer);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StreamTuple)) return false;
        StreamTuple t = (StreamTuple) o;
        return leftLayer.equals(t.leftLayer) && rightLayer.equals(t.rightLayer);
    }

    @Override public String toString() {
        return "Edge{" + leftLayer + " -> " + rightLayer + ", w=" + edgeWeight + "}";
    }

}
