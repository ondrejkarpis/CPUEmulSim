package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 74125
 * 4x Bus buffer s 3-stavovými výstupmi
 *
 * @author Tomáš Hianik
 * @created 9.4.2017.
 */
public class SN74125 extends Chip {

    private static final String NAME = "SN74125";
    private static final String SHORT_DESCRIPTION = "4x Bus buffer s 3-stavovými výstupmi";
    private static final int PINS_COUNT = 14;
    private static final int _1G_ = 1;
    private static final int _1A = 2;
    private static final int _1Y = 3;
    private static final int _2G_ = 4;
    private static final int _2A = 5;
    private static final int _2Y = 6;
    private static final int _GND = 7;
    private static final int _3Y = 8;
    private static final int _3A = 9;
    private static final int _3G_ = 10;
    private static final int _4Y = 11;
    private static final int _4A = 12;
    private static final int _4G_ = 13;
    private static final int _VCC = 14;

    public SN74125() {
        super(PINS_COUNT);
    }

    public SN74125(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate(int A, int G, int Y) {
        if (this.isLow(G)) {
            if (this.isHigh(A)) {
                this.setPin(Y, Pin.PinState.HIGH);
            } else if (this.isLow(A)) {
                this.setPin(Y, Pin.PinState.LOW);
            }
        } else if (this.isHigh(G)) {
            this.setPin(Y, Pin.PinState.HIGH_IMPEDANCE);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_1G_, new InputPin(this, "1G_"));
        registerPin(_1A, new InputPin(this, "1A"));
        registerPin(_1Y, new OutputPin(this, Pin.PinDriver.TRI_STATE, "1Y"));
        registerPin(_2G_, new InputPin(this, "2G_"));
        registerPin(_2A, new InputPin(this, "2A"));
        registerPin(_2Y, new OutputPin(this, Pin.PinDriver.TRI_STATE, "2Y"));
        registerPin(_GND, new InputPin(this, "GGN"));
        registerPin(_3Y, new OutputPin(this, Pin.PinDriver.TRI_STATE, "3Y"));
        registerPin(_3A, new InputPin(this, "3A"));
        registerPin(_3G_, new InputPin(this, "3G_"));
        registerPin(_4Y, new OutputPin(this, Pin.PinDriver.TRI_STATE, "4Y"));
        registerPin(_4A, new InputPin(this, "4A"));
        registerPin(_4G_, new InputPin(this, "4G_"));
        registerPin(_VCC, new InputPin(this, "VVC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate(_1A, _1G_, _1Y);
            this.updateGate(_2A, _2G_, _2Y);
            this.updateGate(_3A, _3G_, _3Y);
            this.updateGate(_4A, _4G_, _4Y);
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
