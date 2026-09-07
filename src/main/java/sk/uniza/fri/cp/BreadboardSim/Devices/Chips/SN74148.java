package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 74148
 * Prioritný kóder z 8 na 3
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class SN74148 extends Chip {

    private static final String NAME = "SN74148";
    private static final String SHORT_DESCRIPTION = "Prioritný kóder z 8 na 3";
    private static final int PINS_COUNT = 16;
    private static final int _4 = 1;
    private static final int _5 = 2;
    private static final int _6 = 3;
    private static final int _7 = 4;
    private static final int _EI = 5;
    private static final int _A2 = 6;
    private static final int _A1 = 7;
    private static final int _GND = 8;
    private static final int _A0 = 9;
    private static final int _0 = 10;
    private static final int _1 = 11;
    private static final int _2 = 12;
    private static final int _3 = 13;
    private static final int _GS = 14;
    private static final int _EO = 15;
    private static final int _VCC = 16;

    private static final int[] inputs = {_0, _1, _2, _3, _4, _5, _6, _7};

    public SN74148() {
        super(PINS_COUNT);
    }

    public SN74148(Board board) {
        super(board, PINS_COUNT);
    }

    private int decodeInputs() {
        int decodedNumber = 0;
        for (int i = 0; i < inputs.length; i++) {
            if (this.isLow(inputs[inputs.length - 1 - i])) break;
            decodedNumber |= 1 << i;
        }

        return decodedNumber;
    }

    private void updateGate() {
        //enable
        if (this.isLow(_EI)) {
            int decodedNumber = decodeInputs();

            if ((decodedNumber & 1) == 1)
                this.setPin(_A0, Pin.PinState.HIGH);
            else
                this.setPin(_A0, Pin.PinState.LOW);

            if ((decodedNumber & 2) == 2)
                this.setPin(_A1, Pin.PinState.HIGH);
            else
                this.setPin(_A1, Pin.PinState.LOW);

            if ((decodedNumber & 4) == 4)
                this.setPin(_A2, Pin.PinState.HIGH);
            else
                this.setPin(_A2, Pin.PinState.LOW);

            if (decodedNumber == 255) {
                this.setPin(_GS, Pin.PinState.HIGH);
                this.setPin(_EO, Pin.PinState.LOW);
            } else {
                this.setPin(_GS, Pin.PinState.LOW);
                this.setPin(_EO, Pin.PinState.HIGH);
            }

        } else if (this.isHigh(_EI)) {
            //not enable
            this.setPin(_A2, Pin.PinState.HIGH);
            this.setPin(_A1, Pin.PinState.HIGH);
            this.setPin(_A0, Pin.PinState.HIGH);
            this.setPin(_GS, Pin.PinState.HIGH);
            this.setPin(_EO, Pin.PinState.HIGH);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_4, new InputPin(this, "4"));
        registerPin(_5, new InputPin(this, "5"));
        registerPin(_6, new InputPin(this, "6"));
        registerPin(_7, new InputPin(this, "7"));
        registerPin(_EI, new InputPin(this, "EI"));
        registerPin(_A2, new OutputPin(this, "A2"));
        registerPin(_A1, new OutputPin(this, "A1"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_A0, new OutputPin(this, "A0"));
        registerPin(_0, new InputPin(this, "0"));
        registerPin(_1, new InputPin(this, "1"));
        registerPin(_2, new InputPin(this, "2"));
        registerPin(_3, new InputPin(this, "3"));
        registerPin(_GS, new OutputPin(this, "GS"));
        registerPin(_EO, new OutputPin(this, "EO"));
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
        this.setPin(_A0, Pin.PinState.NOT_CONNECTED);
        this.setPin(_A1, Pin.PinState.NOT_CONNECTED);
        this.setPin(_A2, Pin.PinState.NOT_CONNECTED);
        this.setPin(_GS, Pin.PinState.NOT_CONNECTED);
        this.setPin(_EO, Pin.PinState.NOT_CONNECTED);
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
