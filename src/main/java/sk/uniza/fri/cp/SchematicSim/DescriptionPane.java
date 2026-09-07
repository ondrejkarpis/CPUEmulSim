package sk.uniza.fri.cp.SchematicSim;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Panel s popisom vybraného objektu. Zjednodušené prevzatie BreadboardSim.DescriptionPane -
 * bez zámku (lock ikony), tá bola čisto kozmetická a nesúvisí s architektúrou schémy.
 * Dá sa doplniť späť podľa potreby.
 *
 * @author Tomáš Hianik (pôvodný autor), zjednodušená adaptácia pre SchematicSim
 */
public class DescriptionPane extends ScrollPane {

    private final StackPane stackPane;

    public DescriptionPane() {
        this.stackPane = new StackPane(new Pane());
        this.setFitToHeight(true);
        this.setFitToWidth(true);
        this.setContent(this.stackPane);
    }

    public void setDescription(Selectable item) {
        if (item.getDescription() != null) {
            this.stackPane.getChildren().set(0, item.getDescription());
        }
    }

    public void clear() {
        this.stackPane.getChildren().set(0, new Pane());
    }
}
