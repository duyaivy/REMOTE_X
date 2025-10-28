package client;

public enum Commands {
    PRESS_MOUSE(-1),
    RELEASE_MOUSE(-2),
    PRESS_KEY(-3),
    RELEASE_KEY(-4),
    MOVE_MOUSE(-5),
    QUANTITY(-6),
    MOUSE_WHEEL_MOVED(-7),
    MOUSE_DRAGGED(-8);

    private int abbrev;

    Commands(int abbrev) {
        this.abbrev = abbrev;
    }

    public int getAbbrev() {
        return this.abbrev;
    }
}
