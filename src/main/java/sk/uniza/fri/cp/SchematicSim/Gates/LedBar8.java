package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * 8-bitový výstupný zobrazovač (8 LED) pre inštrukciu {@code OUT}.
 * Stav každej LED závisí IBA od stavu príslušného vstupného pinu D0..D7 - LED vždy priamo
 * odzrkadľujú aktuálnu hodnotu na vstupe, bez ohľadu na riadiaci signál {@code IW_}.
 * Pri inštrukcii OUT na zbernici DataBus8 rozvádza hodnotu na vstupy LED práve počas
 * aktívneho {@code IW_}; samotné LED žiadny latch nemajú. LED sú čisto pasívne - zariadenie
 * nemá žiadne výstupné vývody, nikdy nehynie na zbernicu, a preto nemôže spôsobiť skrat.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class LedBar8 extends GateSymbol {

    private static final int GRID_WIDTH = 5;
    private static final int GRID_HEIGHT = 10;

    private static final Color LED_ON = Color.RED;
    private static final Color LED_OFF = Color.DARKGRAY;

    private Pin[] dataPins;
    private Pin pinIW_;

    private int currentValue = 0;
    private Circle[] leds;
    private Text valueLabel;

    /** Konštruktor pre paletku (ItemPicker). */
    public LedBar8() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public LedBar8(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new ArrayList<>();

        dataPins = new Pin[8];
        for (int index = 0; index < 8; index++) {
            dataPins[index] = new InputPin(this, "D" + index, 0, index, Side.LEFT);
            pins.add(dataPins[index]);
        }
        pinIW_ = new InputPin(this, "IW_", 0, 8, Side.LEFT);
        pins.add(pinIW_);

        return pins;
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        Rectangle body = new Rectangle(GRID_WIDTH * cell, GRID_HEIGHT * cell, Color.LIGHTPINK);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        Text title = new Text("LED 8");
        title.setLayoutX(cell * 0.6);
        title.setLayoutY(cell * 1.2);

        leds = new Circle[8];
        Pane bodyPane = new Pane(body, title);
        double radius = cell * 0.28;
        for (int index = 0; index < 8; index++) {
            Circle led = new Circle(cell * 2.5, cell * (1.4 + index * 0.95), radius, LED_OFF);
            led.setStroke(Color.BLACK);
            leds[index] = led;
            bodyPane.getChildren().add(led);
        }

        valueLabel = new Text("0x00");
        valueLabel.setLayoutX(cell * 0.6);
        valueLabel.setLayoutY(cell * GRID_HEIGHT - cell * 0.6);
        bodyPane.getChildren().add(valueLabel);

        return bodyPane;
    }

    @Override
    public void simulate() {
        // LED vždy priamo zrkadlia stav vstupných pinov, bez ohľadu na stav IW_
        int value = readData();
        if (value != currentValue) {
            currentValue = value;
            refreshVisual();
        }
    }

    @Override
    public void reset() {
        if (getPins() == null) return;
        for (Pin pin : getPins()) pin.setState(Pin.PinState.NOT_CONNECTED);
    }

    private int readData() {
        int value = 0;
        for (int index = 0; index < 8; index++) {
            if (isHigh(dataPins[index])) value |= 1 << index;
        }
        return value & 0xFF;
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy, aby sa pri rýchlej simulácii
     * FX vlákno nezahltilo radom a aplikácia (klávesy F4/F5/F10...) ostala odozvá.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (leds == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            for (int index = 0; index < 8; index++) {
                leds[index].setFill(((currentValue & (1 << index)) != 0) ? LED_ON : LED_OFF);
            }
            if (valueLabel != null) {
                valueLabel.setText(String.format("0x%02X", currentValue));
            }
        });
    }

    public int getCurrentValue() {
        return currentValue;
    }

    @Override
    public int getGridWidth() {
        return GRID_WIDTH;
    }

    @Override
    public int getGridHeight() {
        return GRID_HEIGHT;
    }

    @Override
    public String getName() {
        return "LED 8";
    }

    @Override
    public String getShortDescription() {
        return "8 LED výstup pre OUT - LED vždy zobrazujú stav svojho pinu, IW_ nič nehradí";
    }
}