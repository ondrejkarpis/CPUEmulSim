package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Sheet.SheetEvent;
import sk.uniza.fri.cp.SchematicSim.Side;

/**
 * Koniec káblika. Prevzaté z BreadboardSim.Wire.WireEnd - narozdiel od originálu sa pripája
 * priamo na {@link Pin} súčiastky namiesto {@code Socket} na breadboarde.
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia Socket -> Pin pre SchematicSim
 */
public class WireEnd extends Joint {

    private Pin pin;

    private double lastPosX = -1;
    private double lastPosY = -1;
    private boolean moved;

    // pri zmene pozície vlastníka pinu (posun súčiastky) sa posunie aj koniec vodiča
    private final ChangeListener<Transform> pinPositionChangeListener = (observable, oldValue, newValue) -> {
        if (lastPosX == -1) {
            lastPosX = getLayoutX();
            lastPosY = getLayoutY();
        }

        Point2D p = pin.getSceneGridPosition();
        setLayoutX(p.getX() / getSheet().getAppliedScale());
        setLayoutY(p.getY() / getSheet().getAppliedScale());

        getWire().moveJointsWithEnd(this, getLayoutX() - lastPosX, getLayoutY() - lastPosY);

        lastPosX = getLayoutX();
        lastPosY = getLayoutY();
    };

    public WireEnd(SchematicSheet sheet, Wire wire) {
        super(sheet, wire);

        this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            getWire().setMouseTransparent(true);
            getWire().setOpacity(0.5);
        });

        this.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            startFullDrag();
            this.moved = true;
            if (this.pin != null) this.disconnect();
            this.setDefaultColor();
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            getWire().setMouseTransparent(false);
            getWire().setOpacity(1);
            if (!this.moved) getSheet().addSelect(getWire());
            this.moved = false;
            event.consume();
        });

        this.incRadius();
    }

    public boolean isConnected() {
        return this.pin != null;
    }

    /**
     * Pripojenie konca vodiča k pinu. Ak bol predtým pripojený inde, najprv sa odpojí.
     * Ak sa nepodarí pripojiť (pin je obsadený), zafarbí sa na červeno.
     */
    public void connect(Pin target) {
        if (this.pin == null) {
            if (target != null && !target.isOccupied()) {
                this.pin = target;
                this.pin.setWireEnd(this);

                this.pin.getOwner().localToParentTransformProperty().addListener(pinPositionChangeListener);

                Point2D p = target.getSceneGridPosition();
                setLayoutX(p.getX() / getSheet().getAppliedScale());
                setLayoutY(p.getY() / getSheet().getAppliedScale());

                lastPosX = getLayoutX();
                lastPosY = getLayoutY();

                this.setColor(this.getWire().getColor().brighter());
                this.getWire().updatePotential();
            } else {
                this.setColor(Color.RED);
            }
        } else {
            disconnect();
            connect(target);
        }
    }

    public Pin getPin() {
        return pin;
    }

    @Override
    public Side getExitSide() {
        return pin != null ? pin.getExitSide() : null;
    }

    @Override
    public void setDefaultColor() {
        if (this.isConnected()) this.setColor(this.getWire().getColor().brighter());
        else super.setDefaultColor();
    }

    @Override
    public void connectWireSegment(WireSegment segment) {
        this.wireSegments[0] = segment;
    }

    @Override
    public void delete() {
        super.delete();
        this.getWire().delete();
    }

    /**
     * Odpojenie konca od pinu.
     */
    protected void disconnect() {
        if (this.pin == null) return;
        Pin pinToUpdate = this.pin;

        this.pin.getOwner().localToParentTransformProperty().removeListener(pinPositionChangeListener);
        this.pin.clearWireEnd();
        this.pin = null;

        this.getWire().updatePotential();
        this.setDefaultColor();

        if (getSheet().isSimulationRunning()) {
            getSheet().addEvent(new SheetEvent(pinToUpdate));
        }
    }

    void releasePin() {
        if (this.pin == null) return;

        Pin connectedPin = this.pin;
        connectedPin.getOwner().localToParentTransformProperty().removeListener(pinPositionChangeListener);
        this.pin = null;
        if (connectedPin.getWireEnd() == this) connectedPin.clearWireEnd();
    }
}
