package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.Point2D;
import javafx.scene.paint.Paint;

/**
 * Mriežková súradnicová sústava plochy schémy. Prevzaté bezo zmeny z BreadboardSim.GridSystem -
 * mriežka je nezávislá od breadboardu, je to čisto prevod grid <-> pixel súradníc.
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public class GridSystem {

    private final int sizeX;
    private final int sizeY;

    public GridSystem(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    public GridSystem(int size) {
        this.sizeX = size;
        this.sizeY = size;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeMin() {
        return Math.min(sizeX, sizeY);
    }

    public int getSizeMax() {
        return Math.max(sizeX, sizeY);
    }

    public Point2D gridToPixel(int gridX, int gridY) {
        return new Point2D(gridX * sizeX, gridY * sizeY);
    }

    public Point2D pixelToGrid(double x, double y) {
        return new Point2D(x / sizeX, y / sizeY);
    }

    /**
     * Pozadie plochy s mriežkou. Čiary mriežky sa kreslia iba okolo viditeľnej časti -
     * volajúci ju hlási cez {@link GridBackground#showArea}.
     */
    public GridBackground generateBackground(double width, double height, Paint bgColor, Paint linesColor) {
        return new GridBackground(width, height, sizeX, sizeY, bgColor, linesColor);
    }
}
