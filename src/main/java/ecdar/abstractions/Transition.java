package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import ecdar.Ecdar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Transition {
    public final ArrayList<Edge> edges = new ArrayList<>();

    public Transition(ObjectProtos protoBufTransition) {
        // ToDo: Construct transition instance based on protoBuf input
    }
}
