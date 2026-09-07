package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import sk.uniza.fri.cp.SchematicSim.DescriptionPane;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.GridOccupancy;
import sk.uniza.fri.cp.SchematicSim.GridSystem;
import sk.uniza.fri.cp.SchematicSim.Item;
import sk.uniza.fri.cp.SchematicSim.Selectable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Plocha simulátora - schematický editor. Nahrádza {@code Board} z BreadboardSim.
 * <p>
 * Kľúčové rozdiely oproti originálu:
 * <ul>
 *     <li>Na začiatku je plocha PRÁZDNA - žiadna počiatočná vývojová doska.</li>
 *     <li>Umiestňovanie súčiastok kontroluje voľnosť buniek mriežky cez {@link GridOccupancy}
 *     namiesto kolíznej detekcie pin↔soket.</li>
 *     <li>Zoom/pan mechanizmus je prevzatý bezo zmeny - je nezávislý od breadboardu.</li>
 * </ul>
 *
 * @author Tomáš Hianik (pôvodný autor Board), adaptácia pre SchematicSim
 */
public class SchematicSheet extends ScrollPane {

    private final double widthPx;
    private final double heightPx;

    private final SchematicSimulator simulator;
    private final GridSystem gridSystem;
    private final SheetLayersManager layersManager;
    private final GridOccupancy occupancy;
    private DescriptionPane descriptionPane;

    private final ArrayList<Selectable> selected;
    private GateSymbol addingItem;

    private boolean hasChanged = false;

    private static final double SCALE_DELTA = 1.1;
    private final SimpleDoubleProperty scaleTotal = new SimpleDoubleProperty(1);

    private final EventHandler<MouseDragEvent> onMouseDragEnteredHandle = event -> {
        hasChanged = true;
        if (addingItem == null && event.getGestureSource() instanceof Item) {
            Item item = (Item) event.getGestureSource();
            SchematicSheet sheet = (SchematicSheet) event.getSource();

            try {
                addingItem = (GateSymbol) item.getClass().getConstructor(SchematicSheet.class).newInstance(sheet);
                addItem(addingItem);
                Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                        true, true, true, true, true,
                        true, true, true, true, null));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    };

