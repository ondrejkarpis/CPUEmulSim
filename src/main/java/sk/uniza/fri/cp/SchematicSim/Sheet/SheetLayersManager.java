package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Wire.Joint;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;
import sk.uniza.fri.cp.SchematicSim.Wire.WireJunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Správca vrstiev plochy schémy. Nahrádza {@code BoardLayersManager} z BreadboardSim -
 * narozdiel od neho má iba 3 vrstvy (pozadie, súčiastky, vodiče), pretože odpadá
 * samostatná vrstva komponentov/breadboardov.
 *
 * @author Tomáš Hianik (pôvodný autor BoardLayersManager), adaptácia pre SchematicSim
 */
public class SheetLayersManager {

    private final Group layers;

    private final Pane backgroundLayer;
    private final Pane gatesLayer;
    private final Pane wiresLayer;

    private final ArrayList<GateSymbol> gates;
    private final ArrayList<Wire> wires;

    SheetLayersManager(Pane background) {
        this.backgroundLayer = background;
        this.gatesLayer = new Pane();
        this.wiresLayer = new Pane();
        this.gates = new ArrayList<>();
        this.wires = new ArrayList<>();

        this.gatesLayer.setMinWidth(this.backgroundLayer.getBoundsInParent().getWidth());
        this.gatesLayer.setMinHeight(this.backgroundLayer.getBoundsInParent().getHeight());

        this.layers = new Group(backgroundLayer, gatesLayer, wiresLayer);

        this.gatesLayer.setPickOnBounds(false);
        this.wiresLayer.setPickOnBounds(false);
    }

    private int getGateId() {
        for (int id = 0; ; id++) {
            boolean found = true;
            for (GateSymbol gate : gates) {
                if (gate.getId() != null && gate.getId().equals("g" + id)) {
                    found = false;
                    break;
                }
            }
            if (found) return id;
        }
    }

    public boolean add(Object object) {
        if (object instanceof Wire) {
            Wire wire = (Wire) object;
            this.wiresLayer.getChildren().add(wire);
            this.wires.add(wire);
            return true;
        }

        if (object instanceof GateSymbol) {
            GateSymbol gate = (GateSymbol) object;
            if (gate.getId() == null) gate.setId("g" + getGateId());
            this.gatesLayer.getChildren().add(gate);
            this.gates.add(gate);
            return true;
        }

        return false;
    }

    public boolean remove(Object object) {
        if (object instanceof Wire) {
            Wire wire = (Wire) object;
            this.wiresLayer.getChildren().remove(wire);
            this.wires.remove(wire);
            return true;
        }

        if (object instanceof WireJunction) {
            ((WireJunction) object).delete();
            return true;
        }

        if (object instanceof Joint) {
            ((Joint) object).getWire().removeJoint((Joint) object);
            return true;
        }

        if (object instanceof GateSymbol) {
            GateSymbol gate = (GateSymbol) object;
            this.gatesLayer.getChildren().remove(gate);
            this.gates.remove(gate);
            return true;
        }

        return false;
    }

    Pane getLayer(String name) {
        switch (name) {
            case "background": return backgroundLayer;
            case "gates": return gatesLayer;
            case "wires": return wiresLayer;
        }
        return backgroundLayer;
    }

    Group getLayers() {
        return layers;
    }

    public List<GateSymbol> getGates() {
        return new ArrayList<>(this.gates);
    }

    public List<Wire> getWires() {
        return new ArrayList<>(this.wires);
    }

    /**
     * Odstránenie všetkých objektov na ploche.
     */
    public void clear() {
        new ArrayList<>(this.wires).forEach(Wire::delete);
        new ArrayList<>(this.gates).forEach(GateSymbol::delete);
    }
}
