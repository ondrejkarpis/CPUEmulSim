package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sk.uniza.fri.cp.SchematicSim.Connectable;
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;
import sk.uniza.fri.cp.SchematicSim.GridSystem;
import sk.uniza.fri.cp.SchematicSim.Item;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Bod pripojenia vodiča na iný vodič (spájač). Umožňuje začínať a ukončovať
 * vodiče na existujúcich vodičoch. Vykresľuje sa ako malý plný krúžok.
 * Implementuje {@link Connectable} pre zapojenie do elektrickej siete potenciálov.
 */
public class WireJunction extends Joint implements Connectable {

    private static final Color FILL_COLOR = Color.BLACK;

    private final List<WireEnd> connectedEnds = new ArrayList<>();
    private final Circle junctionDot;
    private final Circle colorizerDot;

    public WireJunction(SchematicSheet sheet, Wire wire) {
        super(sheet, wire);

        GridSystem grid = getSheet().getGrid();
        double r = grid.getSizeMin() / 3.7;

        this.junctionDot = new Circle(0, 0, r, FILL_COLOR);
        this.colorizerDot = new Circle(0, 0, r * 1.3, Color.RED);
        this.colorizerDot.setOpacity(0);
        this.colorizerDot.setMouseTransparent(true);

        this.getChildren().add(this.junctionDot);
        this.getChildren().add(this.colorizerDot);

        registerWireStartHandlers();
    }

