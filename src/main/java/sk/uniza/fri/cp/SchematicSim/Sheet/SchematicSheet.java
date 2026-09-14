package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.GridOccupancy;
import sk.uniza.fri.cp.SchematicSim.GridSystem;
import sk.uniza.fri.cp.SchematicSim.Item;
import sk.uniza.fri.cp.SchematicSim.Selectable;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;
import sk.uniza.fri.cp.SchematicSim.Wire.WireEnd;
import sk.uniza.fri.cp.SchematicSim.Wire.WireJunction;
import sk.uniza.fri.cp.SchematicSim.Wire.WireSegment;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
    private GridOccupancy occupancy;

    private final ArrayList<Selectable> selected;
    private GateSymbol addingItem;

    private boolean hasChanged = false;

    /** Povolenie editácie schémy (pridávanie/mazanie/presun vodičov a súčiastok). */
    private final SimpleBooleanProperty editingEnabled = new SimpleBooleanProperty(true);

    /** Debug-farbenie vodičov podľa logického stavu (Z sivá, 0 modrá, 1 červená). */
    private final SimpleBooleanProperty debugWires = new SimpleBooleanProperty(false);

    private static final double SCALE_DELTA = 1.1;
    private final SimpleDoubleProperty scaleTotal = new SimpleDoubleProperty(1);

    private final EventHandler<MouseDragEvent> onMouseDragEnteredHandle = event -> {
        if (!isEditingEnabled()) return;
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

                //kotva zoomu = poloha kurzora vo viewporte, počítaná v scénovom priestore
                //(odolná voči rozdielnym súradnicovým systémom obsahu a ScrollPane)
                Bounds viewport = this.getViewportBounds();
                Point2D viewportOrigin = this.localToScene(viewport.getMinX(), viewport.getMinY());
                double anchorX = event.getSceneX() - viewportOrigin.getX();
                double anchorY = event.getSceneY() - viewportOrigin.getY();

                double oldContentWidth = scrollContent.getLayoutBounds().getWidth();
                double oldContentHeight = scrollContent.getLayoutBounds().getHeight();

                contentGroup.setScaleX(contentGroup.getScaleX() * scaleFactor);
                contentGroup.setScaleY(contentGroup.getScaleY() * scaleFactor);
                scaleTotal.setValue(scaleTotal.doubleValue() * scaleFactor);

                //novú veľkosť obsahu dopočítame explicitne - layoutBounds ešte nie je prepočítaný
                double extraWidth = oldContentWidth * scaleFactor - viewport.getWidth();
                double extraHeight = oldContentHeight * scaleFactor - viewport.getHeight();
                repositionScroller(scrollContent, this, scaleFactor, scrollOffset, anchorX, anchorY, extraWidth, extraHeight);
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

        setupWireToWireHandler();

        this.debugWires.addListener((obs, oldValue, newValue) -> {
            for (Wire wire : layersManager.getWires()) wire.setDebugColored(newValue);
        });
    }

    /**
     * Nastavenie spracovania udalostí pre pripájanie vodičov na iné vodiče.
     * Zachytáva uvoľnenie myši na úrovni scény, aby sa detekovalo, keď užívateľ
     * pustí vodič na segmente iného vodiča - vytvorí sa spájač (WireJunction).
     */
    private void setupWireToWireHandler() {
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                    Wire inProgress = Pin.getInProgressWire();
                    if (inProgress == null) return;

                    // pickResult vráti najhlbší hite (cierka spájača / polyline segmentu),
                    // preto prechádzame rodičovacou reťazou až na WireJunction/WireSegment.
                    // Koniec odbočky (WireEnd) s napojeným spájačom sa tiež berie ako spájač.
                    WireSegment segmentTarget = null;
                    WireJunction junctionTarget = null;
                    Node target = event.getPickResult() == null ? null : event.getPickResult().getIntersectedNode();
                    while (target != null && segmentTarget == null && junctionTarget == null) {
                        if (target instanceof WireSegment) {
                            segmentTarget = (WireSegment) target;
                        } else if (target instanceof WireJunction) {
                            junctionTarget = (WireJunction) target;
                        } else if (target instanceof WireEnd) {
                            junctionTarget = ((WireEnd) target).getJunction();
                        }
                        target = target.getParent();
                    }

                    if (segmentTarget != null) {
                        Wire targetWire = segmentTarget.getWire();
                        if (targetWire == inProgress) {
                            // pustenie na vlastný segment = pustenie "do prázdna" - ukončíme bez pripojenia
                            if (!inProgress.areBothEndsConnected()) {
                                inProgress.delete();
                            }
                            Pin.finishInProgressWire();
                            event.consume();
                            return;
                        }

                        Point2D sheetXY = layersManager.getLayer("background")
                                .sceneToLocal(event.getSceneX(), event.getSceneY());
                        WireJunction junction = connectToWireAt(targetWire, sheetXY.getX(), sheetXY.getY());
                        if (junction != null) {
                            if (junction == inProgress.getStartJunction()) {
                                // pustenie späť na vlastný počiatočný spájač = zrušenie ťahu
                                if (!inProgress.areBothEndsConnected()) {
                                    inProgress.delete();
                                }
                                Pin.finishInProgressWire();
                                event.consume();
                                return;
                            }
                            inProgress.catchFreeEnd().connect(junction);
                            finishWireDrop(inProgress);
                            event.consume();
                        }
                    } else if (junctionTarget != null) {
                        WireJunction junction = junctionTarget;
                        if (junction.getWire() == inProgress) {
                            if (!inProgress.areBothEndsConnected()) {
                                inProgress.delete();
                            }
                            Pin.finishInProgressWire();
                            event.consume();
                            return;
                        }
                        if (junction == inProgress.getStartJunction()) {
                            if (!inProgress.areBothEndsConnected()) {
                                inProgress.delete();
                            }
                            Pin.finishInProgressWire();
                            event.consume();
                            return;
                        }
                        inProgress.catchFreeEnd().connect(junction);
                        finishWireDrop(inProgress);
                        event.consume();
                    }
                });
            }
        });
    }

    private void finishWireDrop(Wire inProgress) {
        inProgress.setMouseTransparent(false);
        inProgress.setOpacity(1);
        Pin.finishInProgressWire();
    }

    /**
     * Vytvorí spájač na najbližšom segmente vodiča ku danej pozícii a pripojí naň nový vodič.
     * Ak sa v blízkosti už spájač nachádza, vráti ten (predchádza sa duplicitným spájačom).
     */
    private WireJunction connectToWireAt(Wire targetWire, double sheetX, double sheetY) {
        if (targetWire == null) return null;

        WireJunction near = findJunctionNear(targetWire, sheetX, sheetY);
        if (near != null) return near;

        GridSystem grid = gridSystem;
        int gridX = (int) Math.round(sheetX / grid.getSizeX());
        int gridY = (int) Math.round(sheetY / grid.getSizeY());
        Point2D snapPos = grid.gridToPixel(gridX, gridY);

        return targetWire.createJunction(snapPos);
    }

    private WireJunction findJunctionNear(Wire wire, double x, double y) {
        return wire.findJunctionNear(x, y);
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

    /**
     * Vrstva spájačov (WireJunction) - na vrchu všetkých vrstiev, aby boli čierne body
     * vždy viditeľné a uchopiteľné myšou aj cez neskôr pridané vodiče.
     */
    public Pane getJunctionsLayer() {
        return layersManager.getJunctionsLayer();
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

    public boolean addSelect(Selectable item) {
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
    }

    public void deleteSelect() {
        if (!isEditingEnabled()) return;
        new ArrayList<>(selected).forEach(Selectable::delete);
    }

    public boolean addItem(Object item) {
        hasChanged = true;
        if (item instanceof Wire) {
            ((Wire) item).setDebugColored(this.debugWires.get());
        }
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

    public boolean isEditingEnabled() {
        return editingEnabled.get();
    }

    public void setEditingEnabled(boolean enabled) {
        editingEnabled.set(enabled);
    }

    public ReadOnlyBooleanProperty editingEnabledProperty() {
        return editingEnabled;
    }

    public ReadOnlyBooleanProperty simRunningProperty() {
        return simulator.runningProperty();
    }

    public SchematicSimulator getSimulator() {
        return simulator;
    }

    /**
     * Všetky súčiastky aktuálne umiestnené na ploche.
     */
    public List<GateSymbol> getGates() {
        return layersManager.getGates();
    }

    /**
     * Všetky vodiče aktuálne na ploche.
     */
    public List<Wire> getWires() {
        return layersManager.getWires();
    }

    public void addEvent(SheetEvent event) {
        simulator.addEvent(event);
    }

    public boolean isDebugWires() {
        return debugWires.get();
    }

    public void setDebugWires(boolean debug) {
        debugWires.set(debug);
    }

    public BooleanProperty debugWiresProperty() {
        return debugWires;
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

    private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset, double anchorX, double anchorY, double extraWidth, double extraHeight) {
        double scrollXOffset = scrollOffset.getX();
        double scrollYOffset = scrollOffset.getY();
        if (extraWidth > 0) {
            double newScrollXOffset = (scaleFactor - 1) * anchorX + scaleFactor * scrollXOffset;
            scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
        if (extraHeight > 0) {
            double newScrollYOffset = (scaleFactor - 1) * anchorY + scaleFactor * scrollYOffset;
            scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
        } else {
            scroller.setVvalue(scroller.getVmin());
        }
    }
}
