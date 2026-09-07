package sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 7410
 * 3x 3-vstupový NAND
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class Gen7410 extends Chip {

    private static final String NAME = "7410";
    private static final String SHORT_DESCRIPTION = "3x 3-vstupový NAND";
    private static final int PINS_COUNT = 14;
    private static final int _1A = 1;
    private static final int _1B = 2;
    private static final int _2A = 3;
    private static final int _2B = 4;
    private static final int _2C = 5;
    private static final int _2Y = 6;
    private static final int _GND = 7;
    private static final int _3Y = 8;
    private static final int _3A = 9;
    private static final int _3B = 10;
    private static final int _3C = 11;
    private static final int _1Y = 12;
    private static final int _1C = 13;
    private static final int _VCC = 14;

    public Gen7410() {
        super(PINS_COUNT);
    }

    public Gen7410(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate(int A, int B, int C, int Y) {
        if (this.isHigh(A) && this.isHigh(B) && this.isHigh(C)) {
            this.setPin(Y, Pin.PinState.LOW);
        } else {
            this.setPin(Y, Pin.PinState.HIGH);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_1A, new InputPin(this, "1A"));
        registerPin(_1B, new InputPin(this, "1B"));
        registerPin(_2A, new InputPin(this, "2A"));
        registerPin(_2B, new InputPin(this, "2B"));
        registerPin(_2C, new InputPin(this, "2C"));
        registerPin(_2Y, new OutputPin(this, "2Y"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_3Y, new OutputPin(this, "3Y"));
        registerPin(_3A, new InputPin(this, "3A"));
        registerPin(_3B, new InputPin(this, "3B"));
        registerPin(_3C, new InputPin(this, "3C"));
        registerPin(_1Y, new OutputPin(this, "1Y"));
        registerPin(_1C, new InputPin(this, "1C"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate(_1A, _1B, _1C, _1Y);
            this.updateGate(_2A, _2B, _2C, _2Y);
            this.updateGate(_3A, _3B, _3C, _3Y);
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_1Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_2Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_3Y, Pin.PinState.NOT_CONNECTED);
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
