package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.GridOccupancy;
import sk.uniza.fri.cp.SchematicSim.GridSystem;
import sk.uniza.fri.cp.SchematicSim.Item;
import sk.uniza.fri.cp.SchematicSim.Selectable;
import sk.uniza.fri.cp.SchematicSim.Wire.Joint;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;
import sk.uniza.fri.cp.SchematicSim.Wire.WireEnd;
import sk.uniza.fri.cp.SchematicSim.Wire.WireJunction;
import sk.uniza.fri.cp.SchematicSim.Wire.WireSegment;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plocha simulátora - schematický editor. Nahrádza {@code Board} z BreadboardSim.
 * <p>
 * Kľúčové rozdiely oproti originálu:
 * <ul>
 *     <li>Na začiatku je plocha PRÁZDNA - žiadna počiatočná vývojová doska.</li>
 *     <li>Umiestňovanie súčiastok kontroluje voľnosť buniek mriežky cez {@link GridOccupancy}
 *     namiesto kolíznej detekcie pin↔soket.</li>
 *     <li>Zoom/pan mechanizmus je prispôsobený: pri zome zostáva bod plochy pod kurzorom
 *     na mieste (pôvodné riešenie z Board nastavovalo posun skôr, než sa prepočítal rozsah scrollbaru).</li>
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

    /** Schránka pre kopírovanie/prilepenie vybratých súčiastok a vodičov (Ctrl+C / Ctrl+V). */
    private List<CopiedGate> clipboardGates = new ArrayList<>();
    private List<CopiedWire> clipboardWires = new ArrayList<>();
    private int clipboardBaseX;
    private int clipboardBaseY;

    /** Prebiehajúce označovanie obdĺžnikom (Shift + ťah ľavým tlačidlom) - kotva v súradniciach plochy. */
    private Rectangle rubberBand;
    private double rubberStartX;
    private double rubberStartY;

    /** Posledná poloha myši v scénových súradniciach - kotva pre prilepenie (Ctrl+V). */
    private double lastMouseSceneX = -1;
    private double lastMouseSceneY = -1;

    private boolean hasChanged = false;

    /** Povolenie editácie schémy (pridávanie/mazanie/presun vodičov a súčiastok). */
    private final SimpleBooleanProperty editingEnabled = new SimpleBooleanProperty(true);

    /** Debug-farbenie vodičov podľa logického stavu (Z sivá, 0 modrá, 1 červená). */
    private final SimpleBooleanProperty debugWires = new SimpleBooleanProperty(false);

    private static final double SCALE_DELTA = 1.1;
    private final SimpleDoubleProperty scaleTotal = new SimpleDoubleProperty(1);
    private final Group contentGroup;

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
            if (event.getButton() == MouseButton.PRIMARY && !event.isShiftDown()) clearSelect();
        });

        // označovanie súčiastok a vodičov obdĺžnikom: Shift + stlačenie ľavého tlačidla na
        // voľnej ploche spustí tah, pri ťahu sa kreslí čiarkovaný obdĺžnik a po pustení sa
        // všetky objekty pretínajúce obdĺžnik pridajú k výberu (udržiava sa doterajší výber)
        gridBackground.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || !event.isShiftDown()) return;
            if (!isEditingEnabled()) return;
            Point2D local = gridBackground.sceneToLocal(event.getSceneX(), event.getSceneY());
            rubberStartX = local.getX();
            rubberStartY = local.getY();
            rubberBand = new Rectangle(0, 0, 0, 0);
            rubberBand.setFill(null);
            rubberBand.setStroke(Color.BLACK);
            rubberBand.getStrokeDashArray().addAll(4.0, 4.0);
            rubberBand.setStrokeWidth(1.5);
            rubberBand.setMouseTransparent(true);
            gridBackground.getChildren().add(rubberBand);
            event.consume();
        });

        gridBackground.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (rubberBand == null) return;
            Point2D local = gridBackground.sceneToLocal(event.getSceneX(), event.getSceneY());
            rubberBand.setLayoutX(Math.min(rubberStartX, local.getX()));
            rubberBand.setLayoutY(Math.min(rubberStartY, local.getY()));
            rubberBand.setWidth(Math.abs(local.getX() - rubberStartX));
            rubberBand.setHeight(Math.abs(local.getY() - rubberStartY));
            event.consume();
        });

        gridBackground.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (rubberBand == null) return;
            Rectangle band = rubberBand;
            rubberBand = null;
            gridBackground.getChildren().remove(band);
            if (event.getButton() != MouseButton.PRIMARY || !event.isShiftDown()) return;

            Bounds bandBounds = band.getBoundsInParent();
            for (GateSymbol gate : layersManager.getGates()) {
                if (gate.getBoundsInParent().intersects(bandBounds)) addSelect(gate);
            }
            for (Wire wire : layersManager.getWires()) {
                if (wire.getBoundsInParent().intersects(bandBounds)) addSelect(wire);
            }
            event.consume();
        });

        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, onMouseDragEnteredHandle);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, onMouseDragOverHandle);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleasedHandle);
        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED, onMouseDragExitedHandle);

        // ZOOM/PAN
        this.setPannable(false);
        //scrollbar-y sa skryjú: ScrollPane dostáva obsah presne vo veľkosti viewportu, takže
        //skin nemá čo scrollovať ani centrovať - celý posun schémy (pan aj zoom kotva) vedie
        //výhradne cez zoomPane.translateX/Y, ktoré skin nikdy neprepisuje.
        this.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        this.contentGroup = this.layersManager.getLayers();
        final StackPane zoomPane = new StackPane(contentGroup);
        //TOP_LEFT - StackPane by default CENTROVAL obsah a centrovacia odchýlka sa so zmenou
        //stupnice menila (polovica zmeny rozmeru obsahu), čo pri vyššom priblížení posúvalo
        //schému doprava dole. Kotva obsahu je teraz pevne v (0,0) zoomPane.
        zoomPane.setAlignment(Pos.TOP_LEFT);
        final Pane scrollContent = new Pane(zoomPane);
        this.setContent(scrollContent);

        this.viewportBoundsProperty().addListener((ChangeListener<Bounds>) (obs, oldV, newV) -> {
            scrollContent.setMinSize(newV.getWidth(), newV.getHeight());
            scrollContent.setPrefSize(newV.getWidth(), newV.getHeight());
            scrollContent.setMaxSize(newV.getWidth(), newV.getHeight());
            zoomPane.setMinSize(newV.getWidth(), newV.getHeight());
        });

        zoomPane.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
            double newScale = scaleTotal.doubleValue() * scaleFactor;
            if (newScale < 0.3 || newScale > 3) return;

            //kotva zoomu = bod plochy pod kurzorom (v súradniciach mriežky, pri starej stupnici)
            double mouseSceneX = event.getSceneX();
            double mouseSceneY = event.getSceneY();
            Point2D anchor = contentGroup.sceneToLocal(mouseSceneX, mouseSceneY);

            contentGroup.setScaleX(newScale);
            contentGroup.setScaleY(newScale);
            scaleTotal.setValue(newScale);

            //posun oneskorene (až po najbližšom layout cykle, keď majú scénové transformácie
            //aktuálne rozmery). Kotva sa meria priamo zo scénovej polohy bodu mriežky
            //(contentGroup.localToScene) a presunie sa cez zoomPane.translateX/Y: meraný posun je
            //z definície presný, skin ho nikdy neprepisuje a nezávisí od rozsahu scrollbaru ani
            //od žiadneho centrovania - bod pod kurzorom zostáva fixný pri VŠETKÝCH úrovniach.
            Platform.runLater(() -> {
                scrollContent.applyCss();
                scrollContent.layout();

                Point2D anchorNow = contentGroup.localToScene(anchor);
                //kotva sa presunie o nameranú odchýlku (merané priamo v scéne - presné,
                //nezávislé od rozsahu scrollbaru ani od centrovania skinu)
                zoomPane.setTranslateX(zoomPane.getTranslateX() + (mouseSceneX - anchorNow.getX()));
                zoomPane.setTranslateY(zoomPane.getTranslateY() + (mouseSceneY - anchorNow.getY()));
                //po korekcii sa obsah vráti späť do viewportu (pri najmenších priblíženiach by
                //holá korekcia inak posunula plochu mimo - sivé pásy alebo celé zmiznutie)
                enforceContentInView(zoomPane);
            });
        });

        final javafx.beans.property.ObjectProperty<Point2D> lastMouseCoordinates = new javafx.beans.property.SimpleObjectProperty<>();
        scrollContent.setOnMousePressed(event -> lastMouseCoordinates.set(new Point2D(event.getX(), event.getY())));

        scrollContent.setOnMouseDragged(event -> {
            //delta proti poslednej polohe (nie proti bodu stlačenia) - inak by sa aplikoval
            //celý ťah od stlačenia pri KAŽDOM drag evente a plocha by utekala a menila smer
            double deltaX = event.getX() - lastMouseCoordinates.get().getX();
            double deltaY = event.getY() - lastMouseCoordinates.get().getY();
            lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
            zoomPane.setTranslateX(zoomPane.getTranslateX() + deltaX);
            zoomPane.setTranslateY(zoomPane.getTranslateY() + deltaY);
            enforceContentInView(zoomPane);
        });

        setupWireToWireHandler();

        // kotva pre prilepenie (Ctrl+V) = posledná poloha myši na ploche
        scrollContent.setOnMouseMoved(event -> {
            lastMouseSceneX = event.getSceneX();
            lastMouseSceneY = event.getSceneY();
        });

        // Ctrl+C kopíruje a Ctrl+V vkladá vybraté súčiastky (okno schémy nemá textové polia,
        // takže skratky nijako nekolidujú s úpravou textu)
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN), this::copySelection);
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN), this::pasteClipboard);
            }
        });

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

    /**
     * Po korekcii zoomu/panu obsah neskĺzne mimo viewport: poloha počiatku obsahu
     * (meraná priamo zo scény - contentGroup.localToScene(0,0)) sa priclampe do rozsahu
     * {@code [-(obsah-viewport), 0]}, takže plocha viewport vždy zakrýva. Ak je obsah menší
     * ako viewport, ukotví sa vľavo hore. Ak korekcia dokáže byť v rozsahu bez posunu,
     * nič sa nemení - kotva pod kurzorom tak zostáva exaktná a na okrajoch (najmä pri
     * najmenších priblíženiach) sa plocha nevymkne mimo.
     */
    private void enforceContentInView(StackPane zoomPane) {
        double cw = contentGroup.getLayoutBounds().getWidth() * scaleTotal.doubleValue();
        double ch = contentGroup.getLayoutBounds().getHeight() * scaleTotal.doubleValue();
        Bounds vp = this.localToScene(this.getViewportBounds());
        Point2D o = contentGroup.localToScene(0, 0);

        double excessX = cw - vp.getWidth();
        double oRelX = o.getX() - vp.getMinX();
        double targetRelX = oRelX;
        if (excessX < 0) {
            targetRelX = 0;
        } else if (oRelX > 0) {
            targetRelX = 0;
        } else if (oRelX < -excessX) {
            targetRelX = -excessX;
        }

        double excessY = ch - vp.getHeight();
        double oRelY = o.getY() - vp.getMinY();
        double targetRelY = oRelY;
        if (excessY < 0) {
            targetRelY = 0;
        } else if (oRelY > 0) {
            targetRelY = 0;
        } else if (oRelY < -excessY) {
            targetRelY = -excessY;
        }

        zoomPane.setTranslateX(zoomPane.getTranslateX() + (targetRelX - oRelX));
        zoomPane.setTranslateY(zoomPane.getTranslateY() + (targetRelY - oRelY));
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

    public List<GateSymbol> getSelectedGates() {
        List<GateSymbol> result = new ArrayList<>();
        for (Selectable selectable : selected) {
            if (selectable instanceof GateSymbol) result.add((GateSymbol) selectable);
        }
        return result;
    }

    /**
     * Kopírovanie vybratých súčiastok a vodičov do schránky (Ctrl+C). Ukladá sa snímok typu,
     * vlastností a vzájomných gridových offsetov súčiastok - prilepenie (Ctrl+V) ich vloží
     * rovnako rozložené, len posunuté na miesto pod kurzorom. Kopírujú sa LEN označené
     * vodiče, a to iba tie, ktorých obidva konce sú na kopírovaných súčiastkach.
     */
    public void copySelection() {
        if (!isEditingEnabled()) return;
        List<GateSymbol> gates = getSelectedGates();
        List<Wire> wires = new ArrayList<>();
        for (Selectable selectable : selected) {
            if (selectable instanceof Wire) wires.add((Wire) selectable);
        }
        if (gates.isEmpty() && wires.isEmpty()) return;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (GateSymbol gate : gates) {
            minX = Math.min(minX, gate.getGridPosX());
            minY = Math.min(minY, gate.getGridPosY());
        }
        clipboardBaseX = minX;
        clipboardBaseY = minY;

        clipboardGates = new ArrayList<>();
        for (GateSymbol gate : gates) {
            clipboardGates.add(new CopiedGate(gate, gate.saveProperties(),
                    gate.getGridPosX() - minX, gate.getGridPosY() - minY));
        }

        clipboardWires = new ArrayList<>();
        for (Wire wire : wires) {
            Pin startPin = wire.getEnds()[0].getPin();
            Pin endPin = wire.getEnds()[1].getPin();
            if (startPin == null || endPin == null) continue;
            if (!isCopiedGate(startPin.getOwner()) || !isCopiedGate(endPin.getOwner())) continue;

            List<Point2D> joints = new ArrayList<>();
            for (Joint joint : wire.getJoints()) {
                joints.add(new Point2D(joint.getLayoutX(), joint.getLayoutY()));
            }
            clipboardWires.add(new CopiedWire(wire, wire.getColor(), joints));
        }
    }

    private boolean isCopiedGate(GateSymbol gate) {
        for (CopiedGate copied : clipboardGates) {
            if (copied.source == gate) return true;
        }
        return false;
    }

    /**
     * Prilepenie súčiastok zo schránky (Ctrl+V) na poslednú pozíciu myši na mriežke.
     * Nové súčiastky sa po prilepení označia (výber sa presunie na ne), vodiče sa
     * pripoja na zodpovedajúce piny klonov.
     */
    public void pasteClipboard() {
        if (!isEditingEnabled()) return;
        if (clipboardGates.isEmpty() && clipboardWires.isEmpty()) return;

        Point2D mouseOnGrid = lastMouseToGrid();
        int targetX = (int) Math.round(mouseOnGrid.getX());
        int targetY = (int) Math.round(mouseOnGrid.getY());

        Map<GateSymbol, GateSymbol> cloneBySource = new HashMap<>();
        clearSelect();
        for (CopiedGate copied : clipboardGates) {
            int gridX = targetX + copied.relX;
            int gridY = targetY + copied.relY;

            GateSymbol clone;
            try {
                clone = copied.type.getConstructor(SchematicSheet.class).newInstance(this);
            } catch (InstantiationException | IllegalAccessException
                    | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                continue;
            }

            // ak je miesto obsadené, pokúsime sa posunúť kópiu diagonálne dolu-doprava
            int attempts = 0;
            while (!occupancy.isFree(gridX, gridY, clone.getGridWidth(), clone.getGridHeight())
                    && attempts < 100) {
                gridX++;
                gridY++;
                attempts++;
            }

            addItem(clone);
            clone.moveTo(gridX, gridY);
            clone.loadProperties(copied.properties);
            occupancy.occupy(clone);
            addSelect(clone);
            cloneBySource.put(copied.source, clone);
        }

        double deltaPx = (targetX - clipboardBaseX) * gridSystem.getSizeX();
        double deltaPy = (targetY - clipboardBaseY) * gridSystem.getSizeY();

        for (CopiedWire copied : clipboardWires) {
            Pin startPin = clonePin(copied.source.getEnds()[0].getPin(), cloneBySource);
            Pin endPin = clonePin(copied.source.getEnds()[1].getPin(), cloneBySource);
            if (startPin == null || endPin == null) continue;

            Wire wire = new Wire(this);
            addItem(wire);
            wire.changeColor(copied.color);
            wire.getEnds()[0].connect(startPin);
            wire.getEnds()[1].connect(endPin);
            for (Point2D joint : copied.joints) {
                wire.splitLastSegment().moveTo(joint.getX() + deltaPx, joint.getY() + deltaPy);
            }
        }
    }

    /** Pin s rovnakým indexom na klone súčiastky (null, ak pôvodný pin nemá klon). */
    private Pin clonePin(Pin sourcePin, Map<GateSymbol, GateSymbol> cloneBySource) {
        if (sourcePin == null) return null;
        GateSymbol clone = cloneBySource.get(sourcePin.getOwner());
        if (clone == null) return null;
        int index = sourcePin.getOwner().getPins().indexOf(sourcePin);
        if (index < 0 || index >= clone.getPins().size()) return null;
        return clone.getPins().get(index);
    }

    /** Súradnice poslednej polohy myši prepočítané na mriežku (pri neznámej polohe 2,2). */
    private Point2D lastMouseToGrid() {
        if (lastMouseSceneX < 0 || lastMouseSceneY < 0) {
            return new Point2D(2, 2);
        }
        Point2D local = layersManager.getLayer("background")
                .sceneToLocal(lastMouseSceneX, lastMouseSceneY);
        return gridSystem.pixelToGrid(local.getX(), local.getY());
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

    /**
     * Snímka jednej kopírovanej súčiastky - referencie na originál (pre mapovanie pinov
     * vodičov), vlastnosti (pre obnovu cez loadProperties) a gridový offset voči ľavému
     * hornému rohu celej skupiny vybraných súčiastok.
     */
    private static final class CopiedGate {
        final GateSymbol source;
        final Class<? extends GateSymbol> type;
        final Map<String, String> properties;
        final int relX;
        final int relY;

        CopiedGate(GateSymbol source, Map<String, String> properties, int relX, int relY) {
            this.source = source;
            this.type = source.getClass();
            this.properties = properties;
            this.relX = relX;
            this.relY = relY;
        }
    }

    /**
     * Snímka jedného kopírovaného vodiča - referencie na originál, farba a pozície
     * zlomov (v súradniciach plochy, offsetované pri prilepení rovnakým posunom ako súčiastky).
     */
    private static final class CopiedWire {
        final Wire source;
        final Color color;
        final List<Point2D> joints;

        CopiedWire(Wire source, Color color, List<Point2D> joints) {
            this.source = source;
            this.color = color;
            this.joints = joints;
        }
    }
}
