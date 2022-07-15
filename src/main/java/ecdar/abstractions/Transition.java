package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import ecdar.Ecdar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Transition {
    public final ArrayList<Edge> edges = new ArrayList<>();

    public Transition(ObjectProtos.Transition protoBufTransition) {
        List<ObjectProtos.Transition.EdgeTuple> protoBufEdges = (List<ObjectProtos.Transition.EdgeTuple>) protoBufTransition.getField(ObjectProtos.Transition.getDescriptor().findFieldByName("edges"));
        List<String> componentNames = protoBufEdges.stream().map(ObjectProtos.Transition.EdgeTuple::getComponentName).collect(Collectors.toList());
        List<String> edgeIds = protoBufEdges.stream().map(ObjectProtos.Transition.EdgeTuple::getId).collect(Collectors.toList());

        // For each affected component, find each affected edge within that component, and add these edges to the edges list
        List<Component> affectedComponents = Ecdar.getProject().getComponents().stream().filter(c -> componentNames.contains(c.getName())).collect(Collectors.toList());
        edges.addAll(affectedComponents.stream()
                        .map(c -> c.getEdges()
                                .stream()
                                .filter(e -> edgeIds.contains(e.getId()))
                                .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        );
    }
}
