package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Pozadie plochy schémy s mriežkou. Čiary mriežky sa negenerujú pre celú plochu (pri veľkej
 * ploche by ich boli desaťtisíce), ale iba pre oblasť okolo práve viditeľnej časti -
 * {@link #showArea(Bounds)}. Čiary sa pregenerujú až keď viditeľná časť vyjde z vygenerovanej
 * oblasti (s rezervou na každú stranu), takže pri bežnom posúvaní sa nič neprepočítava.
 */
public class GridBackground extends Pane {

    private final double width;
    private final double height;
    private final int sizeX;
    private final int sizeY;
    private final Paint linesColor;
    private final Group lines = new Group();

    /** oblasť, pre ktorú sú čiary vygenerované (null = zatiaľ žiadne) */
    private Bounds generated;

    GridBackground(double width, double height, int sizeX, int sizeY, Paint bgColor, Paint linesColor) {
        this.width = width;
        this.height = height;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.linesColor = linesColor;
        // myš zachytáva obdĺžnik pozadia (udalosti prebublajú do tohto panelu), čiary nie
        lines.setMouseTransparent(true);
        getChildren().addAll(new Rectangle(width, height, bgColor), lines);
    }

    /**
     * Zabezpečí, aby boli čiary mriežky vygenerované pre danú oblasť (v lokálnych
     * súradniciach pozadia). Ak ju vygenerovaná oblasť už pokrýva, nič sa nedeje.
     */
    public void showArea(Bounds visible) {
        if (generated != null && generated.contains(clip(visible))) return;

        // rezerva polovice viditeľnej oblasti na každú stranu, zarovnaná na mriežku
        double mx = visible.getWidth() / 2;
        double my = visible.getHeight() / 2;
        double minX = Math.max(0, Math.floor((visible.getMinX() - mx) / sizeX) * sizeX);
        double minY = Math.max(0, Math.floor((visible.getMinY() - my) / sizeY) * sizeY);
        double maxX = Math.min(width, Math.ceil((visible.getMaxX() + mx) / sizeX) * sizeX);
        double maxY = Math.min(height, Math.ceil((visible.getMaxY() + my) / sizeY) * sizeY);
        if (maxX < minX || maxY < minY) return;
        generated = new BoundingBox(minX, minY, maxX - minX, maxY - minY);

        // existujúce čiary sa znovu použijú, chýbajúce sa doplnia, prebytočné odoberú
        javafx.collections.ObservableList<javafx.scene.Node> nodes = lines.getChildren();
        int i = 0;
        for (double x = minX; x <= maxX; x += sizeX) setLine(nodes, i++, x, minY, x, maxY);
        for (double y = minY; y <= maxY; y += sizeY) setLine(nodes, i++, minX, y, maxX, y);
        if (i < nodes.size()) nodes.remove(i, nodes.size());
    }

    /** viditeľná oblasť orezaná na plochu (mimo plochy nie je čo generovať) */
    private Bounds clip(Bounds b) {
        double minX = Math.max(0, b.getMinX());
        double minY = Math.max(0, b.getMinY());
        double maxX = Math.min(width, b.getMaxX());
        double maxY = Math.min(height, b.getMaxY());
        return new BoundingBox(minX, minY, Math.max(0, maxX - minX), Math.max(0, maxY - minY));
    }

    private void setLine(List<javafx.scene.Node> nodes, int index, double sx, double sy, double ex, double ey) {
        Line line;
        if (index < nodes.size()) {
            line = (Line) nodes.get(index);
        } else {
            line = new Line();
            line.setStroke(linesColor);
            line.setOpacity(0.5);
            line.setStrokeWidth(1);
            nodes.add(line);
        }
        line.setStartX(sx);
        line.setStartY(sy);
        line.setEndX(ex);
        line.setEndY(ey);
    }
}
