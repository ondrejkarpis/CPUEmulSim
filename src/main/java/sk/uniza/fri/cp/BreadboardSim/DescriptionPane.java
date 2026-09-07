package sk.uniza.fri.cp.BreadboardSim;

import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Panel s popisom objektu.
 *
 * @author Tomáš Hianik
 * @created 7.5.2017
 */
public class DescriptionPane extends ScrollPane {

    private StackPane stackPane;
    private ImageView lockImageView;
    private Image lockImage;
    private Image unlockImage;

    private boolean locked = false;
    private boolean center = false;

    /**
     * Panel na ktorom sa zobrazuje popis vybraného objektu.
     */
    public DescriptionPane() {
        this.stackPane = new StackPane();
        this.lockImageView = new ImageView();
        this.lockImage = new Image("/icons/lock/locked.png");
        this.unlockImage = new Image("/icons/lock/unlocked.png");

        this.setFitToHeight(true);
        this.setFitToWidth(true);

        this.lockImageView.setImage(unlockImage);
        this.stackPane.getChildren().addAll(new Pane(), this.lockImageView);
        this.setContent(this.stackPane);

        StackPane.setAlignment(this.lockImageView, Pos.TOP_LEFT);

        this.lockImageView.setOnMouseClicked(event -> {
            this.locked = !this.locked;
            if (this.locked) {
                this.lockImageView.setImage(this.lockImage);
            } else {
                this.lockImageView.setImage(this.unlockImage);
            }
        });

        this.hvalueProperty().addListener((observable, oldValue, newValue) -> {
            //centrovanie po zmene obsahu
            if (this.center) this.setHvalue(0.5);
            this.center = false;

            double hmin = this.getHmin();
            double hmax = this.getHmax();
            double hvalue = this.getHvalue();
            double contentWidth = this.stackPane.getLayoutBounds().getWidth();
            double viewportWidth = this.getViewportBounds().getWidth();

            double hoffset =
                    Math.max(0, contentWidth - viewportWidth) * (hvalue - hmin) / (hmax - hmin);

            this.lockImageView.setTranslateX(hoffset);
        });

        this.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            double vmin = this.getVmin();
            double vmax = this.getVmax();
            double vvalue = this.getVvalue();
            double contentHeight = this.stackPane.getLayoutBounds().getHeight();
            double viewportHeight = this.getViewportBounds().getHeight();

            double voffset =
                    Math.max(0, contentHeight - viewportHeight) * (vvalue - vmin) / (vmax - vmin);

            this.lockImageView.setTranslateY(voffset);
        });
    }

    /**
     * Nastavenie popisu podľa objektu. Ak je plocha uzamknutá, nemení sa zobrazenie.
     *
     * @param item Objekt, ktorého popis sa má zobraziť.
     */
    public void setDescription(Selectable item) {
        if (!this.locked && item.getDescription() != null) {
            this.stackPane.getChildren().set(0, item.getDescription());
            this.setHvalue(0.5);
            this.center = true;
        }
    }

    /**
     * Vyčistenie plochy.
     */
    public void clear() {
        if (!this.locked)
            this.stackPane.getChildren().set(0, new Pane());
    }
}
