package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import sk.uniza.fri.cp.SchematicSim.Connectable;
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;
import sk.uniza.fri.cp.SchematicSim.HighlightGroup;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Sheet.SheetEvent;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Vodič spájajúci piny súčiastok. Vytvára medzi nimi potenciál. Nahrádza
 * {@code BreadboardSim.Wire.Wire} - Socket je nahradené Pin-om a odstránený je BusInterface
 * hack v {@link #updatePotential()} (žiadne reálne zbernicové čipy v tomto modeli neexistujú).
 * Ostatná logika (jointy, rozpájanie/spájanie segmentov, farbenie) je prevzatá bezo zmeny.
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia Socket -> Pin pre SchematicSim
 */
public class Wire extends HighlightGroup {

    private static Color defaultColor = Color.BLACK;

    // debug farby podľa logického stavu vodiča: Z - sivá, 0 - modrá, 1 - červená
    private static final Color DEBUG_Z_COLOR = Color.GRAY;
    private static final Color DEBUG_LOW_COLOR = Color.BLUE;
    private static final Color DEBUG_HIGH_COLOR = Color.RED;

    private Color color;
    private Potential potential;
    private boolean debugColored;
    private volatile boolean colorRefreshScheduled;

    private final Runnable debugColorListener = this::refreshDebugColors;

    private final WireEnd[] ends;
    private final List<Joint> joints;
    private final List<WireSegment> segments;

    private final Group jointsGroup;
    private final Group segmentsGroup;

    // rozpracovaný vodič začatý ťahaním priamo zo segmentu tohto vodiča
    private Wire draggedWire;

    // segment, nad ktorým je práve kurzor (ak žiadny, null)
    private WireSegment hoveredSegment;

    // segment zvolený kliknutím - zvýrazní sa len on, nie celý vodič
    private WireSegment selectedSegment;

    /** Spájače založené len pre ťahaný vodič; pri zrušení ťahu sa odstránia. */
    private final List<WireJunction> startJunctions = new ArrayList<>();

    /**
     * Spájače (hraničné body) obývané týmto vodičom - v novom modeli je spájač samostatný
     * bod pripojenia DVOCH vodičov (kmeň sa v mieste spájača rozdelí na dva vodiče).
     * Huby nie sú súčasťou {@link #joints} (tie obsahujú len ohyby), aby sa pri ukladaní
     * neukladali ako zlomy kmeňa - konce vodičov sa ukladajú s atribútom junction.
     */
    private final List<WireJunction> hubs = new ArrayList<>();

    /** Preferovaný smer prvej úsečky odbočky ťahanej zo spájača (kolmo na kmeň, smerom ku kurzoru). */
    private Side branchExit;

    private final EventHandler<MouseEvent> onMouseDragDetected = event -> {
        if (!event.isPrimaryButtonDown()) return;

        WireSegment segmentToSplit = findSegmentTarget(event.getTarget());
        if (segmentToSplit == null) return;

        startWireFromSegment(event, segmentToSplit);
        event.consume();
    };

    /**
     * Začatie nového vodiča ťahaním priamo zo segmentu existujúceho vodiča.
     * V mieste ťahu sa vytvorí spájač (WireJunction) a z neho sa vedie nový vodič.
     */
    private void startWireFromSegment(MouseEvent event, WireSegment segmentToSplit) {
        Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
        WireJunction junction = this.createJunction(sheetXY);
        if (junction == null) return;

        Wire branch = Pin.beginWireCreation(junction);
        branch.startJunctions.add(junction);
        draggedWire = branch;
        this.startFullDrag();
    }

    /**
     * Najde {@link WireSegment} v rodičovskej reťazi prekliknutého uzla (pickResult vráti
     * najhlbší prvok - ciarku {@link javafx.scene.shape.Polyline}, nie samotný segment).
     */
    private static WireSegment findSegmentTarget(Object target) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            if (node instanceof WireSegment) return (WireSegment) node;
            node = node.getParent();
        }
        return null;
    }

    /** Najde {@link Joint} (spájač/koniec vodiča) v rodičovskej reťazi prekliknutého uzla. */
    private static Joint findJointTarget(Object target) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            if (node instanceof Joint) return (Joint) node;
            node = node.getParent();
        }
        return null;
    }

    /**
     * Segment vodiča pre zvolený uzol pod kurzorom: priamy segment pod kurzorom, alebo ak je
     * kurzor nad spájačom/zlomom, jeden z jeho segmentov patriacich tomuto vodiču.
     */
    private WireSegment segmentForTarget(Object target) {
        WireSegment segment = findSegmentTarget(target);
        if (segment != null) return segment;

        Joint joint = findJointTarget(target);
        if (joint != null) {
            for (int i = 0; i < 2; i++) {
                WireSegment seg = i == 0 ? joint.getPrimaryWireSegment() : joint.getSecondaryWireSegment();
                if (seg != null && seg.getWire() == this) return seg;
            }
        }
        return null;
    }

    private final EventHandler<MouseEvent> onMouseDragged = event -> {
        if (event.isPrimaryButtonDown() && draggedWire != null) {
            Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
            draggedWire.updateBranchDrag(sheetXY.getX(), sheetXY.getY());
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> onMouseReleased = event -> {
        if (draggedWire != null) {
            draggedWire.setMouseTransparent(false);
            draggedWire.setOpacity(1);

            Wire inProgress = Pin.getInProgressWire();
            if (inProgress != null) {
                if (!inProgress.areBothEndsConnected()) {
                    inProgress.delete();
                }
                Pin.finishInProgressWire();
                if (inProgress.areBothEndsConnected()) {
                    inProgress.settleToGrid();
                }
            }
            draggedWire = null;
        }
    };

    private final EventHandler<MouseEvent> onMouseEntered = event -> {
        this.hoveredSegment = segmentForTarget(event.getTarget());
        if (!this.isSelected()) {
            if (this.hoveredSegment != null) this.highlightSegment(this.hoveredSegment, 0.7);
            else this.unhighlightSegments();
        }
        Color brighter = color.brighter();
        this.setStyle("-fx-effect: dropshadow(gaussian, rgb("
                + brighter.getRed() * 255 + "," + brighter.getGreen() * 255 + "," + brighter.getBlue() * 255 + "), 1, 1.0, 0, 0)");
    };

    private final EventHandler<MouseEvent> onMouseExited = event -> {
        if (!this.isSelected()) {
            this.hoveredSegment = null;
            this.unhighlightSegments();
        }
        this.setStyle("-fx-effect: none");
    };

    /**
     * Vytvorenie nového vodiča začínajúceho na danom pine. Voľný koniec je dostupný cez
     * {@link #catchFreeEnd()}.
     */
    public Wire(Pin startPin) {
        this.joints = new LinkedList<>();
        this.segments = new LinkedList<>();
        this.segmentsGroup = new Group();
        this.jointsGroup = new Group();
        this.color = defaultColor;

        SchematicSheet sheet = startPin.getOwner().getSheet();

        this.ends = new WireEnd[2];
        this.ends[0] = new WireEnd(sheet, this);
        this.ends[0].connect(startPin);

        this.ends[1] = new WireEnd(sheet, this);
        this.ends[1].moveTo(this.ends[0].getLayoutX(), this.ends[0].getLayoutY());

        WireSegment segment = new WireSegment(this, this.ends[0], this.ends[1]);
        this.segments.add(segment);

        this.segmentsGroup.getChildren().add(segment);
        this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);
        this.getChildren().addAll(segmentsGroup, jointsGroup);
        this.setId("wire");

        registerEvents();
    }

    /**
     * Vytvorenie voľného vodiča priamo na ploche (bez okamžitého pripojenia) - napr. pre
     * budúci nástroj "manuálne pridať vodič" v paletke. Konce sa umiestnia na [1,1] a [2,2].
     */
    public Wire(SchematicSheet sheet) {
        this.joints = new LinkedList<>();
        this.segments = new LinkedList<>();
        this.segmentsGroup = new Group();
        this.jointsGroup = new Group();
        this.color = defaultColor;

        this.ends = new WireEnd[2];
        this.ends[0] = new WireEnd(sheet, this);
        this.ends[0].moveTo(1, 1);
        this.ends[1] = new WireEnd(sheet, this);
        this.ends[1].moveTo(2, 2);

        WireSegment segment = new WireSegment(this, this.ends[0], this.ends[1]);
        this.segments.add(segment);

        this.segmentsGroup.getChildren().add(segment);
        this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);
        this.getChildren().addAll(segmentsGroup, jointsGroup);

        registerEvents();
    }

    /**
     * Vytvorenie nového vodiča začínajúceho na spájači (WireJunction) na existujúcom vodiči.
     * Voľný koniec je dostupný cez {@link #catchFreeEnd()}.
     */
    public Wire(WireJunction startJunction) {
        this.joints = new LinkedList<>();
        this.segments = new LinkedList<>();
        this.segmentsGroup = new Group();
        this.jointsGroup = new Group();
        this.color = defaultColor;

        SchematicSheet sheet = startJunction.getSheet();

        this.ends = new WireEnd[2];
        this.ends[0] = new WireEnd(sheet, this);
        this.ends[0].connect(startJunction);

        this.ends[1] = new WireEnd(sheet, this);
        this.ends[1].moveTo(this.ends[0].getLayoutX(), this.ends[0].getLayoutY());

        WireSegment segment = new WireSegment(this, this.ends[0], this.ends[1]);
        this.segments.add(segment);

        this.segmentsGroup.getChildren().add(segment);
        this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);
        this.getChildren().addAll(segmentsGroup, jointsGroup);
        this.setId("wire");

        registerEvents();
    }

    public SchematicSheet getSheet() {
        return this.ends[0].getSheet();
    }

    public static void setDefaultColor(Color defColor) {
        defaultColor = defColor;
    }

    public static Color getDefaultColor() {
        return defaultColor;
    }

    public void changeColor(Color newColor) {
        this.color = newColor;
        this.refreshSegmentColors();
        for (WireEnd end : this.ends) end.setDefaultColor();
        if (this.isSelected()) {
            if (this.selectedSegment != null) this.highlightSegment(this.selectedSegment, 1);
            else this.highlightSegments(1);
        }
    }

    public Color getColor() {
        return this.color;
    }

    /**
     * Zapnutie/vypnutie debug-farbenia vodiča podľa logického stavu potenciálu.
     * Pri prekreslení sa použije pôvodná (užívateľská) farba alebo debug-farba podľa stavu.
     */
    public void setDebugColored(boolean enabled) {
        this.debugColored = enabled;
        if (this.potential != null) {
            if (enabled) this.potential.addValueListener(this.debugColorListener);
            else this.potential.removeValueListener(this.debugColorListener);
        }
        this.refreshSegmentColors();
    }

    /**
     * Skoalescovaná obnova farieb pri zmene potenciálu - pri kontinuálnom behu simulácie
     * by každá zmena potenciálu inak vytvorila samostatnú runLater úlohu a FX vlákno by
     * nestíhalo vyprázdňovať rad. Spraví sa len najnovší stav.
     */
    private void refreshDebugColors() {
        if (!this.debugColored) return;
        if (this.colorRefreshScheduled) return;
        this.colorRefreshScheduled = true;
        Platform.runLater(() -> {
            this.colorRefreshScheduled = false;
            if (this.debugColored) this.refreshSegmentColors();
        });
    }

    private void refreshSegmentColors() {
        Color target = this.getCurrentColor();
        this.segments.forEach(segment -> segment.setColor(target));
    }

    /** Aktuálne používaná farba segmentov - debug-farba podľa stavu alebo užívateľská farba. */
    Color getCurrentColor() {
        if (!this.debugColored) return this.color;
        if (this.potential == null) return DEBUG_Z_COLOR;
        switch (this.potential.getValue()) {
            case HIGH: return DEBUG_HIGH_COLOR;
            case LOW: return DEBUG_LOW_COLOR;
            default: return DEBUG_Z_COLOR;
        }
    }

    public WireEnd[] getEnds() {
        return ends;
    }

    public List<Joint> getJoints() {
        return joints;
    }

    /**
     * Voľný koniec vodiča pri jeho prvotnom vytváraní.
     */
    public WireEnd catchFreeEnd() {
        return this.ends[1];
    }

    /**
     * Posun voľného konca rozpracovanej odbočky počas ťahania. Ak odbočka začína na spájači,
     * aktualizuje sa aj preferovaný smer prvej úsečky podľa polohy kurzora voči kmeňu vodiča.
     */
    void updateBranchDrag(double x, double y) {
        if (this.ends[0].getJunction() != null) {
            Side side = this.ends[0].getJunction().branchExitFor(x, y);
            if (side != null) this.branchExit = side;
        }
        this.catchFreeEnd().moveTo(x, y);
    }

    /** Preferovaný smer prvej úsečky odbočky (pre OrthogonalRouter pri ťahaní zo spájača). */
    Side getBranchExit() {
        return this.branchExit;
    }

    /** Spájač, z ktorého sa tento vodič začal ťahať (null pre vodič začatý na pine). */
    public WireJunction getStartJunction() {
        return this.ends[0].getJunction();
    }

    /**
     * Vytvorenie spájača (WireJunction) na danom mieste tohto vodiča. Rozdelí segment,
     * na ktorom sa miesto nachádza, a vloží spájač medzi vzniknuté segmenty.
     *
     * @param position Pozícia spájača v layoutových súradniciach.
     * @return Vytvorený spájač, alebo null ak sa nepodarilo.
     */
    public WireJunction createJunction(Point2D position) {
        WireSegment targetSegment = findSegmentAt(position);
        if (targetSegment == null) return null;

        // spájač polož presne na trasu segmentu, aby sa vodič opticky nerozdelil
        // do dvoch mierne posunutých častí (pri ťahaní z vodiča je bod mimo priamky)
        Point2D snappedPosition = snapToSegment(targetSegment, position);

        // začiatočný bod odbočky sa zarovná na mriežku: najprv na najbližší priesečník
        // mriežky, potom sa premietne späť na trasu kmeňa, aby nevybočil mimo vodiča
        // (trasa dokončeného vodiča vedie po čiarach mriežky, preto bod zostane na mriežke)
        double grid = getSheet().getGrid().getSizeMin();
        snappedPosition = new Point2D(
                Math.round(snappedPosition.getX() / grid) * grid,
                Math.round(snappedPosition.getY() / grid) * grid);
        snappedPosition = snapToSegment(targetSegment, snappedPosition);

        // ak je v blízkosti už spájač (hub), vrátime ho - netvoríme duplicitný spájač
        WireJunction existing = findJunctionNear(snappedPosition.getX(), snappedPosition.getY());
        if (existing != null) return existing;

        // spájač presne na konci vodiča nemá čo rozdeľovať
        if (snappedPosition.distance(this.ends[0].getLayoutX(), this.ends[0].getLayoutY()) < 1e-3
                || snappedPosition.distance(this.ends[1].getLayoutX(), this.ends[1].getLayoutY()) < 1e-3) {
            return null;
        }

        List<Point2D> route = fullRoute();
        if (route == null || route.size() < 2) return null;

        // rezné body: nový bod + všetky existujúce huby + legacy vnútorné spájače
        List<Point2D> rawCuts = new ArrayList<>();
        rawCuts.add(snappedPosition);
        for (WireJunction hub : this.hubs) rawCuts.add(hub.getConnectionPoint());
        for (Joint joint : this.joints) {
            if (joint instanceof WireJunction) rawCuts.add(joint.getConnectionPoint());
        }
        List<Point2D> cutsSorted = sortCutsAlongRoute(route, rawCuts);
        if (cutsSorted.isEmpty()) return null;

        // ak sú všetky rezy už rozdelené (bod pripadol na existujúci spájač), nič nerobíme
        if (cutsSorted.size() == 1) {
            WireJunction only = junctionAtPosition(cutsSorted.get(0));
            if (only != null) return only;
        }

        List<List<Point2D>> subRoutes = cutRoute(route, cutsSorted);
        if (subRoutes == null || subRoutes.size() < 2) return null;

        Connectable outerRight = connectorOf(this.ends[1]);

        // hub pre každú hranicu (0..cuts-1); existujúce objekty sa znovu použijú,
        // aby si zachovali napojené konce odbočiek
        List<WireJunction> boundaryHubs = new ArrayList<>();
        WireJunction tappedHub = null;
        for (Point2D cut : cutsSorted) {
            WireJunction boundary = junctionAtPosition(cut);
            if (boundary == null) {
                boundary = new WireJunction(getSheet(), this);
                boundary.moveTo(cut.getX(), cut.getY());
            }
            if (cut.distance(snappedPosition) < 1e-3) tappedHub = boundary;
            boundaryHubs.add(boundary);
        }

        // znovu postavíme vodič pre každý podúsek; this ostáva prvým podúsekom,
        // ďalšie podúseky dostanú vlastný objekt Wire (kmeň sa rozdelí na niekoľko vodičov)
        for (int i = 0; i < subRoutes.size(); i++) {
            List<Point2D> subRoute = subRoutes.get(i);
            WireJunction hubRight = (i < boundaryHubs.size()) ? boundaryHubs.get(i) : null;

            Wire wire;
            if (i == 0) {
                wire = this;
                wire.clearGeometry();
                if (hubRight != null) wire.ends[1].connect(hubRight);
                else wire.ends[1].connect(outerRight);
            } else {
                wire = new Wire(getSheet());
                wire.changeColor(this.color);
                getSheet().addItem(wire);
                wire.clearGeometry();
                wire.ends[0].connect(boundaryHubs.get(i - 1));
                if (hubRight != null) wire.ends[1].connect(hubRight);
                else wire.ends[1].connect(outerRight);
            }

            if (hubRight != null) wire.registerHub(hubRight);

            wire.addSegmentWithPath(wire.ends[0], wire.ends[1], subRoute);
        }

        // hraničný spájač v mieste ťahania - na ten sa napojí nová odbočka
        return tappedHub;
    }

    /**
     * Nájde segment vodiča, ktorý je najbližšie k danému bodu.
     */
    private WireSegment findSegmentAt(Point2D position) {
        double bestDist = Double.MAX_VALUE;
        WireSegment best = null;

        for (WireSegment seg : this.segments) {
            List<Double> points = seg.getRoutedPoints();
            for (int i = 0; i < points.size() - 2; i += 2) {
                double x1 = points.get(i);
                double y1 = points.get(i + 1);
                double x2 = points.get(i + 2);
                double y2 = points.get(i + 3);
                double dist = distanceToSegment(position.getX(), position.getY(), x1, y1, x2, y2);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = seg;
                }
            }
        }

        if (best != null && bestDist > 10) return null;
        return best;
    }

    /**
     * Verejný prístup k najbližšiemu segmentu v blízkosti bodu (pre načítavanie súborov).
     */
    public WireSegment findSegmentNear(Point2D position) {
        return findSegmentAt(position);
    }

    /**
     * Nájde existujúci spájač na vodiči v blízkosti daného bodu (pre načítavanie súborov).
     */
    public WireJunction findJunctionAt(Point2D position) {
        for (WireJunction hub : this.hubs) {
            if (Math.abs(hub.getLayoutX() - position.getX()) < 1
                    && Math.abs(hub.getLayoutY() - position.getY()) < 1) {
                return hub;
            }
        }
        for (Joint joint : this.joints) {
            if (joint instanceof WireJunction
                    && Math.abs(joint.getLayoutX() - position.getX()) < 1
                    && Math.abs(joint.getLayoutY() - position.getY()) < 1) {
                return (WireJunction) joint;
            }
        }
        return null;
    }

    /**
     * Nájde spájač (hub alebo legacy vnútorný spájač) v okruhu {@code radius} okolo bodu.
     */
    public WireJunction findJunctionNear(double x, double y) {
        for (WireJunction hub : this.hubs) {
            if (Math.abs(hub.getLayoutX() - x) < 8 && Math.abs(hub.getLayoutY() - y) < 8) {
                return hub;
            }
        }
        for (Joint joint : this.joints) {
            if (joint instanceof WireJunction
                    && Math.abs(joint.getLayoutX() - x) < 8
                    && Math.abs(joint.getLayoutY() - y) < 8) {
                return (WireJunction) joint;
            }
        }
        return null;
    }

    /**
     * Spájač na danej pozícii (hub, alebo legacy vnútorný spájač, ktorý sa pri nájdení
     * vyzdvihne na hub). Vráti null, ak na pozícii žiadny spájač nie je.
     */
    private WireJunction junctionAtPosition(Point2D position) {
        for (WireJunction hub : this.hubs) {
            if (hub.getConnectionPoint().distance(position) < 1e-3) return hub;
        }
        for (int i = this.joints.size() - 1; i >= 0; i--) {
            Joint joint = this.joints.get(i);
            if (joint instanceof WireJunction
                    && joint.getConnectionPoint().distance(position) < 1e-3) {
                WireJunction legacy = (WireJunction) joint;
                this.joints.remove(i);
                this.hubs.add(legacy);
                legacy.setHostWire(this);
                return legacy;
            }
        }
        return null;
    }

    /**
     * Samostatný spájač priamo na tomto vodiči (bez rozdeľovania) - používa sa pri
     * načítavaní súborov pre konce, ktoré sa na seba napájajú v inom bode.
     */
    public WireJunction createStandaloneHub(Point2D pos) {
        WireJunction hub = new WireJunction(getSheet(), this);
        hub.moveTo(pos.getX(), pos.getY());
        registerHub(hub);
        return hub;
    }

    private static double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lenSq));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.hypot(px - projX, py - projY);
    }

    /**
     * Najbližší bod na trase segmentu k danej pozícii. Trasa je zlomená čiara (orto-routing),
     * preto hľadáme projekciu na jej jednotlivé úsečky.
     */
    private static Point2D snapToSegment(WireSegment segment, Point2D position) {
        List<Double> points = segment.getRoutedPoints();
        Point2D best = position;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < points.size() - 2; i += 2) {
            double x1 = points.get(i);
            double y1 = points.get(i + 1);
            double x2 = points.get(i + 2);
            double y2 = points.get(i + 3);
            double dx = x2 - x1;
            double dy = y2 - y1;
            double lenSq = dx * dx + dy * dy;
            double t = lenSq == 0 ? 0 : Math.max(0, Math.min(1, ((position.getX() - x1) * dx + (position.getY() - y1) * dy) / lenSq));
            double px = x1 + t * dx;
            double py = y1 + t * dy;
            double dist = (position.getX() - px) * (position.getX() - px) + (position.getY() - py) * (position.getY() - py);
            if (dist < bestDist) {
                bestDist = dist;
                best = new Point2D(px, py);
            }
        }
        return best;
    }

    /**
     * Rozdelenie trasy (zlomenej čiary) v bode {@code cut}, ktorý leží na tejto trase,
     * na dve nadväzujúce časti {@code firstPath} + {@code secondPath}. Používa sa pri
     * založení spájača - nové segmenty prevezmú pôvodné úseky trasy, aby sa vodič
     * opticky nezmenil a nemusel sa prepočítavať routing.
     */
    private static void splitRoutePoints(List<Double> points, Point2D cut, List<Point2D> firstPath, List<Point2D> secondPath) {
        List<Point2D> pts = toPoint2DList(points);

        int edgeIndex = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < pts.size() - 1; i++) {
            double dist = distanceToSegment(cut.getX(), cut.getY(),
                    pts.get(i).getX(), pts.get(i).getY(),
                    pts.get(i + 1).getX(), pts.get(i + 1).getY());
            if (dist < bestDist) {
                bestDist = dist;
                edgeIndex = i;
            }
        }

        int splitVertex = edgeIndex + 1;
        boolean onVertex = cut.distance(pts.get(splitVertex)) < 1e-6;

        for (int j = 0; j < splitVertex; j++) {
            firstPath.add(pts.get(j));
        }
        firstPath.add(cut);

        secondPath.add(cut);
        for (int j = onVertex ? splitVertex + 1 : splitVertex; j < pts.size(); j++) {
            secondPath.add(pts.get(j));
        }

        dedupConsecutive(firstPath);
        dedupConsecutive(secondPath);
    }

    /**
     * Zlúčenie trás dvoch segmentov, ktoré sa stretávajú na spoločnom zlome {@code shared}.
     * Prvý segment sa preberá v smere od jeho vzdialenejšieho zlomu k spoločnému, druhý
     * od spoločného k jeho vzdialenejšiemu zlomu - výsledkom je súvislá trasa bez
     * duplicitného spoločného bodu. Routing sa pri tom neprepočítava.
     */
    private static List<Point2D> mergeRoutes(WireSegment first, WireSegment second, Joint shared) {
        List<Point2D> merged = new ArrayList<>();
        appendRoute(merged, directedRoute(first, shared, true));
        appendRoute(merged, directedRoute(second, shared, false));
        return merged;
    }

    private static void appendRoute(List<Point2D> target, List<Point2D> route) {
        for (Point2D p : route) {
            if (target.isEmpty() || !target.get(target.size() - 1).equals(p)) target.add(p);
        }
    }

    /**
     * Trasa segmentu v danom smere: od spoločného zlomu smerom von ({@code towardShared=false})
     * alebo od vzdialenejšieho zlomu k spoločnému ({@code towardShared=true}).
     */
    private static List<Point2D> directedRoute(WireSegment segment, Joint shared, boolean towardShared) {
        List<Point2D> pts = toPoint2DList(segment.getRoutedPoints());
        boolean reversed = towardShared ? segment.getStartJoint() == shared : segment.getEndJoint() == shared;
        if (reversed) Collections.reverse(pts);
        return pts;
    }

    private static List<Point2D> toPoint2DList(List<Double> flat) {
        List<Point2D> pts = new ArrayList<>(flat.size() / 2);
        for (int i = 0; i + 1 < flat.size(); i += 2) {
            pts.add(new Point2D(flat.get(i), flat.get(i + 1)));
        }
        return pts;
    }

    private static void dedupConsecutive(List<Point2D> pts) {
        Point2D prev = null;
        for (int i = 0; i < pts.size(); i++) {
            if (prev != null && prev.equals(pts.get(i))) {
                pts.remove(i);
                i--;
            }
            prev = pts.get(i);
        }
    }

    // ---------------------------------------------------------------------------
    // Rozdeľovanie / spájanie vodičov na spájačoch (nový "hub" model)
    // ---------------------------------------------------------------------------

    /**
     * Zoradenie a odduplikovanie rezov pozdĺž trasy vodiča. Rezy presne v koncových
     * bodoch sa vynechajú (spájač na konci vodiča nič nerozdeľuje).
     */
    private static List<Point2D> sortCutsAlongRoute(List<Point2D> route, List<Point2D> cuts) {
        List<Double> cum = cumulativeDist(route);
        double total = cum.get(cum.size() - 1);

        List<RouteCut> list = new ArrayList<>();
        for (Point2D cut : cuts) {
            double s = paramAlong(route, cum, cut);
            if (s < 1e-6 || s > total - 1e-6) continue;
            list.add(new RouteCut(cut, s));
        }
        list.sort(Comparator.comparingDouble(RouteCut::getParam));

        List<Point2D> result = new ArrayList<>();
        double last = -1;
        for (RouteCut cut : list) {
            if (cut.getParam() - last < 1e-3) continue;
            last = cut.getParam();
            result.add(cut.getPoint());
        }
        return result;
    }

    private static final class RouteCut {
        private final Point2D point;
        private final double param;

        RouteCut(Point2D point, double param) {
            this.point = point;
            this.param = param;
        }

        Point2D getPoint() {
            return point;
        }

        double getParam() {
            return param;
        }
    }

    private static List<Double> cumulativeDist(List<Point2D> route) {
        List<Double> cum = new ArrayList<>(route.size());
        double acc = 0;
        for (int i = 0; i < route.size(); i++) {
            if (i == 0) cum.add(0.0);
            else {
                acc += route.get(i - 1).distance(route.get(i));
                cum.add(acc);
            }
        }
        return cum;
    }

    /** Kandidátny (≈ arclength) parameter bodu pozdĺž trasy. */
    private static double paramAlong(List<Point2D> route, List<Double> cum, Point2D p) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < route.size() - 1; i++) {
            double d = distanceToSegment(p.getX(), p.getY(),
                    route.get(i).getX(), route.get(i).getY(),
                    route.get(i + 1).getX(), route.get(i + 1).getY());
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        double t = projectParam(route.get(best), route.get(best + 1), p);
        return cum.get(best) + route.get(best).distance(route.get(best + 1)) * t;
    }

    private static double projectParam(Point2D a, Point2D b, Point2D p) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return 0;
        return Math.max(0, Math.min(1, ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / lenSq));
    }

    /**
     * Rozdelenie trasy sériou rezov (zoradených po arclength). Vracia podúseky tak, že
     * prvý podúsek končí prvým rezom, druhý medzi prvým a druhým rezom atď. Geometria
     * trasy sa zachováva - NEPREPODČÍTAVA sa routing.
     */
    private static List<List<Point2D>> cutRoute(List<Point2D> pts, List<Point2D> cuts) {
        List<Point2D> remaining = new ArrayList<>(pts);
        List<List<Point2D>> result = new ArrayList<>();
        for (Point2D cut : cuts) {
            List<Point2D> first = new ArrayList<>();
            List<Point2D> second = new ArrayList<>();
            List<Double> flat = new ArrayList<>(remaining.size() * 2);
            for (Point2D p : remaining) {
                flat.add(p.getX());
                flat.add(p.getY());
            }
            splitRoutePoints(flat, cut, first, second);
            if (first.size() < 2 || second.size() < 2) return null;
            result.add(first);
            remaining = second;
        }
        if (remaining.size() < 2) return null;
        result.add(remaining);
        return result;
    }

    /**
     * Usporiadaný reťazec zlomov tohto vodiča od {@code ends[0]} po {@code ends[1]}.
     */
    private List<Joint> orderedJoints() {
        List<Joint> chain = new ArrayList<>();
        chain.add(this.ends[0]);
        Joint current = this.ends[0];
        int guard = 0;
        while (guard++ <= this.segments.size()) {
            WireSegment seg = segmentFrom(current, chain);
            if (seg == null) break;
            Joint next = seg.getOtherJoint(current);
            if (chain.contains(next)) break;
            chain.add(next);
            current = next;
        }
        return chain;
    }

    private WireSegment segmentFrom(Joint from, List<Joint> visited) {
        for (WireSegment seg : this.segments) {
            if (seg.getStartJoint() == from || seg.getEndJoint() == from) {
                Joint other = seg.getOtherJoint(from);
                if (!visited.contains(other)) return seg;
            }
        }
        return null;
    }

    private WireSegment segmentBetween(Joint a, Joint b) {
        for (WireSegment seg : this.segments) {
            if (seg.getStartJoint() == a && seg.getEndJoint() == b) return seg;
            if (seg.getStartJoint() == b && seg.getEndJoint() == a) return seg;
        }
        return null;
    }

    /**
     * Celá trasa vodiča od {@code ends[0]} po {@code ends[1]} (spojené trasy segmentov).
     * Vracia null, ak sa nepodarí prejsť neprerušene.
     */
    List<Point2D> fullRoute() {
        List<Joint> chain = orderedJoints();
        if (chain.size() < 2) return null;
        List<Point2D> pts = new ArrayList<>();
        for (int i = 0; i < chain.size() - 1; i++) {
            WireSegment seg = segmentBetween(chain.get(i), chain.get(i + 1));
            if (seg == null) return null;
            for (Point2D p : toPoint2DList(seg.getRoutedPoints())) {
                if (pts.isEmpty() || !pts.get(pts.size() - 1).equals(p)) pts.add(p);
            }
        }
        dedupConsecutive(pts);
        return pts;
    }

    /** Cieľ pripojenia konca vodiča (pin, spájač, alebo null pre voľný koniec). */
    private static Connectable connectorOf(WireEnd end) {
        if (end.getPin() != null) return end.getPin();
        return end.getJunction();
    }

    private static WireEnd otherEnd(Wire w, WireEnd end) {
        return w.getEnds()[0] == end ? w.getEnds()[1] : w.getEnds()[0];
    }

    private static WireEnd endAtJunction(Wire w, WireJunction junction) {
        for (WireEnd end : w.getEnds()) {
            if (end.getJunction() == junction) return end;
        }
        return null;
    }

    /** Trasa vodiča od konca {@code hubEnd} (ktorý je na spájači) smerom k druhému koncu. */
    private static List<Point2D> routeFrom(Wire w, WireEnd hubEnd) {
        List<Point2D> route = w.fullRoute();
        if (route == null) return new ArrayList<>();
        if (hubEnd == w.getEnds()[1]) Collections.reverse(route);
        return route;
    }

    /**
     * Vyčistenie geometrie vodiča (segenty, ohyby aj legacy zlomy) - zachovajú sa len
     * konce {@code ends} a huby. Používa sa pri rekonštrukcii vodiča po rozdeľovaní/spájaní.
     */
    void clearGeometry() {
        for (WireSegment seg : new ArrayList<>(this.segments)) {
            seg.getStartJoint().removeWireSegment(seg);
            seg.getEndJoint().removeWireSegment(seg);
        }
        this.segments.clear();
        this.joints.clear();
        this.segmentsGroup.getChildren().clear();

        this.jointsGroup.getChildren().clear();
        this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);

        // zvýraznenia a výber odkazujú na staré segmenty, ktoré sa práve rušia - odstránime ich,
        // aby po rekonštrukcii vodiča (createJunction) nezostala visieť biela čiarkovaná čiara
        // nad novým vodičom
        this.unhighlightSegments();
        this.selectedSegment = null;
        this.hoveredSegment = null;
    }

    /**
     * Pridanie segmentov vodiča pre celú trasu {@code path}. Vnútorné body trasy dostanú
     * vlastný zlom (Joint), aby sa geometria vodiča zachovala pri ukladaní a aby sa dali
     * ťahať/zmazať aj ohyby. Trasa sa NEPREPOČÍTAVÁ cez routing.
     */
    void addSegmentWithPath(Joint left, Joint right, List<Point2D> path) {
        if (path.size() < 2) return;
        if (path.size() == 2) {
            this.addSegmentWithPathOnly(left, right, path);
            return;
        }

        Joint prev = left;
        int pieceStart = 0;
        for (int i = 1; i + 1 < path.size(); i++) {
            Point2D point = path.get(i);
            // kolineárne vnútorné body (napr. miesto zaniknutého spájača po zlúčení vodičov)
            // sa nestanú zlomom - zostanú len priebežnými bodmi trasy bez sivého krúžku
            if (isCollinearWithNeighbours(path.get(i - 1), point, path.get(i + 1))) {
                continue;
            }
            Joint bend = new Joint(getSheet(), this);
            bend.moveTo(point.getX(), point.getY());
            bend.setJointDotVisible(false);
            this.joints.add(bend);
            this.jointsGroup.getChildren().add(bend);

            this.addSegmentWithPathOnly(prev, bend, path.subList(pieceStart, i + 1));
            prev = bend;
            pieceStart = i;
        }
        this.addSegmentWithPathOnly(prev, right, path.subList(pieceStart, path.size()));
    }

    /** Je bod {@code b} na priamke spájajúcej body {@code a} a {@code c}? */
    private static boolean isCollinearWithNeighbours(Point2D a, Point2D b, Point2D c) {
        double cross = (b.getX() - a.getX()) * (c.getY() - b.getY())
                - (b.getY() - a.getY()) * (c.getX() - b.getX());
        return Math.abs(cross) < 1e-6;
    }

    private void addSegmentWithPathOnly(Joint left, Joint right, List<Point2D> path) {
        WireSegment segment = new WireSegment(this, left, right, path);
        this.segments.add(segment);
        this.segmentsGroup.getChildren().add(segment);
    }

    /**
     * Registrácia spájača (hub) do tohto vodiča. Spájač sa vykresľuje v samostatnej vrstve
     * NAVRCHU schémy (nie v {@code jointsGroup}), aby nebol prekrytý neskôr pridanými vodičmi
     * a dal sa uchopiť myšou. Vodič spájač iba "obýva" (zodpovedá zaň) cez {@code hubs}.
     */
    void registerHub(WireJunction hub) {
        if (hub == null) return;
        if (!this.hubs.contains(hub)) this.hubs.add(hub);
        hub.setHostWire(this);
        Pane junctionsLayer = getSheet().getJunctionsLayer();
        if (hub.getParent() != junctionsLayer) {
            junctionsLayer.getChildren().add(hub);
        }
    }

    /**
     * Vodiče, ktoré sú aktuálne napojené na daný spájač (bez koncov tohto vodiča).
     */
    private static List<Wire> attachedWiresOf(WireJunction junction) {
        List<Wire> wires = new ArrayList<>();
        for (WireEnd end : new ArrayList<>(junction.getConnectedEnds())) {
            Wire wire = end.getWire();
            if (wire != null && !wires.contains(wire)) wires.add(wire);
        }
        return wires;
    }

    private void resolveDeletedHub(WireJunction hub) {
        if (hub == null || hub.isRemoved()) return;
        List<Wire> wires = attachedWiresOf(hub);
        if (wires.size() == 2) {
            Wire[] pair = straightPair(hub, wires);
            if (pair != null) {
                mergeWiresAtJunction(hub, pair[0], pair[1]);
            } else {
                destroyJunction(hub);
            }
        } else if (wires.size() <= 1) {
            destroyJunction(hub);
        }
        // 3+ vodičov: spájač zostáva (je stále potrebný ako odbočný bod)
    }

    /**
     * Dvojica vodičov na spájači, ktoré cez spájač pokračujú najpriamejšie (kmeň). Zvyšné
     * vodiče na spájači sú odbočky - ich konce sa pri zániku spájača uvoľnia. Pri presne
     * dvoch vodičoch vráti túto dvojicu (nezáleží na geometrii).
     */
    private static Wire[] straightPair(WireJunction junction, List<Wire> wires) {
        if (wires.size() < 2) return null;
        Wire bestA = null;
        Wire bestB = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int a = 0; a < wires.size(); a++) {
            Point2D dirA = unitDirectionFrom(wires.get(a), junction);
            for (int b = a + 1; b < wires.size(); b++) {
                Point2D dirB = unitDirectionFrom(wires.get(b), junction);
                double score = dirA.getX() * dirB.getX() + dirA.getY() * dirB.getY();
                if (score < bestScore) {
                    bestScore = score;
                    bestA = wires.get(a);
                    bestB = wires.get(b);
                }
            }
        }
        return bestA == null ? null : new Wire[]{bestA, bestB};
    }

    /** Jednotkový smer, ktorým vodič vychádza zo spájača (od spájača k druhému koncu). */
    private static Point2D unitDirectionFrom(Wire wire, WireJunction junction) {
        WireEnd end = endAtJunction(wire, junction);
        if (end == null) return new Point2D(0, 0);
        List<Point2D> route = routeFrom(wire, end);
        if (route.size() < 2) return new Point2D(0, 0);
        Point2D d = route.get(1).subtract(route.get(0));
        double len = d.magnitude();
        if (len < 1e-9) return new Point2D(0, 0);
        return new Point2D(d.getX() / len, d.getY() / len);
    }

    /**
     * Zlúčenie dvoch vodičov, ktoré ostali po odstránení tretieho na jednom spájači: vznikne
     * jediný vodič prechádzajúci cez bod spájača (bez prerušený). Spájač sa odstráni.
     */
    private void mergeWiresAtJunction(WireJunction junction, Wire wa, Wire wb) {
        WireEnd ea = endAtJunction(wa, junction);
        WireEnd eb = endAtJunction(wb, junction);
        if (ea == null || eb == null) return;

        Connectable targetA = connectorOf(otherEnd(wa, ea));
        Connectable targetB = connectorOf(otherEnd(wb, eb));

        List<Point2D> ra = routeFrom(wa, ea);
        List<Point2D> rb = routeFrom(wb, eb);
        Collections.reverse(ra);

        List<Point2D> merged = new ArrayList<>();
        appendRoute(merged, ra);
        appendRoute(merged, rb);
        dedupConsecutive(merged);

        // najprv uvoľníme piny/spájače pôvodných vodičov (aby sa na ne mohol pripojiť nový),
        // až potom postavíme zlúčený vodič
        Color color = wa.getColor();
        SchematicSheet sheet = wa.getSheet();
        detachWire(wa);
        detachWire(wb);
        destroyJunction(junction);

        Wire mergedWire = buildWireFromRoute(sheet, targetA, targetB, merged, color);
        mergedWire.updatePotential();
    }

    /**
     * Zlúčenie dvoch vodičov po odstránení vodiča medzi DVOCH rôznych spájačov: cesta
     * zvyšných vodičov sa spojí cez zapamätanú trasu odstráneného vodiča (spájače preč).
     */
    private void mergeAcrossHubs(WireJunction h1, WireJunction h2, List<Point2D> middleRoute) {
        WireEnd ea = firstEndAt(h1);
        WireEnd eb = firstEndAt(h2);
        if (ea == null || eb == null) return;
        Wire wa = ea.getWire();
        Wire wb = eb.getWire();
        if (wa == null || wb == null || wa == wb) return;

        Connectable targetA = connectorOf(otherEnd(wa, ea));
        Connectable targetB = connectorOf(otherEnd(wb, eb));

        List<Point2D> ra = routeFrom(wa, ea);
        Collections.reverse(ra);

        List<Point2D> merged = new ArrayList<>();
        appendRoute(merged, ra);
        appendRoute(merged, middleRoute);
        List<Point2D> rb = routeFrom(wb, eb);
        appendRoute(merged, rb);
        dedupConsecutive(merged);

        Color color = wa.getColor();
        SchematicSheet sheet = wa.getSheet();
        detachWire(wa);
        detachWire(wb);
        destroyJunction(h1);
        destroyJunction(h2);

        Wire mergedWire = buildWireFromRoute(sheet, targetA, targetB, merged, color);
        mergedWire.updatePotential();
    }

    private static WireEnd firstEndAt(WireJunction junction) {
        for (WireEnd end : new ArrayList<>(junction.getConnectedEnds())) {
            if (end.getWire() != null) return end;
        }
        return null;
    }

    /**
     * Vytvorenie úplne nového vodiča z trasy a pripojenie koncov na cieľové body
     * (piny alebo spájače; null = voľný koniec umiestnený na konce trasy).
     */
    private static Wire buildWireFromRoute(SchematicSheet sheet, Connectable targetA, Connectable targetB,
                                           List<Point2D> route, Color color) {
        Wire wire = new Wire(sheet);
        wire.changeColor(color);
        sheet.addItem(wire);
        wire.clearGeometry();

        WireEnd e0 = wire.getEnds()[0];
        WireEnd e1 = wire.getEnds()[1];
        if (targetA != null) e0.connect(targetA);
        else e0.moveTo(route.get(0).getX(), route.get(0).getY());
        if (targetB != null) e1.connect(targetB);
        else e1.moveTo(route.get(route.size() - 1).getX(), route.get(route.size() - 1).getY());

        wire.addSegmentWithPath(e0, e1, route);
        return wire;
    }

    /**
     * Odpojenie vodiča zo všetkých náväzností (piny aj spájače) a odstránenie z plochy.
     * Na rozdiel od {@link #delete()} nespúšťa zlučovanie okolitých vodičov.
     */
    private static void detachWire(Wire wire) {
        for (WireEnd end : wire.getEnds()) {
            if (end.getPin() != null) {
                end.disconnect();
            } else if (end.getJunction() != null) {
                end.detachFromJunction();
            }
        }
        wire.getSheet().removeItem(wire);
    }

    /**
     * Úplné zničenie spájača: odstránenie node zo scény, vyradenie z hubov hostiteľa
     * a odpojenie všetkých ešte pripojených koncov (bez kaskádovej rekurzie).
     */
    void destroyJunction(WireJunction junction) {
        if (junction == null || junction.isRemoved()) return;
        junction.markRemoved();

        if (junction.getParent() instanceof Pane) {
            ((Pane) junction.getParent()).getChildren().remove(junction);
        }
        for (Wire wire : new ArrayList<>(attachedWiresOf(junction))) {
            wire.hubs.remove(junction);
        }
        Wire host = junction.getHostWire();
        if (host != null) host.hubs.remove(junction);

        for (WireEnd end : new ArrayList<>(junction.getConnectedEnds())) {
            end.detachFromJunction();
        }
    }

    /**
     * Nájde segment, ktorý spája daný joint s týmto vodičom - pre WireEnd.connect(WireJunction).
     */
    WireSegment getLastSegmentTo(Joint target) {
        for (WireSegment seg : this.segments) {
            if (seg.getStartJoint() == target || seg.getEndJoint() == target) {
                return seg;
            }
        }
        return null;
    }

    /**
     * Odstránenie zlomu na vodiči. Odstrániť je možné iba vnútorné zlomy, nie konce.
     */
    public void removeJoint(Joint joint) {
        if (joint instanceof WireEnd) return;

        WireSegment firstSegment = joint.getPrimaryWireSegment();
        WireSegment secondSegment = joint.getSecondaryWireSegment();

        Joint firstJoint = firstSegment.getOtherJoint(joint);
        Joint secondJoint = secondSegment.getOtherJoint(joint);

        // zlúčené segmenty prevezmú spojené pôvodné trasy, aby sa vodič po zmazaní
        // zlomu opticky nezmenil a nemusel sa prepočítavať routing
        List<Point2D> mergedRoute = mergeRoutes(firstSegment, secondSegment, joint);

        firstJoint.removeWireSegment(firstSegment);
        secondJoint.removeWireSegment(secondSegment);

        WireSegment newSegment = new WireSegment(this, firstJoint, secondJoint, mergedRoute);
        this.segments.add(newSegment);
        this.segmentsGroup.getChildren().add(newSegment);

        this.segments.remove(firstSegment);
        this.segments.remove(secondSegment);
        this.joints.remove(joint);

        this.segmentsGroup.getChildren().removeAll(firstSegment, secondSegment);
        this.jointsGroup.getChildren().remove(joint);
    }

    /**
     * Odstránenie spájača (WireJunction) - volá sa pri jeho zmazaní (napr. double-click).
     * Ak na spájači zostávajú presne DVA vodiče, zlúčia sa do jedného (spájač sa odstráni);
     * inak sa spájač bezpečne zničí a pripojené konce sa odpoja.
     */
    void removeJunction(WireJunction junction) {
        if (junction == null || junction.isRemoved()) return;

        List<Wire> wires = attachedWiresOf(junction);
        Wire[] pair = straightPair(junction, wires);
        if (pair != null) {
            mergeWiresAtJunction(junction, pair[0], pair[1]);
        } else {
            destroyJunction(junction);
        }
    }

    public boolean areBothEndsConnected() {
        return this.ends[0].isConnected() && this.ends[1].isConnected();
    }

    /**
     * Prepočítanie trás všetkých segmentov po dokončení vodiča - zlomy sa tým zarovnajú
     * na mriežku (pri ťahaní sa zámerne nezarovnávajú, aby pohyb vodiča ostal plynulý).
     */
    public void settleToGrid() {
        for (WireSegment segment : this.segments) {
            segment.updateGraphics();
        }
    }

    public Joint splitLastSegment() {
        return this.splitSegment(((LinkedList<WireSegment>) this.segments).getLast());
    }

    private Joint splitSegment(WireSegment segment) {
        Joint newJoint = new Joint(getSheet(), this);

        Joint firstJoint = segment.getStartJoint();
        Joint secondJoint = segment.getEndJoint();

        newJoint.moveTo(firstJoint.getLayoutX(), firstJoint.getLayoutY());

        firstJoint.removeWireSegment(segment);
        secondJoint.removeWireSegment(segment);

        WireSegment firstSegment = new WireSegment(this, firstJoint, newJoint);
        WireSegment secondSegment = new WireSegment(this, newJoint, secondJoint);

        this.segments.add(firstSegment);
        this.segments.add(secondSegment);

        int fjIndex = this.joints.indexOf(firstJoint);
        this.joints.add(fjIndex + 1, newJoint);

        this.segmentsGroup.getChildren().addAll(firstSegment, secondSegment);
        this.jointsGroup.getChildren().add(newJoint);

        this.segments.remove(segment);
        this.segmentsGroup.getChildren().remove(segment);

        return newJoint;
    }

    private void registerEvents() {
        this.addEventHandler(MouseEvent.DRAG_DETECTED, onMouseDragDetected);
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleased);
        this.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, onMouseEntered);
        this.addEventHandler(MouseEvent.MOUSE_EXITED, onMouseExited);
    }

    private double firstDeltaX, firstDeltaY;
    private WireEnd lastMovingEnd;

    /**
     * Posunutie zlomov o deltu podľa zmeny polohy jedného z koncov vodiča. Ak sa pohnú oba
     * konce o rovnakú vzdialenosť (napr. presun celého prepojenia spolu s oboma súčiastkami),
     * posunú sa aj zlomy.
     */
    void moveJointsWithEnd(WireEnd wireEnd, double deltaX, double deltaY) {
        if (this.lastMovingEnd != wireEnd) {
            if (Math.abs(firstDeltaX - deltaX) < 0.5 && Math.abs(firstDeltaY - deltaY) < 0.5) {
                this.joints.forEach(joint -> joint.moveBy(deltaX, deltaY));
                this.firstDeltaX = 0;
                this.firstDeltaY = 0;
                return;
            }
        }
        this.firstDeltaX = deltaX;
        this.firstDeltaY = deltaY;
        this.lastMovingEnd = wireEnd;
    }

    /**
     * Prepočet potenciálu vodiča podľa aktuálne pripojených pinov na jeho koncoch.
     * Podporuje pripojenie cez spájače (WireJunction) - nájde pin aj cez reťaz zlomov.
     */
    void updatePotential() {
        if (this.potential != null) {
            this.potential.removeValueListener(this.debugColorListener);
            this.potential.delete();
            this.potential = null;
        }

        Pin toUpdate;
        if (this.ends[0] != null && this.ends[1] != null) {
            Pin start = getEndPin(this.ends[0]);
            Pin end = getEndPin(this.ends[1]);

            if (start != null && end != null) {
                this.potential = new Potential(start, end);
                if (this.debugColored) this.potential.addValueListener(this.debugColorListener);
                toUpdate = start;
            } else {
                toUpdate = start != null ? start : end;
            }

            if (toUpdate != null && getSheet().simRunningProperty().getValue()) {
                getSheet().addEvent(new SheetEvent(toUpdate));
            }
        }
        this.refreshSegmentColors();
    }

    /**
     * Nájde pin, na ktorý je koniec vodiča napojený (priamo alebo cez spájač).
     */
    private static Pin getEndPin(WireEnd end) {
        if (end.getPin() != null) return end.getPin();
        if (end.getJunction() != null) return end.getJunction().findConnectedPin();
        return null;
    }

    @Override
    public void delete() {
        super.delete();

        if (this.getParent() == null) return; // už odstránený (napr. po zlúčení susedov)

        // spájače, na ktoré bol tento vodič napojený - po odpojení sa z nich môžu spájať
        // zvyšné vodiče (ak na spájači ostanú presne dva, zlúčia sa do jedného)
        List<WireJunction> endJunctions = new ArrayList<>();
        for (WireEnd end : this.ends) {
            WireJunction junction = end.getJunction();
            if (junction != null && !endJunctions.contains(junction)) endJunctions.add(junction);
        }

        // mazaný vodič medzi dvomi rôznymi spájačmi - zapamätáme si jeho trasu, aby sa
        // po ňom mohli spojiť dva okolité vodiče do jednej priamky
        List<Point2D> throughRoute = null;
        if (endJunctions.size() == 2 && endJunctions.get(0) != endJunctions.get(1)) {
            throughRoute = fullRoute();
        }

        for (WireEnd end : this.ends) {
            end.disconnect();
        }
        this.startJunctions.clear();

        // legacy vnútorné spájače (staré súbory) - odpojíme ich konce; uzly zmiznú s vodičom
        for (Joint joint : new ArrayList<>(this.joints)) {
            if (joint instanceof WireJunction) {
                WireJunction legacy = (WireJunction) joint;
                for (WireEnd end : new ArrayList<>(legacy.getConnectedEnds())) {
                    end.detachFromJunction();
                }
                legacy.markRemoved();
            }
        }

        this.getSheet().removeItem(this);

        if (endJunctions.isEmpty()) return;

        if (endJunctions.size() == 2 && endJunctions.get(0) != endJunctions.get(1)) {
            WireJunction h1 = endJunctions.get(0);
            WireJunction h2 = endJunctions.get(1);
            List<Wire> s1 = attachedWiresOf(h1);
            List<Wire> s2 = attachedWiresOf(h2);
            if (s1.size() == 1 && s2.size() == 1 && s1.get(0) != s2.get(0) && throughRoute != null) {
                mergeAcrossHubs(h1, h2, throughRoute);
                return;
            }
            resolveDeletedHub(h1);
            resolveDeletedHub(h2);
        } else {
            resolveDeletedHub(endJunctions.get(0));
        }
    }

    @Override
    public void select() {
        super.select();
        if (!this.isSelectable()) return;
        if (this.hoveredSegment != null) {
            this.selectedSegment = this.hoveredSegment;
            this.highlightSegment(this.selectedSegment, 1);
        } else {
            this.highlightSegments(1);
        }
    }

    @Override
    public void deselect() {
        super.deselect();
        this.selectedSegment = null;
        this.unhighlightSegments();
    }

    private final ArrayList<Shape> selectionShapes = new ArrayList<>();

    private void highlightSegments(double opacity) {
        this.unhighlightSegments();

        this.segments.forEach(segment -> {
            javafx.scene.shape.Polyline highlight = new javafx.scene.shape.Polyline();
            highlight.getPoints().setAll(segment.getRoutedPoints());
            for (double value : STROKE_DASH_ARRAY) highlight.getStrokeDashArray().add(value);
            highlight.setStrokeWidth(1.5);
            highlight.setStroke(this.getColor().invert());
            highlight.setStrokeLineCap(StrokeLineCap.ROUND);
            highlight.setOpacity(opacity);
            highlight.setMouseTransparent(true);

            this.selectionShapes.add(highlight);
            this.getChildren().add(highlight);
        });
    }

    /**
     * Zvýraznenie iba jedného segmentu vodiča - používa sa pri výbere kliknutím aj pri
     * premýšaní kurzora, aby sa nezvýrazňoval celý vodič, ale len segment pod kurzorom.
     */
    private void highlightSegment(WireSegment segment, double opacity) {
        this.unhighlightSegments();

        javafx.scene.shape.Polyline highlight = new javafx.scene.shape.Polyline();
        highlight.getPoints().setAll(segment.getRoutedPoints());
        for (double value : STROKE_DASH_ARRAY) highlight.getStrokeDashArray().add(value);
        highlight.setStrokeWidth(1.5);
        highlight.setStroke(this.getColor().invert());
        highlight.setStrokeLineCap(StrokeLineCap.ROUND);
        highlight.setOpacity(opacity);
        highlight.setMouseTransparent(true);

        this.selectionShapes.add(highlight);
        this.getChildren().add(highlight);
    }

    private void unhighlightSegments() {
        this.getChildren().removeAll(this.selectionShapes);
        this.selectionShapes.clear();
    }

    @Override
    public Pane getDescription() {
        Pane cached = super.getDescription();
        if (cached == null) {
            VBox wrapper = new VBox();
            wrapper.setAlignment(Pos.CENTER);

            ColorPicker colorPicker = new ColorPicker(getColor());
            colorPicker.setOnAction(event -> this.changeColor(colorPicker.getValue()));

            wrapper.getChildren().add(colorPicker);
            this.cacheDescription(wrapper);
            return wrapper;
        }
        return cached;
    }
}
