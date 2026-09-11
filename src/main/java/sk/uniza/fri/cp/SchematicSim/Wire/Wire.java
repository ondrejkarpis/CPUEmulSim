package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.application.Platform;
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
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;
import sk.uniza.fri.cp.SchematicSim.HighlightGroup;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Sheet.SheetEvent;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
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

    private Color color;
    private Potential potential;
    private volatile boolean labelUpdateScheduled;

    // pri kontinuálnom behu simulácie by každá zmena potenciálu vytvorila samostatnú runLater
    // úlohu a FX vlákno by nestíhalo vyprázdňovať rad -> aplikácia (napr. klávesa F10) by nereagovala.
    // Zmeny preto skoalescujeme do jednej čakajúcej úlohy - vykoná sa len najnovší stav.
    private final Runnable potentialValueListener = () -> scheduleStateLabelRefresh();

    private void scheduleStateLabelRefresh() {
        if (labelUpdateScheduled) return;
        labelUpdateScheduled = true;
        Platform.runLater(() -> {
            labelUpdateScheduled = false;
            refreshStateLabel();
        });
    }

    private final WireEnd[] ends;
    private final List<Joint> joints;
    private final List<WireSegment> segments;

    private final Group jointsGroup;
    private final Group segmentsGroup;

    // rozpracovaný vodič začatý ťahaním priamo zo segmentu tohto vodiča
    private Wire draggedWire;

    /** Spájače založené len pre ťahaný vodič; pri zrušení ťahu sa odstránia. */
    private final List<WireJunction> startJunctions = new ArrayList<>();

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
            }
            draggedWire = null;
        }
    };

    private final EventHandler<MouseEvent> onMouseEntered = event -> {
        if (!this.isSelected()) this.highlightSegments(0.7);
        Color brighter = color.brighter();
        this.setStyle("-fx-effect: dropshadow(gaussian, rgb("
                + brighter.getRed() * 255 + "," + brighter.getGreen() * 255 + "," + brighter.getBlue() * 255 + "), 1, 1.0, 0, 0)");
    };

    private final EventHandler<MouseEvent> onMouseExited = event -> {
        if (!this.isSelected()) this.unhighlightSegments();
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
        this.segments.forEach(segment -> segment.setColor(this.color));
        for (WireEnd end : this.ends) end.setDefaultColor();
        if (this.isSelected()) this.highlightSegments(1);
    }

    public Color getColor() {
        return this.color;
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

        WireJunction junction = new WireJunction(getSheet(), this);

        Joint firstJoint = targetSegment.getStartJoint();
        Joint secondJoint = targetSegment.getEndJoint();

        junction.moveTo(snappedPosition.getX(), snappedPosition.getY());

        firstJoint.removeWireSegment(targetSegment);
        secondJoint.removeWireSegment(targetSegment);

        WireSegment firstSegment = new WireSegment(this, firstJoint, junction);
        WireSegment secondSegment = new WireSegment(this, junction, secondJoint);

        this.segments.add(firstSegment);
        this.segments.add(secondSegment);

        int fjIndex = this.joints.indexOf(firstJoint);
        this.joints.add(fjIndex + 1, junction);

        this.segmentsGroup.getChildren().addAll(firstSegment, secondSegment);
        this.jointsGroup.getChildren().add(junction);

        this.segments.remove(targetSegment);
        this.segmentsGroup.getChildren().remove(targetSegment);

        return junction;
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
        for (Joint joint : this.joints) {
            if (joint instanceof WireJunction
                    && Math.abs(joint.getLayoutX() - position.getX()) < 1
                    && Math.abs(joint.getLayoutY() - position.getY()) < 1) {
                return (WireJunction) joint;
            }
        }
        return null;
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

        firstJoint.removeWireSegment(firstSegment);
        secondJoint.removeWireSegment(secondSegment);

        WireSegment newSegment = new WireSegment(this, firstJoint, secondJoint);
        this.segments.add(newSegment);
        this.segmentsGroup.getChildren().add(newSegment);

        this.segments.remove(firstSegment);
        this.segments.remove(secondSegment);
        this.joints.remove(joint);

        this.segmentsGroup.getChildren().removeAll(firstSegment, secondSegment);
        this.jointsGroup.getChildren().remove(joint);
    }

    /**
     * Odstránenie spájača (WireJunction) z vodiča. Segmenty patriace tomuto vodiču sa
     * zlúčia do jedného. Segmenty patriace iným vodičom sa odstránia. Pripojené konce
     * vodičov (WireEnd) sa odpoja.
     */
    void removeJunction(WireJunction junction) {
        WireSegment seg0 = junction.getPrimaryWireSegment();
        WireSegment seg1 = junction.getSecondaryWireSegment();

        if (seg0 != null && seg1 != null) {
            if (seg0.getWire() == seg1.getWire() && seg0.getWire() == this) {
                Joint j0 = seg0.getOtherJoint(junction);
                Joint j1 = seg1.getOtherJoint(junction);

                this.segments.remove(seg0);
                this.segments.remove(seg1);
                this.segmentsGroup.getChildren().removeAll(seg0, seg1);

                j0.removeWireSegment(seg0);
                j1.removeWireSegment(seg1);

                WireSegment merged = new WireSegment(this, j0, j1);
                this.segments.add(merged);
                this.segmentsGroup.getChildren().add(merged);
            } else {
                if (seg0.getWire() != this) {
                    Wire otherWire = seg0.getWire();
                    otherWire.segments.remove(seg0);
                    otherWire.segmentsGroup.getChildren().remove(seg0);
                }
                if (seg1.getWire() != this) {
                    Wire otherWire = seg1.getWire();
                    otherWire.segments.remove(seg1);
                    otherWire.segmentsGroup.getChildren().remove(seg1);
                }
            }
        } else if (seg0 != null) {
            Wire otherWire = seg0.getWire();
            if (otherWire != this) {
                otherWire.segments.remove(seg0);
                otherWire.segmentsGroup.getChildren().remove(seg0);
            }
        } else if (seg1 != null) {
            Wire otherWire = seg1.getWire();
            if (otherWire != this) {
                otherWire.segments.remove(seg1);
                otherWire.segmentsGroup.getChildren().remove(seg1);
            }
        }

        List<WireEnd> attachedEnds = new ArrayList<>(junction.getConnectedEnds());
        for (WireEnd end : attachedEnds) {
            end.disconnect();
        }

        this.joints.remove(junction);
        this.jointsGroup.getChildren().remove(junction);
    }

    public boolean areBothEndsConnected() {
        return this.ends[0].isConnected() && this.ends[1].isConnected();
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
        this.addEventHandler(MouseEvent.MOUSE_ENTERED, onMouseEntered);
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
            this.potential.removeValueListener(potentialValueListener);
            this.potential.delete();
            this.potential = null;
        }

        Pin toUpdate;
        if (this.ends[0] != null && this.ends[1] != null) {
            Pin start = getEndPin(this.ends[0]);
            Pin end = getEndPin(this.ends[1]);

            if (start != null && end != null) {
                this.potential = new Potential(start, end);
                this.potential.addValueListener(potentialValueListener);
                toUpdate = start;
            } else {
                toUpdate = start != null ? start : end;
            }

            if (toUpdate != null && getSheet().simRunningProperty().getValue()) {
                getSheet().addEvent(new SheetEvent(toUpdate));
            }
        }
        refreshStateLabel();
    }

    /**
     * Nájde pin, na ktorý je koniec vodiča napojený (priamo alebo cez spájač).
     */
    private static Pin getEndPin(WireEnd end) {
        if (end.getPin() != null) return end.getPin();
        if (end.getJunction() != null) return end.getJunction().findConnectedPin();
        return null;
    }

    private void refreshStateLabel() {
        Potential.Value value = this.potential == null ? Potential.Value.NC : this.potential.getValue();
        this.segments.forEach(segment -> segment.setState(value));
    }

    @Override
    public void delete() {
        super.delete();
        this.ends[0].disconnect();
        this.ends[1].disconnect();

        for (int i = this.joints.size() - 1; i >= 0; i--) {
            Joint joint = this.joints.get(i);
            if (joint instanceof WireJunction) {
                joint.delete();
            }
        }

        // ak bol vodič vytvorený ako odbočka zo segmentu a nebol dokončený,
        // zrušíme aj ním založený spájač a kmeňový vodič sa spojí späť do jednej priamky
        for (WireJunction startJunction : this.startJunctions) {
            if (startJunction.getConnectedEnds().isEmpty()) {
                startJunction.getWire().removeJunction(startJunction);
            }
        }
        this.startJunctions.clear();

        this.getSheet().removeItem(this);
    }

    @Override
    public void select() {
        super.select();
        if (!this.isSelectable()) return;
        this.highlightSegments(1);
    }

    @Override
    public void deselect() {
        super.deselect();
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
