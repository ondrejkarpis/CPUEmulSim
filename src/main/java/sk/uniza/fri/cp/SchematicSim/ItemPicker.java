package sk.uniza.fri.cp.SchematicSim;

import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;

/**
 * Panel s výberom súčiastok, ktoré je možné pridať na plochu schémy ťahaním.
 * Nahrádza {@code ItemPicker} z BreadboardSim - odpadá {@code componentsPane}
 * (breadboardy/komponenty) aj prepínacie tlačidlá, keďže existuje iba jeden druh
 * umiestniteľných objektov - logické súčiastky.
 *
 * @author Tomáš Hianik (pôvodný autor), zjednodušenie pre SchematicSim
 */
public class ItemPicker extends VBox {

    private final ScrollPane contentPane;
    private final FlowPane gatesPane;

    public ItemPicker() {
        this.contentPane = new ScrollPane();
        VBox.setVgrow(this.contentPane, Priority.ALWAYS);

        this.gatesPane = new FlowPane();
        this.gatesPane.setVgap(5);
        this.gatesPane.setHgap(5);

        this.contentPane.setContent(this.gatesPane);
        this.contentPane.setFitToWidth(true);

        this.getChildren().add(this.contentPane);
    }

    /**
     * Registrácia novej súčiastky do paletky. Volajte napr. s {@code new AndGate()},
     * {@code new OrGate()}, {@code new NotGate()} (bezparametrické konštruktory).
     */
    public void registerItem(GateSymbol item) {
        gatesPane.getChildren().add(item);

        item.addEventFilter(MouseEvent.DRAG_DETECTED, event -> item.startFullDrag());
    }
}
