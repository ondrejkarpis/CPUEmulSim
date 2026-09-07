package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 74151
 * 8 na 1 multiplexor
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class SN74151 extends Chip {

    private static final String NAME = "SN74151";
    private static final String SHORT_DESCRIPTION = "8 na 1 multiplexor";
    private static final int PINS_COUNT = 16;
    private static final int _D3 = 1;
    private static final int _D2 = 2;
    private static final int _D1 = 3;
    private static final int _D0 = 4;
    private static final int _Y = 5;
    private static final int _W = 6;
    private static final int _G_ = 7;
    private static final int _GND = 8;
    private static final int _C = 9;
    private static final int _B = 10;
    private static final int _A = 11;
    private static final int _D7 = 12;
    private static final int _D6 = 13;
    private static final int _D5 = 14;
    private static final int _D4 = 15;
    private static final int _VCC = 16;

    private static final int[] dInputs = {_D0, _D1, _D2, _D3, _D4, _D5, _D6, _D7};

    public SN74151() {
        super(PINS_COUNT);
    }

    public SN74151(Board board) {
        super(board, PINS_COUNT);
    }

    private int decodeInputs() {
        int decodedNumber = 0;

        if (this.isHigh(_A)) decodedNumber += 1;
        if (this.isHigh(_B)) decodedNumber += 2;
        if (this.isHigh(_C)) decodedNumber += 4;

        return decodedNumber;
    }

    private void updateGate() {
        if (this.isLow(_G_)) {
            int decodedNumber = decodeInputs();

            if (this.isHigh(dInputs[decodedNumber])) {
                this.setPin(_Y, Pin.PinState.HIGH);
                this.setPin(_W, Pin.PinState.LOW);
            } else {
                this.setPin(_Y, Pin.PinState.LOW);
                this.setPin(_W, Pin.PinState.HIGH);
            }

        } else if (this.isHigh(_G_)) {
            this.setPin(_Y, Pin.PinState.LOW);
            this.setPin(_W, Pin.PinState.HIGH);
        }

    }

    @Override
    protected void fillPins() {
        registerPin(_D3, new InputPin(this, "D3"));
        registerPin(_D2, new InputPin(this, "D2"));
        registerPin(_D1, new InputPin(this, "D1"));
        registerPin(_D0, new InputPin(this, "D0"));
        registerPin(_Y, new OutputPin(this, "Y"));
        registerPin(_W, new OutputPin(this, "W"));
        registerPin(_G_, new InputPin(this, "G_"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_C, new InputPin(this, "C"));
        registerPin(_B, new InputPin(this, "B"));
        registerPin(_A, new InputPin(this, "A"));
        registerPin(_D7, new OutputPin(this, "D7"));
        registerPin(_D6, new OutputPin(this, "D6"));
        registerPin(_D5, new OutputPin(this, "D5"));
        registerPin(_D4, new OutputPin(this, "D4"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate();
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_W, Pin.PinState.NOT_CONNECTED);
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
