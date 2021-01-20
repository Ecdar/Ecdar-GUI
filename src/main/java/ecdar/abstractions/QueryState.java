package ecdar.abstractions;

import ecdar.utility.colors.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.material.Material;

public enum QueryState {
    RUNNING(Color.GREY_BLUE, Color.Intensity.I600, Material.HOURGLASS_EMPTY, 0),
    SUCCESSFUL(Color.GREEN, Color.Intensity.I700, Material.DONE, 1),
    ERROR(Color.RED, Color.Intensity.I700, Material.CLEAR, 2),
    UNKNOWN(Color.GREY, Color.Intensity.I600, Material.HELP, 3),
    SYNTAX_ERROR(Color.YELLOW, Color.Intensity.I700, Material.WARNING, 4);

    private final Color color;
    private final Color.Intensity colorIntensity;
    private final Ikon iconCode;
    private final int statusCode;

    QueryState(final Color color, final Color.Intensity colorIntensity, Ikon ikon, int statusCode) {
        this.color = color;
        this.colorIntensity = colorIntensity;
        this.iconCode = ikon;
        this.statusCode = statusCode;
    }

    public Color getColor() {
        return color;
    }

    public Color.Intensity getColorIntensity() {
        return colorIntensity;
    }

    public Ikon getIconCode() { return iconCode; }

    public int getStatusCode() { return statusCode; }
}
