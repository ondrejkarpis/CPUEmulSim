package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

import java.util.Arrays;

/**
 * Obvod 74138
 * Dekóder z 3 na 8
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class SN74138 extends Chip {

    private static final String NAME = "SN74138";
    private static final String SHORT_DESCRIPTION = "Dekóder z 3 na 8";
    private static final int PINS_COUNT = 16;
    private static final int _A = 1;
    private static final int _B = 2;
    private static final int _C = 3;
    private static final int _G2A_ = 4;
    private static final int _G2B_ = 5;
    private static final int _G1 = 6;
    private static final int _Y7 = 7;
    private static final int _GND = 8;
    private static final int _Y6 = 9;
    private static final int _Y5 = 10;
    private static final int _Y4 = 11;
    private static final int _Y3 = 12;
    private static final int _Y2 = 13;
    private static final int _Y1 = 14;
    private static final int _Y0 = 15;
    private static final int _VCC = 16;

    private static final int[] outputs = {_Y0, _Y1, _Y2, _Y3, _Y4, _Y5, _Y6, _Y7};

    public SN74138() {
        super(PINS_COUNT);
    }

    public SN74138(Board board) {
        super(board, PINS_COUNT);
    }

    private void decodeOneOutputLow(int Y) {
        Arrays.stream(outputs).forEach(output -> {
            if (output == Y) {
                this.setPin(output, Pin.PinState.LOW);
            } else {
                this.setPin(output, Pin.PinState.HIGH);
            }
        });

    }

    private void updateGate(int G1, int G2A_, int G2B_, int C, int B, int A) {
        //enable
        if (this.isHigh(G1) && this.isLow(G2A_) && this.isLow(G2B_)) {
            int codedOutput = 0;
            if (this.isHigh(A)) codedOutput |= 1;
            if (this.isHigh(B)) codedOutput |= 1 << 1;
            if (this.isHigh(C)) codedOutput |= 1 << 2;

            decodeOneOutputLow(outputs[codedOutput]);
        } else {
            //not enable
            Arrays.stream(outputs).forEach(output -> this.setPin(output, Pin.PinState.HIGH));
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_A, new InputPin(this, "A"));
        registerPin(_B, new InputPin(this, "B"));
        registerPin(_C, new InputPin(this, "C"));
        registerPin(_G2A_, new InputPin(this, "G2A_"));
        registerPin(_G2B_, new InputPin(this, "G2B_"));
        registerPin(_G1, new InputPin(this, "G1"));
        registerPin(_Y7, new OutputPin(this, "Y7"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_Y6, new OutputPin(this, "Y6"));
        registerPin(_Y5, new OutputPin(this, "Y5"));
        registerPin(_Y4, new OutputPin(this, "Y4"));
        registerPin(_Y3, new OutputPin(this, "Y3"));
        registerPin(_Y2, new OutputPin(this, "Y2"));
        registerPin(_Y1, new OutputPin(this, "Y1"));
        registerPin(_Y0, new OutputPin(this, "Y0"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate(_G1, _G2A_, _G2B_, _C, _B, _A);
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_Y7, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y6, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y5, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y4, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y3, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y2, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y1, Pin.PinState.NOT_CONNECTED);
        this.setPin(_Y0, Pin.PinState.NOT_CONNECTED);
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
