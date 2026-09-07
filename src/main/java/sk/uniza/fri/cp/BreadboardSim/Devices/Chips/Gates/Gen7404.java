package sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 7404
 * 6x negácia
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class Gen7404 extends Chip {

    private static final String NAME = "7404";
    private static final String SHORT_DESCRIPTION = "6x negácia";
    private static final int PINS_COUNT = 14;
    private static final int _1A = 1;
    private static final int _1Y = 2;
    private static final int _2A = 3;
    private static final int _2Y = 4;
    private static final int _3A = 5;
    private static final int _3Y = 6;
    private static final int _GND = 7;
    private static final int _4Y = 8;
    private static final int _4A = 9;
    private static final int _5Y = 10;
    private static final int _5A = 11;
    private static final int _6Y = 12;
    private static final int _6A = 13;
    private static final int _VCC = 14;

    public Gen7404() {
        super(PINS_COUNT);
    }

    public Gen7404(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate(int A, int Y) {
        if (this.isLow(A)) {
            this.setPin(Y, Pin.PinState.HIGH);
        } else {
            this.setPin(Y, Pin.PinState.LOW);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_1A, new InputPin(this, "1A"));
        registerPin(_1Y, new OutputPin(this, "1Y"));
        registerPin(_2A, new InputPin(this, "2A"));
        registerPin(_2Y, new OutputPin(this, "2Y"));
        registerPin(_3A, new InputPin(this, "3A"));
        registerPin(_3Y, new OutputPin(this, "3Y"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_4Y, new OutputPin(this, "4Y"));
        registerPin(_4A, new InputPin(this, "4A"));
        registerPin(_5Y, new OutputPin(this, "5Y"));
        registerPin(_5A, new InputPin(this, "5A"));
        registerPin(_6Y, new OutputPin(this, "6Y"));
        registerPin(_6A, new InputPin(this, "6A"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate(_1A, _1Y);
            this.updateGate(_2A, _2Y);
            this.updateGate(_3A, _3Y);
            this.updateGate(_4A, _4Y);
            this.updateGate(_5A, _5Y);
            this.updateGate(_6A, _6Y);
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_1Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_2Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_3Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_4Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_5Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_6Y, Pin.PinState.NOT_CONNECTED);
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortStringDescription() {
        return SHORT_DESCRIPTION;
    }

    public Pane getImage() {
        return generateItemImage(NAME, PINS_COUNT);
    }
}
