package ecdar.simulation;

import ecdar.abstractions.Location;

import java.util.ArrayList;

public class EcdarSystemState {
    private ArrayList<Location> locations = new ArrayList<Location>();
    // Zone (federation, readable something)

     public EcdarSystemState(ArrayList<Location> locations) {
        this.locations = locations;
     }

    public ArrayList<Location> getLocations() {
        return locations;
    }
}
