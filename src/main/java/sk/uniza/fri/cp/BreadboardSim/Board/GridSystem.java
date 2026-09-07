package sk.uniza.fri.cp.BreadboardSim.Board;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Mriežková súradnicová sústava plochy simulátora.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class GridSystem {

	private int sizeX;
	private int sizeY;

    /**
     * Vytvára súradnicovú sústavu s rôzmymi rozmermi na X-ovej a Y-ovej osy.
     *
     * @param sizeX Veľkosť mriežky na X-ovej osy v pixeloch
     * @param sizeY Veľkosť mriežky na Y-ovej osy v pixeloch
     */
    public GridSystem(int sizeX, int sizeY){
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    /**
     * Vytvára súradnicovú sústavu s rovnakým rozmerom na X-ovej aj Y-ovej osy.
     *
     * @param size Veľkosť strany štvorčeka v pixeloch
     */
    public GridSystem(int size){
        this.sizeX = size;
        this.sizeY = size;
    }

    /**
     * Rozmer na X-ovej osy v pixeloch.
     *
     * @return Šírka chlievika.
     */
    public int getSizeX() {
        return sizeX;
    }

    /**
     * Rozmer na Y-ovej osy v pixeloch.
     *
     * @return Výška chlievika.
     */
    public int getSizeY() {
        return sizeY;
    }

    /**
     * Menší rozmer z X-ovej / Y-onovej osy.
     *
     * @return Menší rozmer chlievika.
     */
    public int getSizeMin(){ return Math.min(sizeX, sizeY);}

    /**
     * Väčší rozmer z X-ovej / Y-onovej osy.
     *
     * @return Väčší rozmer chlievika.
     */
    public int getSizeMax(){ return Math.max(sizeX, sizeY);}

    /**
     * Prepočet z jednotiek mriežky na pixely.
     * Point2D.x = gridX * šírkaŠtvorčekaPX
     *
     * @param gridX X-ová súradnica na mriežke.
     * @param gridY Y-ová súradnica na mriežke.
     * @return Súradnice v pixeloch.
     */
    public Point2D gridToPixel(int gridX, int gridY) {
        return new Point2D(gridX * sizeX, gridY * sizeY);
    }

    /**
     * Podľa súradníc v pixeloch vráti súradnice na mriežke.
     *
     * @param x X-ová súrdanica v pixeloch.
     * @param y Y-ová súrdanica v pixeloch.
     * @return Súradnice na mriežke.
     */
    Point2D pixelToGrid(double x, double y) {
        return new Point2D(x / sizeX, y / sizeY);
    }

    /**
     * Vygeneruje panel s pozadím podľa nastavenej veľkosti mriežky.
     *
     * @param width      Šírka plochy v pixeloch.
     * @param height     Výška plochy v pixeloch.
     * @param bgColor    Farba pozadia.
     * @param linesColor Farba čiar mriežky.
     * @return Panel s vygenerovaným pozadím.
     */
    Pane generateBackground(double width, double height, Paint bgColor, Paint linesColor) {
        Pane bck = new Pane(new Rectangle(width, height, bgColor));

		for (int x = 0; x <= width; x += sizeX){
			bck.getChildren().add(generateGridLine(x, 0, x, height, linesColor));
		}

		for (int y = 0; y <= height; y += sizeY){
			bck.getChildren().add(generateGridLine(0, y, width, y, linesColor));
		}

		return bck;
    }

    /**
     * Generovanie čiary na mriežku pozadia.
     *
     * @param startX    Začiatočná súracnica X.
     * @param startY    Začiatočná súracnica Y.
     * @param endX      Koncová súracnica X.
     * @param endY      Koncová súracnica Y.
     * @param lineColor Farba čiary.
     * @return Vygenerovaná čiara.
     */
    private Line generateGridLine(double startX, double startY, double endX, double endY, Paint lineColor) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(lineColor);
        line.setOpacity(0.5);
        line.setStrokeWidth(1);

        return line;
    }
}