package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import sk.uniza.fri.cp.SchematicSim.Connectable;
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
    private WireJunction junction;

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

    // pri zmene pozície spájača (WireJunction) sa posunie aj koniec vodiča
    private final ChangeListener<Number> junctionPositionChangeListener = (observable, oldValue, newValue) -> {
        if (this.junction == null) return;

        double deltaX = junction.getLayoutX() - lastPosX;
        double deltaY = junction.getLayoutY() - lastPosY;
        setLayoutX(junction.getLayoutX());
        setLayoutY(junction.getLayoutY());

        getWire().moveJointsWithEnd(this, deltaX, deltaY);

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
            if (this.pin != null) {
                this.disconnect();
                this.setDefaultColor();
            } else if (this.junction != null) {
                Pin.beginWireCreation(this.junction);
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            Wire inProgress = Pin.getInProgressWire();
            if (this.junction != null && inProgress != null) {
                Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
                inProgress.updateBranchDrag(sheetXY.getX(), sheetXY.getY());
                this.setLayoutX(this.junction.getLayoutX());
                this.setLayoutY(this.junction.getLayoutY());
                event.consume();
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            getWire().setMouseTransparent(false);
            getWire().setOpacity(1);
            if (!this.moved) getSheet().addSelect(getWire());
            this.moved = false;

            Wire inProgress = Pin.getInProgressWire();
            if (inProgress != null) {
                inProgress.setMouseTransparent(false);
                inProgress.setOpacity(1);
                if (!inProgress.areBothEndsConnected()) {
                    inProgress.delete();
                }
                Pin.finishInProgressWire();
            }

            event.consume();
        });

        this.incRadius();
    }

    public boolean isConnected() {
        return this.pin != null || this.junction != null;
    }

    /**
     * Pripojenie konca vodiča k pinu alebo spájaču. Ak bol predtým pripojený inde,
     * najprv sa odpojí. Ak sa nepodarí pripojiť (pin je obsadený), zafarbí sa na červeno.
     */
    public void connect(Connectable target) {
        if (target instanceof Pin) {
            connectToPin((Pin) target);
        } else if (target instanceof WireJunction) {
            connectToJunction((WireJunction) target);
        }
    }

    private void connectToPin(Pin target) {
        if (this.pin == null && this.junction == null) {
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
            connectToPin(target);
        }
    }

    private void connectToJunction(WireJunction target) {
        if (this.pin == null && this.junction == null) {
            if (target != null) {
                this.junction = target;
                target.addWireEnd(this);

                this.junction.layoutXProperty().addListener(junctionPositionChangeListener);
                this.junction.layoutYProperty().addListener(junctionPositionChangeListener);

                Point2D p = target.getConnectionPoint();
                setLayoutX(p.getX());
                setLayoutY(p.getY());

                lastPosX = getLayoutX();
                lastPosY = getLayoutY();

                this.setColor(this.getWire().getColor().brighter());
                this.getWire().updatePotential();
            } else {
                this.setColor(Color.RED);
            }
        } else {
            disconnect();
            connectToJunction(target);
        }
    }

    public Pin getPin() {
        return pin;
    }

    public WireJunction getJunction() {
        return junction;
    }

    /**
     * Prepočítanie pozície konca vodiča podľa aktuálnej polohy pinu (používa sa pri premiestnení
     * vývodu na súčiastke, kedy sa pin sám presunul - posun tela súčiastky rieši listener vyššie).
     */
    public void refreshPosition() {
        if (pin == null) return;

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
        this.disconnect();
        super.delete();
        this.getWire().delete();
    }

    /**
     * Odpojenie konca od pinu alebo spájača.
     */
    protected void disconnect() {
        if (this.pin != null) {
            Pin pinToUpdate = this.pin;
            this.pin.getOwner().localToParentTransformProperty().removeListener(pinPositionChangeListener);
            this.pin.clearWireEnd();
            this.pin = null;
            this.getWire().updatePotential();
            this.setDefaultColor();
            if (getSheet().isSimulationRunning()) {
                getSheet().addEvent(new SheetEvent(pinToUpdate));
            }
        } else if (this.junction != null) {
            this.junction.layoutXProperty().removeListener(junctionPositionChangeListener);
            this.junction.layoutYProperty().removeListener(junctionPositionChangeListener);
            this.junction.removeWireEnd(this);
            this.junction = null;
            this.getWire().updatePotential();
            this.setDefaultColor();
        }
    }

}
