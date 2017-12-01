package SW9.presentations;

import SW9.abstractions.Location;

/**
 *
 */
public class LocationHelpUrgentPresentation extends LocationPresentation {
    public LocationHelpUrgentPresentation() {
        super(getLocation(), null, false);
    }

    public static Location getLocation() {
        final Location location = new Location("L1");
        location.setUrgency(Location.Urgency.URGENT);
        return location;
    }
}
