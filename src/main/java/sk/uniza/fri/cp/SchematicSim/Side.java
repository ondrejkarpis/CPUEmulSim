package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.Point2D;

/**
 * Strana súčiastky, na ktorej sa nachádza vývod (pin). Určuje smer, ktorým musí vodič
 * z pinu vychádzať kolmo, aby sa dal viesť ortogonálne (Manhattan štýl).
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public enum Side {
    LEFT(-1, 0),
    RIGHT(1, 0),
    TOP(0, -1),
    BOTTOM(0, 1);

    private final Point2D vector;

    Side(double x, double y) {
        this.vector = new Point2D(x, y);
    }

    /**
     * Jednotkový vektor smeru, ktorým z pinu na tejto strane vychádza vodič.
     */
    public Point2D vector() {
        return vector;
    }

    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT;
    }
}
