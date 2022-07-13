package ecdar.abstractions;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.ObjectProtos;

import java.util.ArrayList;
import java.util.List;

public class Transition {
    public final ArrayList<Edge> edges = new ArrayList<>();

    public Transition(ObjectProtos.Transition protoBufTransition) {
        List<ComponentProtos.Edge> protoBufEdges = (List<ComponentProtos.Edge>) protoBufTransition.getField(ObjectProtos.Transition.getDescriptor().findFieldByName("edges"));
        for (ComponentProtos.Edge protoBufEdge : protoBufEdges) {
            edges.add(new Edge(protoBufEdge));
        }
    }
}
