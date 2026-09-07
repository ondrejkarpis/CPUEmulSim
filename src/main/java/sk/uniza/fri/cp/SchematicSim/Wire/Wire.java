package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseButton;
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

    private final WireEnd[] ends;
    private final List<Joint> joints;
    private final List<WireSegment> segments;

    private final Group jointsGroup;
    private final Group segmentsGroup;

    private Joint createdJoint;

    private final EventHandler<MouseEvent> onMouseDragDetected = event -> {
        if (event.isPrimaryButtonDown() && (event.isControlDown() || event.getClickCount() == 2)
                && event.getTarget() instanceof WireSegment) {
            WireSegment segmentToSplit = (WireSegment) event.getTarget();
            createdJoint = splitSegment(segmentToSplit);
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> onMouseDragged = event -> {
        if (event.isPrimaryButtonDown()) {
            if (createdJoint != null) {
                Event.fireEvent(createdJoint, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                        true, true, true, true, true,
                        true, true, true, true, null));
            }
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> onMouseReleased = event -> createdJoint = null;

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
     * Nahrádza pôvodné {@code Wire.updatePotential()} - bez BusInterface hacku (žiadne
     * zbernicové čipy v abstraktnom modeli neexistujú).
     */
    void updatePotential() {
        if (this.potential != null) {
            this.potential.delete();
            this.potential = null;
        }

        Pin toUpdate;
        if (this.ends[0] != null && this.ends[1] != null) {
            Pin start = this.ends[0].getPin();
            Pin end = this.ends[1].getPin();

            if (start != null && end != null) {
                this.potential = new Potential(start, end);
                toUpdate = start;
            } else {
                toUpdate = start != null ? start : end;
            }

            if (toUpdate != null && getSheet().simRunningProperty().getValue()) {
                getSheet().addEvent(new SheetEvent(toUpdate));
            }
        }
    }

    @Override
    public void delete() {
        super.delete();
        this.ends[0].disconnect();
        this.ends[1].disconnect();
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
