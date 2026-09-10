package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.SchematicSim.Item;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Sheet.SheetChangeEvent;
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Logická súčiastka na schéme. Nahrádza {@code Device}(+{@code Chip}) z BreadboardSim.
 * Narozdiel od pôvodného modelu tu nejde o reálne 74xx puzdro s napájaním (VCC/GND) -
 * ide o abstraktný logický symbol (AND/OR/NOT...), ktorý sa na plochu umiestňuje voľne,
 * bez kolíznej detekcie s breadboardom (o voľnosť buniek sa stará
 * {@link sk.uniza.fri.cp.SchematicSim.GridOccupancy} v {@link SchematicSheet}).
 *
 * @author Claude (návrh podľa SchematicSim architektúry, simulačné helpery prevzaté z pôvodného Device)
 */
public abstract class GateSymbol extends Item {

    private List<Pin> pins;

    /**
     * Bezparametrický konštruktor pre ItemPicker (paletku).
     */
    protected GateSymbol() {
        super();
        this.pins = null; // v paletke sa piny nevytvárajú, iba statický obrázok cez getImage()
    }

    protected GateSymbol(SchematicSheet sheet) {
        super(sheet);
        this.pins = createPins();
        this.getChildren().addAll(pins);
        this.getChildren().add(0, drawBody()); // telo pod pinmi
    }

    /**
     * Vytvorenie pinov súčiastky. Volá sa raz z konštruktora.
     */
    protected abstract List<Pin> createPins();

    /**
     * Grafická značka tela súčiastky (obdĺžnik/IEEE tvar s popiskom...).
     */
    protected abstract Pane drawBody();

    /**
     * Simulačná metóda - prepočíta výstupy na základe aktuálnych vstupov.
     */
    public abstract void simulate();

    /**
     * Resetovanie výstupov (napr. pri zastavení simulácie).
     */
    public abstract void reset();

    public abstract int getGridWidth();

    public abstract int getGridHeight();

    public abstract String getName();

    public abstract String getShortDescription();

    public List<Pin> getPins() {
        return pins;
    }

    /**
     * Nahradenie zoznamu pinov súčiastky (dynamická zmena počtu vstupov cez kontextové menu).
     * Musí byť v súlade s obsahom potomkov (children) - SchemeLoader indexuje piny podľa
     * tohto zoznamu, a preto poradie (vstupy, výstup) musí zostať stabilné.
     */
    protected final void replacePins(List<Pin> pins) {
        this.pins = pins;
    }

    /**
     * Vlastnosti súčiastky pre ukladanie schémy (názov → hodnota). Predvolene prázdne -
     * konfigurovateľné súčiastky (LED, prepínač, zbernice...) ju prekrývajú. SchemeLoader
     * ukladá výslednú mapu genericky, takže pridanie novej súčiastky nevyžaduje zmeny v loaderi.
     */
    public Map<String, String> saveProperties() {
        return Collections.emptyMap();
    }

    /**
     * Obnovenie vlastností súčiastky po načítaní schémy. Volá sa vždy po vytvorení hradla
     * reflexiou a pred pripájaním vodičov (zbernica si tak najskôr vytvorí svoje odbočky,
     * aby sa na ne dali pripojiť vodiče podľa indexu pinu). Predvolene no-op.
     */
    public void loadProperties(Map<String, String> properties) {
    }

    public Point2D getGridPos() {
        return new Point2D(getGridPosX(), getGridPosY());
    }

    // === simulačné helpery, prevzaté z BreadboardSim.Device ===

    /**
     * Skontroluje a aktualizuje aktuálnu hodnotu na vstupnom pine podľa potenciálu, ku ktorému je pripojený.
     * Nepripojené vstupy sa (rovnako ako v origináli) správajú náhodne - modeluje to plávajúci vstup.
     */
    public boolean isHigh(Pin inputPin) {
        if (inputPin == null || !inputPin.isConnected()) return false;

        Potential.Value value = inputPin.getPotential() != null ? inputPin.getPotential().getValue() : Potential.Value.NC;

        if (value == Potential.Value.HIGH) {
            inputPin.setState(Pin.PinState.HIGH);
            return true;
        } else if (value == Potential.Value.LOW) {
            inputPin.setState(Pin.PinState.LOW);
        } else {
            if (Math.random() < 0.5) {
                inputPin.setState(Pin.PinState.HIGH);
                return true;
            } else {
                inputPin.setState(Pin.PinState.LOW);
            }
        }
        return false;
    }

    public boolean isLow(Pin inputPin) {
        if (inputPin == null || !inputPin.isConnected()) return false;

        Potential.Value value = inputPin.getPotential() != null ? inputPin.getPotential().getValue() : Potential.Value.NC;

        if (value == Potential.Value.LOW) {
            inputPin.setState(Pin.PinState.LOW);
            return true;
        } else if (value == Potential.Value.HIGH) {
            inputPin.setState(Pin.PinState.HIGH);
        } else {
            if (Math.random() < 0.5) {
                inputPin.setState(Pin.PinState.HIGH);
            } else {
                inputPin.setState(Pin.PinState.LOW);
                return true;
            }
        }
        return false;
    }

    /**
     * Nastavenie hodnoty na výstupnom pine a zaznamenanie zmenovej udalosti pre simuláciu,
     * ak sa hodnota oproti predošlému stavu zmenila.
     */
    public void setPin(Pin pin, Pin.PinState state) {
        if (pin.getState() != state) {
            pin.setState(state);
            postChangeEvent(pin, state);
        }
    }

    /**
     * Ako setPin(), ale udalosť sa vytvorí vždy, aj keď sa hodnota nezmenila.
     */
    public void setPinForce(Pin pin, Pin.PinState state) {
        pin.setState(state);
        postChangeEvent(pin, state);
    }

    private void postChangeEvent(Pin pin, Pin.PinState state) {
        Potential.Value value;
        switch (state) {
            case HIGH: value = Potential.Value.HIGH; break;
            case LOW: value = Potential.Value.LOW; break;
            default: value = Potential.Value.NC;
        }
        getSheet().addEvent(new SheetChangeEvent(pin, value));
    }

    @Override
    public void delete() {
        super.delete();
        // pri zmazaní súčiastky sa zmažú aj všetky na jej piny pripojené vodiče
        // (narozdiel od Socket.disconnect() v origináli tu niet samostatného komponentu,
        // na ktorom by mohol "osirelý" vodič ostať visieť)
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.getWireEnd() != null) pin.getWireEnd().getWire().delete();
            }
        }
    }
}
