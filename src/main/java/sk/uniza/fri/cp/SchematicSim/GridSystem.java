package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

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

    Point2D pixelToGrid(double x, double y) {
        return new Point2D(x / sizeX, y / sizeY);
    }

    Pane generateBackground(double width, double height, Paint bgColor, Paint linesColor) {
        Pane bck = new Pane(new Rectangle(width, height, bgColor));

        for (int x = 0; x <= width; x += sizeX) {
            bck.getChildren().add(generateGridLine(x, 0, x, height, linesColor));
        }
        for (int y = 0; y <= height; y += sizeY) {
            bck.getChildren().add(generateGridLine(0, y, width, y, linesColor));
        }

        return bck;
    }

    private Line generateGridLine(double startX, double startY, double endX, double endY, Paint lineColor) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(lineColor);
        line.setOpacity(0.5);
        line.setStrokeWidth(1);
        return line;
    }
}
