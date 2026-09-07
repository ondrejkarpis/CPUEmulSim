package sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.NotConnectedPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 7430
 * 8-vstupový NAND
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class Gen7430 extends Chip {

    private static final String NAME = "7430";
    private static final String SHORT_DESCRIPTION = "8-vstupový NAND";
    private static final int PINS_COUNT = 14;
    private static final int _A = 1;
    private static final int _B = 2;
    private static final int _C = 3;
    private static final int _D = 4;
    private static final int _E = 5;
    private static final int _F = 6;
    private static final int _GND = 7;
    private static final int _Y_ = 8;
    private static final int _NC1 = 9;
    private static final int _NC2 = 10;
    private static final int _G = 11;
    private static final int _H = 12;
    private static final int _NC3 = 13;
    private static final int _VCC = 14;

    public Gen7430() {
        super(PINS_COUNT);
    }

    public Gen7430(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate(int A, int B, int C, int D, int E, int F, int G, int H, int Y) {
        if (this.isHigh(A) && this.isHigh(B) && this.isHigh(C)
                && this.isHigh(D) && this.isHigh(E) && this.isHigh(F)
                && this.isHigh(G) && this.isHigh(H)) {
            this.setPin(Y, Pin.PinState.LOW);
        } else {
            this.setPin(Y, Pin.PinState.HIGH);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_A, new InputPin(this, "A"));
        registerPin(_B, new InputPin(this, "B"));
        registerPin(_C, new InputPin(this, "C"));
        registerPin(_D, new InputPin(this, "D"));
        registerPin(_E, new InputPin(this, "E"));
        registerPin(_F, new InputPin(this, "F"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_Y_, new OutputPin(this, "Y_"));
        registerPin(_NC1, new NotConnectedPin(this));
        registerPin(_NC2, new NotConnectedPin(this));
        registerPin(_G, new InputPin(this, "G"));
        registerPin(_H, new InputPin(this, "H"));
        registerPin(_NC3, new NotConnectedPin(this));
        registerPin(_VCC, new InputPin(this, "VCC"));

    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate(_A, _B, _C, _D, _E, _F, _G, _H, _Y_);
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_Y_, Pin.PinState.NOT_CONNECTED);
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
