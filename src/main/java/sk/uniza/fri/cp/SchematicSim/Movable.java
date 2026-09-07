package sk.uniza.fri.cp.SchematicSim;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Wire.Joint;

/**
 * Objekt s ktorým je možné pohybovať pomocou kurzora na ploche schémy, so snapovaním na mriežku.
 * Prevzaté takmer bezo zmeny z BreadboardSim.Movable - tento mechanizmus je úplne nezávislý
 * od breadboardu, mení sa iba typ plochy (Board -> SchematicSheet).
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public abstract class Movable extends HighlightGroup {

    private int gridPosX;
    private int gridPosY;
    private SchematicSheet sheet;

    private double nodeOffsetX = -1;
    private double nodeOffsetY = -1;

    private final EventHandler<MouseEvent> onMousePressedEventHandler = event -> {
        if (!event.isPrimaryButtonDown()) return;
        nodeOffsetX = event.getSceneX() - getLayoutX() * sheet.getAppliedScale();
        nodeOffsetY = event.getSceneY() - getLayoutY() * sheet.getAppliedScale();
        event.consume();
    };

    private final EventHandler<MouseEvent> onMouseDraggedEventHandler = event -> {
        if (!event.isPrimaryButtonDown()) return;

        setCursor(Cursor.DEFAULT);

        if (nodeOffsetX == -1) {
            Point2D layout = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
            Bounds boundsInParent = getBoundsInParent();
            nodeOffsetX = event.getSceneX() - (layout.getX() - boundsInParent.getWidth() / 2.0 - boundsInParent.getMinX()) * sheet.getAppliedScale();
            nodeOffsetY = event.getSceneY() - (layout.getY() - boundsInParent.getHeight() / 2.0 - boundsInParent.getMinY()) * sheet.getAppliedScale();
        }

        GridSystem grid = sheet.getGrid();
        int gridX;
        int gridY;

        if (event.getSource() instanceof Joint) {
            // jointy (zlomy vodiča) chytáme za stred
            Point2D sheetXY = sheet.sceneToSheet(event.getSceneX(), event.getSceneY());
            gridX = (int) (Math.round(sheetXY.getX() / grid.getSizeX()) * grid.getSizeX()) / grid.getSizeX();
            gridY = (int) (Math.round(sheetXY.getY() / grid.getSizeY()) * grid.getSizeY()) / grid.getSizeY();
        } else {
            gridX = (int) (Math.round((event.getSceneX() - nodeOffsetX) / grid.getSizeX() / sheet.getAppliedScale()) * grid.getSizeX()) / grid.getSizeX();
            gridY = (int) (Math.round((event.getSceneY() - nodeOffsetY) / grid.getSizeY() / sheet.getAppliedScale()) * grid.getSizeY()) / grid.getSizeY();
        }

        if (gridPosX != gridX || gridPosY != gridY) {
            if (gridX < 0) gridX = 0;
            else if (gridX * grid.getSizeX() + getBoundsInParent().getWidth() > sheet.getWidthPx())
                gridX = (int) (Math.round(sheet.getWidthPx() - getBoundsInParent().getWidth()) / grid.getSizeX());

            if (gridY < 0) gridY = 0;
            else if (gridY * grid.getSizeY() + getBoundsInParent().getHeight() > sheet.getHeightPx())
                gridY = (int) (Math.round(sheet.getHeightPx() - getBoundsInParent().getHeight()) / grid.getSizeY());

            moveTo(gridX, gridY);

            gridPosX = gridX;
            gridPosY = gridY;
        }

        event.consume();
    };

    public Movable(SchematicSheet sheet) {
        this.sheet = sheet;
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
    }

    public void makeImmovable() {
        this.removeEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
        this.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
    }

    public void moveBy(double deltaX, double deltaY) {
        this.gridPosX += Math.round(deltaX) / sheet.getGrid().getSizeX();
        this.gridPosY += Math.round(deltaY) / sheet.getGrid().getSizeY();
        this.setLayoutX(getLayoutX() + deltaX);
        this.setLayoutY(getLayoutY() + deltaY);
    }

    public void moveBy(int deltaX, int deltaY) {
        this.gridPosX += deltaX;
        this.gridPosY += deltaY;
        Point2D point = sheet.getGrid().gridToPixel(this.gridPosX, this.gridPosY);
        this.relocate(point.getX(), point.getY());
    }

    public void moveTo(Point2D point) {
        this.relocate(point.getX(), point.getY());
    }

    public void moveTo(double posX, double posY) {
        this.setLayoutX(posX);
        this.setLayoutY(posY);
    }

    public void moveTo(int gridPosX, int gridPosY) {
        this.gridPosX = gridPosX;
        this.gridPosY = gridPosY;

        Point2D point = getSheet().getGrid().gridToPixel(gridPosX, gridPosY);
        this.setLayoutX(point.getX());
        this.setLayoutY(point.getY());
    }

    public SchematicSheet getSheet() {
        return this.sheet;
    }

    public int getGridPosX() {
        return gridPosX;
    }

    public int getGridPosY() {
        return gridPosY;
    }

    public void setGridPos(int gridPosX, int gridPosY) {
        this.gridPosX = gridPosX;
        this.gridPosY = gridPosY;
    }

    @Override
    public void delete() {
        super.delete();
        this.getSheet().removeItem(this);
    }
}
