package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 74153
 * 2x 1 zo 4 multiplexor
 *
 * @author Tomáš Hianik
 * @created 6.5.2017.
 */
public class SN74153 extends Chip {

    private static final String NAME = "SN74153";
    private static final String SHORT_DESCRIPTION = "2x 1 zo 4 multiplexor";
    private static final int PINS_COUNT = 16;
    private static final int _1G_ = 1;
    private static final int _B = 2;
    private static final int _1C3 = 3;
    private static final int _1C2 = 4;
    private static final int _1C1 = 5;
    private static final int _1C0 = 6;
    private static final int _1Y = 7;
    private static final int _GND = 8;
    private static final int _2Y = 9;
    private static final int _2C0 = 10;
    private static final int _2C1 = 11;
    private static final int _2C2 = 12;
    private static final int _2C3 = 13;
    private static final int _A = 14;
    private static final int _2G_ = 15;
    private static final int _VCC = 16;

    public SN74153() {
        super(PINS_COUNT);
    }

    public SN74153(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate(int A, int B, int C0, int C1, int C2, int C3, int G_, int Y) {
        if (this.isHigh(G_)) {
            this.setPin(Y, Pin.PinState.LOW);
        } else if (this.isLow(G_)) {
            //adresa
            int address = 0;
            if (this.isHigh(B)) address += 1;
            if (this.isHigh(A)) address += 2;

            switch (address) {
                case 0:
                    this.setPin(Y, this.isHigh(C0) ? Pin.PinState.HIGH : Pin.PinState.LOW);
                    break;
                case 1:
                    this.setPin(Y, this.isHigh(C1) ? Pin.PinState.HIGH : Pin.PinState.LOW);
                    break;
                case 2:
                    this.setPin(Y, this.isHigh(C2) ? Pin.PinState.HIGH : Pin.PinState.LOW);
                    break;
                case 3:
                    this.setPin(Y, this.isHigh(C3) ? Pin.PinState.HIGH : Pin.PinState.LOW);
                    break;
                default:
            }
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_1G_, new InputPin(this, "1G_"));
        registerPin(_B, new InputPin(this, "B"));
        registerPin(_1C3, new InputPin(this, "1C3"));
        registerPin(_1C2, new InputPin(this, "1C2"));
        registerPin(_1C1, new InputPin(this, "1C1"));
        registerPin(_1C0, new InputPin(this, "1C0"));
        registerPin(_1Y, new OutputPin(this, "1Y"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_2Y, new OutputPin(this, "2Y"));
        registerPin(_2C0, new InputPin(this, "2C0"));
        registerPin(_2C1, new InputPin(this, "2C1"));
        registerPin(_2C2, new InputPin(this, "2C2"));
        registerPin(_2C3, new InputPin(this, "2C3"));
        registerPin(_A, new InputPin(this, "A"));
        registerPin(_2G_, new InputPin(this, "2G_"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate(_A, _B, _1C0, _1C1, _1C2, _1C3, _1G_, _1Y);
            this.updateGate(_A, _B, _2C0, _2C1, _2C2, _2C3, _2G_, _2Y);
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_1Y, Pin.PinState.NOT_CONNECTED);
        this.setPin(_2Y, Pin.PinState.NOT_CONNECTED);
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
