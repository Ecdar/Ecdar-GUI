package SW9.presentations;

import SW9.abstractions.Location;

/**
 *
 */
public class LocationHelpInitialPresentation extends LocationPresentation {
    public LocationHelpInitialPresentation() {
        super(getLocation(), null, false);
    }

    public static Location getLocation() {
        final Location location = new Location("L0");
        location.setType(Location.Type.INITIAL);
        return location;
    }
}
