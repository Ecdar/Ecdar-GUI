package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import java.util.ArrayList;

public class Transition {
    public final ArrayList<Edge> edges = new ArrayList<>();

    public Transition(ObjectProtos protoBufTransition) {
        // ToDo: Construct transition instance based on protoBuf input
    }
}