    private final EventHandler<MouseDragEvent> onMouseDragOverHandle = event -> {
        if (addingItem != null) {
            Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                    event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                    true, true, true, true, true,
                    true, true, true, true, null));
        }
    };

    private final EventHandler<MouseDragEvent> onMouseDragReleasedHandle = event -> {
        if (addingItem != null) {
            // kontrola voľnosti mriežky - nahrádza pôvodnú kolíznu detekciu so soketmi breadboardu
            if (!occupancy.isFree(addingItem.getGridPosX(), addingItem.getGridPosY(),
                    addingItem.getGridWidth(), addingItem.getGridHeight())) {
                addingItem.delete();
            } else {
                occupancy.occupy(addingItem);
                Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_RELEASED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                        true, true, true, true, true,
                        true, true, true, true, null));
            }
            addingItem = null;
        }
    };

    private final EventHandler<MouseDragEvent> onMouseDragExitedHandle = event -> {
        if (addingItem != null) {
            addingItem.delete();
            addingItem = null;
        }
        event.consume();
    };

    public SchematicSheet(double width, double height, int gridSizePx) {
        this.widthPx = width;
        this.heightPx = height;
        this.selected = new ArrayList<>();
        this.simulator = new SchematicSimulator();
        this.gridSystem = new GridSystem(gridSizePx);
        this.occupancy = new GridOccupancy();

        Pane gridBackground = gridSystem.generateBackground(this.widthPx, this.heightPx, Color.WHITE, Color.LIGHTGRAY);
        this.layersManager = new SheetLayersManager(gridBackground);

        // POZOR: narozdiel od Board tu nevzniká žiadna počiatočná SchoolBreadboard - plocha je prázdna

        gridBackground.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) clearSelect();
        });

        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, onMouseDragEnteredHandle);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, onMouseDragOverHandle);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleasedHandle);
        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED, onMouseDragExitedHandle);

        // ZOOM/PAN - prevzaté bezo zmeny z Board, nezávislé od breadboardu
        this.setPannable(false);

        final Group contentGroup = this.layersManager.getLayers();
        final StackPane zoomPane = new StackPane(contentGroup);
        final Group scrollContent = new Group(zoomPane);
        this.setContent(scrollContent);

        this.viewportBoundsProperty().addListener((ChangeListener<Bounds>) (obs, oldV, newV) ->
                zoomPane.setMinSize(newV.getWidth(), newV.getHeight()));

        zoomPane.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;

            if (scaleFactor * scaleTotal.get() >= 0.3 && scaleFactor * scaleTotal.get() <= 3) {
                Point2D scrollOffset = figureScrollOffset(scrollContent, this);
                contentGroup.setScaleX(contentGroup.getScaleX() * scaleFactor);
                contentGroup.setScaleY(contentGroup.getScaleY() * scaleFactor);
                scaleTotal.setValue(scaleTotal.doubleValue() * scaleFactor);
                repositionScroller(scrollContent, this, scaleFactor, scrollOffset);
            }
        });

        final javafx.beans.property.ObjectProperty<Point2D> lastMouseCoordinates = new javafx.beans.property.SimpleObjectProperty<>();
        scrollContent.setOnMousePressed(event -> lastMouseCoordinates.set(new Point2D(event.getX(), event.getY())));

        scrollContent.setOnMouseDragged(event -> {
            double deltaX = event.getX() - lastMouseCoordinates.get().getX();
            double extraWidth = scrollContent.getLayoutBounds().getWidth() - this.getViewportBounds().getWidth();
            double deltaH = deltaX * (this.getHmax() - this.getHmin()) / extraWidth;
            this.setHvalue(Math.max(0, Math.min(this.getHmax(), this.getHvalue() - deltaH)));

            double deltaY = event.getY() - lastMouseCoordinates.get().getY();
            double extraHeight = scrollContent.getLayoutBounds().getHeight() - this.getViewportBounds().getHeight();
            double deltaV = deltaY * (this.getHmax() - this.getHmin()) / extraHeight;
            this.setVvalue(Math.max(0, Math.min(this.getVmax(), this.getVvalue() - deltaV)));
        });
    }

    public double getAppliedScale() {
        return scaleTotal.getValue();
    }

    public SimpleDoubleProperty zoomScaleProperty() {
        return scaleTotal;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public void clearChange() {
        this.hasChanged = false;
    }

    public GridSystem getGrid() {
        return gridSystem;
    }

    public GridOccupancy getOccupancy() {
        return occupancy;
    }

    public double getWidthPx() {
        return widthPx;
    }

    public double getHeightPx() {
        return heightPx;
    }

    /**
     * Prepočet zo scénových súradníc na súradnice plochy (nahrádza pôvodné Board.sceneToBoard).
     */
    public Point2D sceneToSheet(double sceneX, double sceneY) {
        return layersManager.getLayer("background").sceneToLocal(sceneX, sceneY);
    }

    public double getOriginSceneOffsetX() {
        return layersManager.getLayer("background").getLocalToSceneTransform().getTx();
    }

    public double getOriginSceneOffsetY() {
        return layersManager.getLayer("background").getLocalToSceneTransform().getTy();
    }

    public void setDescriptionPane(DescriptionPane descriptionPane) {
        this.descriptionPane = descriptionPane;
    }

    public boolean addSelect(Selectable item) {
        if (selected.isEmpty() && descriptionPane != null) descriptionPane.setDescription(item);

        if (!selected.contains(item)) {
            item.select();
            return selected.add(item);
        }
        return false;
    }

    public boolean removeSelect(Selectable item) {
        item.deselect();
        return selected.remove(item);
    }

    public void clearSelect() {
        selected.forEach(Selectable::deselect);
        selected.clear();
        if (descriptionPane != null) descriptionPane.clear();
    }

    public void deleteSelect() {
        new ArrayList<>(selected).forEach(Selectable::delete);
    }

    public boolean addItem(Object item) {
        hasChanged = true;
        return layersManager.add(item);
    }

    public boolean removeItem(Object item) {
        hasChanged = true;
        if (item instanceof GateSymbol) occupancy.free((GateSymbol) item);
        return layersManager.remove(item);
    }

    public void clearSheet() {
        this.layersManager.clear();
        this.occupancy.clear();
    }

    /**
     * Zapnutie simulácie. Nahrádza pôvodné Board.powerOn - namiesto zberu PowerSocket-ov
     * (abstraktné hradlá nemajú napájanie) sa jednoducho odovzdajú všetky súčiastky na ploche.
     */
    public void powerOn() {
        if (!simulator.runningProperty().getValue()) {
            simulator.start(layersManager.getGates());
        }
    }

    public void powerOff() {
        simulator.stop();
    }

    public boolean isSimulationRunning() {
        return simRunningProperty().getValue();
    }

    public ReadOnlyBooleanProperty simRunningProperty() {
        return simulator.runningProperty();
    }

    public SchematicSimulator getSimulator() {
        return simulator;
    }

    public void addEvent(SheetEvent event) {
        simulator.addEvent(event);
    }

    public Point2D getMousePositionOnGrid(MouseEvent event) {
        Point2D local = layersManager.getLayer("background").sceneToLocal(event.getSceneX(), event.getSceneY());
        return gridSystem.pixelToGrid(local.getX(), local.getY());
    }

    // ZOOM - prevzaté bezo zmeny z Board
    private Point2D figureScrollOffset(Node scrollContent, ScrollPane scroller) {
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
        double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
        double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
        return new Point2D(scrollXOffset, scrollYOffset);
    }

    private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset) {
        double scrollXOffset = scrollOffset.getX();
        double scrollYOffset = scrollOffset.getY();
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        if (extraWidth > 0) {
            double halfWidth = scroller.getViewportBounds().getWidth() / 2;
            double newScrollXOffset = (scaleFactor - 1) * halfWidth + scaleFactor * scrollXOffset;
            scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        if (extraHeight > 0) {
            double halfHeight = scroller.getViewportBounds().getHeight() / 2;
            double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
            scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
        } else {
            scroller.setVvalue(scroller.getVmin());
        }
    }
}
