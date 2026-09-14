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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final double baseRadius;

    /**
     * Vodič, ktorý spájač obýva (v novom modeli je spájač hraničným bodom dvoch vodičov -
     * kmeň sa v ňom rozdeľuje na dva vodiče). Hostiteľ zodpovedá za vykreslenie node.
     */
    private Wire hostWire;

    /** Príznak, že je spájač už zničený (zabezpečuje idempotentné mazanie). */
    private boolean removed;

    public WireJunction(SchematicSheet sheet, Wire wire) {
        super(sheet, wire);

        GridSystem grid = getSheet().getGrid();
        double r = grid.getSizeMin() / 3.7;
        this.baseRadius = r;

        this.junctionDot = new Circle(0, 0, r, FILL_COLOR);
        this.colorizerDot = new Circle(0, 0, r * 1.3, Color.RED);
        this.colorizerDot.setOpacity(0);
        this.colorizerDot.setMouseTransparent(true);

        this.getChildren().add(this.junctionDot);
        this.getChildren().add(this.colorizerDot);

        // zvýraznenie bodu pri nájazde kurzora - signalizuje, že sa dá uchopiť a presunúť
        this.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> this.junctionDot.setRadius(this.baseRadius * 1.2));
        this.addEventFilter(MouseEvent.MOUSE_EXITED, event -> this.junctionDot.setRadius(this.baseRadius));

        registerWireStartHandlers();
    }

    public void setHostWire(Wire wire) {
        this.hostWire = wire;
    }

    public Wire getHostWire() {
        return this.hostWire;
    }

    @Override
    public Wire getWire() {
        return this.hostWire != null ? this.hostWire : super.getWire();
    }

    public boolean isRemoved() {
        return this.removed;
    }

    void markRemoved() {
        this.removed = true;
    }
