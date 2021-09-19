package ecdar.utility.protobuf;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.abstractions.Nail;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.protobuf.ProtoBufMessages.ComponentProtos;
import ecdar.utility.protobuf.ProtoBufMessages.QueryProtos;

import java.util.ArrayList;

public class ProtoBufConverter {
    public static QueryProtos.Query getQueryProtoBuf(String query) {
        QueryProtos.Query.Builder queryBuilder = QueryProtos.Query.newBuilder();

        queryBuilder.setQuery(query)
                .addAllComponents(getProtoBufComponents());

        return queryBuilder.build();
    }

    private static ArrayList<ComponentProtos.Component> getProtoBufComponents() {
        ArrayList<ComponentProtos.Component> components = new ArrayList<>();

        for (Component component : Ecdar.getProject().getComponents()) {
            ComponentProtos.Component.Builder componentBuilder = ComponentProtos.Component.newBuilder();

            componentBuilder.setName(component.getName())
                    .setDeclarations(component.getDeclarationsText())
                    .addAllLocation(getProtoBufLocations(component))
                    .addAllEdge(getProtoBufEdges(component))
                    .setDescription(component.getDescription())
                    .setX(component.getBox().getX())
                    .setY(component.getBox().getY())
                    .setWidth(component.getBox().getWidth())
                    .setHeight(component.getBox().getHeight())
                    .setColor(EnabledColor.getIdentifier(component.getColor()))
                    .setIncludeInPeriodicCheck(component.isIncludeInPeriodicCheck());

            components.add(componentBuilder.build());
        }

        return components;
    }

    private static ArrayList<ComponentProtos.Location> getProtoBufLocations(Component component) {
        ArrayList<ComponentProtos.Location> locations = new ArrayList<>();

        for (Location loc : component.getLocations()) {
            ComponentProtos.Location.Builder locationBuilder = ComponentProtos.Location.newBuilder();

            locationBuilder.setId(loc.getId())
                    .setNickname(loc.getNickname())
                    .setInvariant(loc.getInvariant())
                    .setType(loc.getType().name())
                    .setUrgency(loc.getUrgency().name())
                    .setX(loc.getX())
                    .setY(loc.getY())
                    .setColor(EnabledColor.getIdentifier(loc.getColor()))
                    .setNicknameX(loc.getNicknameX())
                    .setNicknameY(loc.getNicknameY())
                    .setInvariantX(loc.getInvariantX())
                    .setInvariantY(loc.getInvariantY());

            locations.add(locationBuilder.build());
        }

        return locations;
    }

    private static ArrayList<ComponentProtos.Edge> getProtoBufEdges(Component component) {
        ArrayList<ComponentProtos.Edge> edges = new ArrayList<>();

        for (Edge edge : component.getEdges()) {
            ComponentProtos.Edge.Builder edgeBuilder = ComponentProtos.Edge.newBuilder();

            edgeBuilder.setSourceLocation(edge.getSourceLocation().getId())
                    .setTargetLocation(edge.getTargetLocation().getId())
                    .setStatus(edge.getStatus().toString())
                    .setSelect(edge.getSelect())
                    .setGuard(edge.getGuard())
                    .setUpdate(edge.getUpdate())
                    .setSync(edge.getSync())
                    .addAllNail(getProtoBufNails(edge));

            edges.add(edgeBuilder.build());
        }

        return edges;
    }

    private static ArrayList<ComponentProtos.Nail> getProtoBufNails(Edge edge) {
        ArrayList<ComponentProtos.Nail> nails = new ArrayList<>();

        for (Nail nail : edge.getNails()) {
            ComponentProtos.Nail.Builder nailBuilder = ComponentProtos.Nail.newBuilder();

            nailBuilder.setX(nail.getX())
                    .setY(nail.getY())
                    .setPropertyType(nail.getPropertyType().name())
                    .setPropertyX(nail.getPropertyX())
                    .setPropertyX(nail.getPropertyY());

            nails.add(nailBuilder.build());
        }

        return nails;
    }
}