    /**
     * Umožňuje začínať nový vodič priamo ťahaním z tohto spájača (malý plný krúžok).
     * Vytvorí sa rozpracovaný vodič, ktorý sa končí na spájači a ťahá sa za kurzorom.
     */
    private void registerWireStartHandlers() {
        this.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (!event.isPrimaryButtonDown()) return;
            Pin.beginWireCreation(this);
            this.startFullDrag();
            event.consume();
        });

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            Wire inProgress = Pin.getInProgressWire();
            if (inProgress != null) {
                Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
                inProgress.updateBranchDrag(sheetXY.getX(), sheetXY.getY());
                event.consume();
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            Wire inProgress = Pin.getInProgressWire();
            if (inProgress == null) return;

            inProgress.setMouseTransparent(false);
            inProgress.setOpacity(1);
            if (!inProgress.areBothEndsConnected()) {
                inProgress.delete();
            }
            Pin.finishInProgressWire();
            if (inProgress.areBothEndsConnected()) {
                inProgress.settleToGrid();
            }
            event.consume();
        });
    }

    @Override
    protected Group generateJointGraphic(double radius) {
        return new Group();
    }

    public void setJunctionColor(Color color) {
        if (Platform.isFxApplicationThread()) {
            this.colorizerDot.setFill(color);
            this.colorizerDot.setOpacity(1);
        } else {
            Platform.runLater(() -> {
                this.colorizerDot.setFill(color);
                this.colorizerDot.setOpacity(1);
            });
        }
    }

    public void resetJunctionColor() {
        if (Platform.isFxApplicationThread()) this.colorizerDot.setOpacity(0);
        else Platform.runLater(() -> this.colorizerDot.setOpacity(0));
    }

    /**
     * Nájde pin, na ktorý je tento spájač elektricky napojený cez vodiče.
     */
    public Pin findConnectedPin() {
        for (WireSegment segment : getConnectedSegments()) {
            if (segment == null) continue;
            Joint other = segment.getOtherJoint(this);
            if (other instanceof WireEnd) {
                Pin pin = ((WireEnd) other).getPin();
                if (pin != null) return pin;
            } else {
                Pin pin = findPinThroughJoints(other, this);
                if (pin != null) return pin;
            }
        }
        return null;
    }

    private static Pin findPinThroughJoints(Joint current, Joint visited) {
        for (int i = 0; i < 2; i++) {
            WireSegment seg = i == 0 ? current.getPrimaryWireSegment() : current.getSecondaryWireSegment();
            if (seg == null) continue;
            Joint other = seg.getOtherJoint(current);
            if (other == visited) continue;
            if (other instanceof WireEnd) {
                Pin pin = ((WireEnd) other).getPin();
                if (pin != null) return pin;
            } else if (other instanceof WireJunction) {
                Pin pin = ((WireJunction) other).findConnectedPin();
                if (pin != null) return pin;
            }
        }
        return null;
    }

    private List<WireSegment> getConnectedSegments() {
        List<WireSegment> result = new ArrayList<>();
        if (wireSegments[0] != null) result.add(wireSegments[0]);
        if (wireSegments[1] != null) result.add(wireSegments[1]);
        return result;
    }

    @Override
    public void connectWireSegment(WireSegment segment) {
        if (this.wireSegments[0] == null) this.wireSegments[0] = segment;
        else this.wireSegments[1] = segment;
    }

    @Override
    public Side getExitSide() {
        return null;
    }

    /**
     * Preferovaný smer odbočky pri jej ťahaní z tohto spájača. Vodič má vychádzať kolmo
     * na kmeňový vodič (aby ho neprekrýval), pričom znamienko určí poloha kurzora:
     * nad spájačom TOP, pod BOTTOM, vľavo LEFT, vpravo RIGHT.
     * <p>
     * Smer kmeňa sa určuje LOKÁLNE v mieste spájača (posledná úsečka trasy primárneho
     * segmentu, ktorý podľa {@code createJunction} končí práve na tomto spájači) - nie z
     * celkového rozsahu segmentu. Segment môže byť totiž zvislo-vodorovná trasa (odbočka
     * tvaru L) a spájač môže stáť na jej vodorovnom "stube"; vtedy je lokálny smer vodorovný
     * a odbočka má ísť hore/dole, hoci celý segment je celkovo zvislý.
     */
    public Side branchExitFor(double mouseX, double mouseY) {
        WireSegment trunk = getPrimaryWireSegment() != null ? getPrimaryWireSegment() : getSecondaryWireSegment();
        if (trunk == null) return null;

        if (isLastPieceHorizontal(trunk)) {
            return mouseY < getLayoutY() ? Side.TOP : Side.BOTTOM;
        }
        return mouseX < getLayoutX() ? Side.LEFT : Side.RIGHT;
    }

    /** {@code true} ak je lokálny smer kmeňa v mieste spájača vodorovný. */
    private static boolean isLastPieceHorizontal(WireSegment segment) {
        List<Double> points = segment.getRoutedPoints();
        for (int i = points.size() - 4; i >= 0; i -= 2) {
            double x1 = points.get(i);
            double y1 = points.get(i + 1);
            double x2 = points.get(i + 2);
            double y2 = points.get(i + 3);
            double lenX = Math.abs(x2 - x1);
            double lenY = Math.abs(y2 - y1);
            if (lenX == 0 && lenY == 0) continue;
            return lenX >= lenY;
        }
        // žiadna použiteľná trasa - pôvodná heuristika z celkového rozsahu segmentu
        return Math.abs(segment.getEndX() - segment.getStartX()) >= Math.abs(segment.getEndY() - segment.getStartY());
    }

    @Override
    public Point2D getConnectionPoint() {
        return new Point2D(getLayoutX(), getLayoutY());
    }

    @Override
    public void delete() {
        getWire().removeJunction(this);
    }

    @Override
    public Item getOwnerItem() {
        return null;
    }

    void addWireEnd(WireEnd end) {
        if (!connectedEnds.contains(end)) connectedEnds.add(end);
    }

    void removeWireEnd(WireEnd end) {
        connectedEnds.remove(end);
    }

    List<WireEnd> getConnectedEnds() {
        return connectedEnds;
    }

    @Override
    public void highlight(int highlightType) {
    }

    @Override
    public void unhighlight(int highlightType) {
    }

    @Override
    public Point2D getSceneGridPosition() {
        return getConnectionPoint();
    }

    @Override
    public boolean isOccupied() {
        return false;
    }

    @Override
    public Potential getPotential() {
        Pin pin = findConnectedPin();
        if (pin != null) return pin.getPotential();
        return null;
    }
}