/**
     * Spájač, ktorý sa práve presúva myšou (null, ak žiadny) - potrebné pre routing bez
     * snapovania počas ťahania.
     */
    private static WireJunction draggingJunction;

    static boolean isAnyJunctionDragged() {
        return draggingJunction != null;
    }

    /**
     * Umožňuje začínať nový vodič priamo ťahaním z tohto spájača (Ctrl+ťah, malý plný krúžok)
     * a presúvať spájač prostým ťahaním myšou (pripojené vodiče sa automaticky preroutujú).
     */
    private void registerWireStartHandlers() {
        this.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (!event.isPrimaryButtonDown()) return;
            if (event.isShortcutDown()) {
                // Ctrl+ťah = spustenie novej odbočky zo spájača
                Pin.beginWireCreation(this);
                this.startFullDrag();
                event.consume();
                return;
            }

            // prostý ťah = presun spájača (body vodičov sa aktuálne vykreslú nelícne)
            draggingJunction = this;
            this.startFullDrag();
            event.consume();
        });

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (draggingJunction == this) {
                Point2D sheet = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
                setLayoutX(sheet.getX());
                setLayoutY(sheet.getY());
                event.consume();
                return;
            }

            Wire inProgress = Pin.getInProgressWire();
            if (inProgress != null) {
                Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
                inProgress.updateBranchDrag(sheetXY.getX(), sheetXY.getY());
                event.consume();
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (draggingJunction == this) {
                draggingJunction = null;
                finishJunctionDrag();
                event.consume();
                return;
            }

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

    /** Zarovnanie presunutého spájača na mriežku a prepočet trás pripojených vodičov. */
    private void finishJunctionDrag() {
        GridSystem grid = getSheet().getGrid();
        double size = grid.getSizeMin();
        setLayoutX(Math.round(getLayoutX() / size) * size);
        setLayoutY(Math.round(getLayoutY() / size) * size);

        for (WireEnd end : new ArrayList<>(this.connectedEnds)) {
            Wire wire = end.getWire();
            if (wire != null) {
                wire.settleToGrid();
                wire.updatePotential();
            }
        }
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
     * V novom modeli sa spájačom vodiace vodiče navzájom dotýkajú cez napojené konce,
     * preto sa prechádza graf napojených koncov (s ochranou proti cyklom). Ako poistka
     * pre staré súbory ostáva prechod cez segmenty.
     */
    public Pin findConnectedPin() {
        return findConnectedPin(new HashSet<>());
    }

    private Pin findConnectedPin(Set<WireJunction> visited) {
        if (!visited.add(this)) return null;

        for (WireEnd end : new ArrayList<>(this.connectedEnds)) {
            Pin direct = end.getPin();
            if (direct != null) return direct;

            Wire wire = end.getWire();
            if (wire == null) continue;
            for (WireEnd other : wire.getEnds()) {
                if (other == end) continue;
                Pin pin = other.getPin();
                if (pin != null) return pin;
                WireJunction otherJunction = other.getJunction();
                if (otherJunction != null && otherJunction != this) {
                    Pin found = otherJunction.findConnectedPin(visited);
                    if (found != null) return found;
                }
            }
        }

        // legacy: prechod cez segmenty (starý model s vnútorným spájačom na kmeňovom vodiči)
        if (!visited.isEmpty()) {
            Pin legacy = findPinThroughSegments();
            if (legacy != null) return legacy;
        }
        return null;
    }

    private Pin findPinThroughSegments() {
        Pin pin = findPinThroughSegments(wireSegments[0], this);
        if (pin != null) return pin;
        return findPinThroughSegments(wireSegments[1], this);
    }

    private static Pin findPinThroughSegments(WireSegment seg, Joint visited) {
        if (seg == null) return null;
        Joint other = seg.getOtherJoint(visited);
        if (other instanceof WireEnd) {
            return ((WireEnd) other).getPin();
        }
        if (other instanceof WireJunction) {
            return ((WireJunction) other).findPinThroughSegments(other.getPrimaryWireSegment(), other);
        }
        return null;
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
        WireSegment trunk = findTrunkSegment();
        if (trunk == null) return null;

        if (isTrunkSegmentHorizontal(trunk)) {
            return mouseY < getLayoutY() ? Side.TOP : Side.BOTTOM;
        }
        return mouseX < getLayoutX() ? Side.LEFT : Side.RIGHT;
    }

    /**
     * Nájde segment kmeňa tesne pri tomto spájači. V novom modeli sú kmeňové vodiče na spájač
     * napojené cez {@link WireEnd} (kmeň sa v spájači rozdeľuje na dva vodiče), preto sa segment
     * hľadá cez napojené konce - nie cez vlastné {@code wireSegments}, ktoré sa plnia len priamym
     * (legacy) spojením segmentu so spájačom. Koniec rozpracovanej odbočky sa preskakuje, aby sa
     * smer neurčoval z odbočky samej, ale z kmeňa.
     */
    private WireSegment findTrunkSegment() {
        Wire inProgress = Pin.getInProgressWire();
        for (WireEnd end : this.connectedEnds) {
            if (inProgress != null && end.getWire() == inProgress) continue;
            WireSegment seg = end.getWireSegmentForJunction();
            if (seg != null) return seg;
        }
        // legacy: segmenty registrované priamo na spájači
        if (getPrimaryWireSegment() != null) return getPrimaryWireSegment();
        return getSecondaryWireSegment();
    }

    /**
     * {@code true} ak je lokálny smer kmeňa v mieste spájača vodorovný. Rozhoduje úsečka trasy
     * tesne pri konci, ktorý sa dotýka spájača: pri {@code startJoint} je to prvá úsečka trasy,
     * pri {@code endJoint} zase posledná.
     */
    private boolean isTrunkSegmentHorizontal(WireSegment segment) {
        for (WireEnd end : this.connectedEnds) {
            if (end.getWireSegmentForJunction() != segment) continue;
            if (segment.getStartJoint() == end) return isFirstPieceHorizontal(segment);
            if (segment.getEndJoint() == end) return isLastPieceHorizontal(segment);
        }
        return isLastPieceHorizontal(segment);
    }

    /** {@code true} ak je prvá úsečka trasy segmentu vodorovná (úsečka opúšťajúca spájač). */
    private static boolean isFirstPieceHorizontal(WireSegment segment) {
        List<Double> points = segment.getRoutedPoints();
        for (int i = 0; i + 3 < points.size(); i += 2) {
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

    /** {@code true} ak je posledná úsečka trasy segmentu vodorovná (úsečka vchádzajúca do spájača). */
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
        if (this.removed) return;
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
