package ecdar.utility.keyboard;

public enum NudgeDirection {
    UP(0, -10),
    RIGHT(10, 0),
    DOWN(0, 10),
    LEFT(-10, 0);

    private int x;
    private int y;

    NudgeDirection(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public double getXOffset() {
        return x;
    }

    public double getYOffset() {
        return y;
    }

    public NudgeDirection reverse() {
        if (this.equals(UP)) return DOWN;
        else if (this.equals(LEFT)) return RIGHT;
        else if (this.equals(RIGHT)) return LEFT;
        else return UP;
    }
}
