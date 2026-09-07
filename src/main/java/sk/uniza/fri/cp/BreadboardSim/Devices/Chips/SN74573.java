package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 74573
 * 8-bit register
 *
 * @author Tomáš Hianik
 * @created 13.4.2017.
 */
public class SN74573 extends Chip {
    private static final String NAME = "SN74573";
    private static final String SHORT_DESCRIPTION = "8-bit register";
    private static final int PINS_COUNT = 20;
    private static final int _OE_ = 1;
    private static final int _1D = 2;
    private static final int _2D = 3;
    private static final int _3D = 4;
    private static final int _4D = 5;
    private static final int _5D = 6;
    private static final int _6D = 7;
    private static final int _7D = 8;
    private static final int _8D = 9;
    private static final int _GND = 10;
    private static final int _LE = 11;
    private static final int _8Q = 12;
    private static final int _7Q = 13;
    private static final int _6Q = 14;
    private static final int _5Q = 15;
    private static final int _4Q = 16;
    private static final int _3Q = 17;
    private static final int _2Q = 18;
    private static final int _1Q = 19;
    private static final int _VCC = 20;

    private static final int[] inputs = {_1D, _2D, _3D, _4D, _5D, _6D, _7D, _8D};
    private static final int[] outputs = {_1Q, _2Q, _3Q, _4Q, _5Q, _6Q, _7Q, _8Q};
    private final boolean[] savedData = new boolean[8];

    //inspektor
    private volatile Stage inspectionStage;
    private Text[] showedBits;

    public SN74573() {
        super(PINS_COUNT);
    }

    public SN74573(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate() {
        if (this.isHigh(_LE)) {
            //povoleny zapis
            for (int i = 0; i < 8; i++) {
                this.savedData[i] = this.isHigh(inputs[i]);
            }

            updateInspector();
        }

        if (this.isHigh(_OE_)) {
            for (int i = 0; i < 8; i++) {
                this.setPin(outputs[i], Pin.PinState.HIGH_IMPEDANCE);
            }
        } else if (this.isLow(_OE_)) {
            //povoleny vystup
            for (int i = 0; i < 8; i++) {
                this.setPin(outputs[i], savedData[i] ? Pin.PinState.HIGH : Pin.PinState.LOW);
            }
        }

    }

    private void updateInspector() {
        if (this.inspectionStage != null && this.inspectionStage.isShowing()) {
            if (Platform.isFxApplicationThread()) {
                for (int i = 0; i < 8; i++) {
                    if (this.showedBits != null)
                        this.showedBits[i].setText(this.savedData[i] ? "H" : "L");
                }
            } else {
                Platform.runLater(() -> {
                    for (int i = 0; i < 8; i++) {
                        if (this.showedBits != null)
                            this.showedBits[i].setText(this.savedData[i] ? "H" : "L");
                    }
                });
            }
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_OE_, new InputPin(this, "OE_"));
        registerPin(_1D, new InputPin(this, "1D"));
        registerPin(_2D, new InputPin(this, "2D"));
        registerPin(_3D, new InputPin(this, "3D"));
        registerPin(_4D, new InputPin(this, "4D"));
        registerPin(_5D, new InputPin(this, "5D"));
        registerPin(_6D, new InputPin(this, "6D"));
        registerPin(_7D, new InputPin(this, "7D"));
        registerPin(_8D, new InputPin(this, "8D"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_LE, new InputPin(this, "LE"));
        registerPin(_8Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "8Q"));
        registerPin(_7Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "7Q"));
        registerPin(_6Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "6Q"));
        registerPin(_5Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "5Q"));
        registerPin(_4Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "4Q"));
        registerPin(_3Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "3Q"));
        registerPin(_2Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "2Q"));
        registerPin(_1Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "1Q"));
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
        this.setPin(_1Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_2Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_3Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_4Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_5Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_6Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_7Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_8Q, Pin.PinState.NOT_CONNECTED);

        for (int i = 0; i < 8; i++) {
            this.savedData[i] = false;
        }

        updateInspector();
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

    @Override
    protected Stage getInspectionWindow() {
        if (this.inspectionStage != null) return inspectionStage;

        this.inspectionStage = super.getInspectionWindow();

        Group textsGroup = new Group();
        this.showedBits = new Text[8];

        //z obr. IC
        double padding = 10;
        double pinWidth = 15;
        double pinMargin = 6;
        double icHeight = 70;

        //data
        for (int i = 0; i < 8; i++) {
            this.showedBits[i] = new Text(this.savedData[i] ? "H" : "L");
            textsGroup.getChildren().add(this.showedBits[i]);

            this.showedBits[i].setLayoutX(padding + i * (4 + pinWidth + 2 * pinMargin));
        }

        //posunutie zobrazenia dat do stredu IC
        textsGroup.setTranslateY(-(icHeight));

        VBox root = ((VBox) inspectionStage.getScene().getRoot());
        root.getChildren().add(textsGroup);

        this.inspectionStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            this.showedBits = null;
            this.inspectionStage = null;
        });

        return this.inspectionStage;
    }
}
