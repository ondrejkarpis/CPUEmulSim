package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;

import java.util.List;
import java.util.stream.Stream;

/**
 * Segment vodiča medzi dvomi zlomami. Nahrádza pôvodnú {@code WireSegment extends Line} -
 * namiesto priamej čiary (bindovanej priamo na súradnice jointov) sa vykresľuje ortogonálna
 * (Manhattan) trasa vypočítaná cez {@link OrthogonalRouter}, prepočítaná zakaždým, keď sa
 * pohne ktorýkoľvek z jej koncových bodov.
 *
 * @author Tomáš Hianik (pôvodný autor WireSegment ako Line), prerobenie na ortogonálny routing
 */
public class WireSegment extends Group {

    private final Wire wire;
    private Joint startJoint;
    private Joint endJoint;
    private final Polyline line = new Polyline();

    WireSegment(Wire wire, Joint start, Joint end) {
        this(wire, start, end, null);
    }

    /**
     * Vytvorenie segmentu s vopred určenou trasou. Ak je {@code path} dodaná, trasa sa
     * neprepočítava cez {@link OrthogonalRouter}, ale preberie sa priamo. Používa sa pri
     * rozdelení existujúceho vodiča na dva segmenty a pri zlúčení dvoch segmentov, kde je
     * potrebné zachovať pôvodnú geometriu trasy - inak by nový routing zmenil vzhľad
     * dotknutého vodiča.
     */
    WireSegment(Wire wire, Joint start, Joint end, List<Point2D> path) {
        this.wire = wire;
        this.startJoint = start;
        this.endJoint = end;

        this.startJoint.connectWireSegment(this);
        this.endJoint.connectWireSegment(this);

        this.line.setStrokeWidth(6);
        this.line.setFill(null);
        this.line.setStroke(wire.getCurrentColor());
        this.line.setOpacity(1);
        this.getChildren().add(line);

        // na rozdiel od originálu (priama väzba property-bindingom) tu trasa nie je lineárna
        // funkcia koncových bodov, preto počúvame na zmenu polohy a prepočítavame ju ručne
        this.startJoint.layoutXProperty().addListener((o, ov, nv) -> updateGraphics());
        this.startJoint.layoutYProperty().addListener((o, ov, nv) -> updateGraphics());
        this.endJoint.layoutXProperty().addListener((o, ov, nv) -> updateGraphics());
        this.endJoint.layoutYProperty().addListener((o, ov, nv) -> updateGraphics());

        if (path != null) {
            applyPath(path);
        } else {
            updateGraphics();
        }
    }

    void updateGraphics() {
        Point2D p0 = startJoint.getConnectionPoint();
        Point2D p1 = endJoint.getConnectionPoint();

        // zlomy sa snapujú na mriežku až po dokončení vodiča (obidva konce pripojené a vodič
        // sa práve neťahá); počas ťahania sa trasa počíta plynulo bez snapovania
        int grid = isSettled() ? wire.getSheet().getGrid().getSizeMin() : 0;
        List<Point2D> path = OrthogonalRouter.route(p0, startJoint.getExitSide(), p1, endJoint.getExitSide(),
                wire.getBranchExit(), grid);

        applyPath(path);
    }

    private boolean isSettled() {
        return this.wire.areBothEndsConnected() && Pin.getInProgressWire() != this.wire
                && !WireJunction.isAnyJunctionDragged();
    }

    private void applyPath(List<Point2D> path) {
        Double[] flat = path.stream().flatMap(p -> Stream.of(p.getX(), p.getY())).toArray(Double[]::new);
        line.getPoints().setAll(flat);
    }

    public Wire getWire() {
        return this.wire;
    }

    public void setColor(Paint color) {
        this.line.setStroke(color);
    }

    /** Pre zvýraznenie pri výbere (Wire.highlightSegments) - prvý a posledný bod aktuálnej trasy. */
    public double getStartX() { return line.getPoints().isEmpty() ? startJoint.getLayoutX() : line.getPoints().get(0); }
    public double getStartY() { return line.getPoints().size() < 2 ? startJoint.getLayoutY() : line.getPoints().get(1); }
    public double getEndX() { return line.getPoints().size() < 2 ? endJoint.getLayoutX() : line.getPoints().get(line.getPoints().size() - 2); }
    public double getEndY() { return line.getPoints().isEmpty() ? endJoint.getLayoutY() : line.getPoints().get(line.getPoints().size() - 1); }

    /** Body aktuálnej ortogonálnej trasy - pre presnejšie zvýraznenie viac ako len prvý/posledný bod. */
    public java.util.List<Double> getRoutedPoints() {
        return line.getPoints();
    }

    Joint getEndJoint() {
        return endJoint;
    }

    Joint getOtherJoint(Joint joint) {
        return startJoint == joint ? endJoint : startJoint;
    }

    Joint getStartJoint() {
        return startJoint;
    }
}
