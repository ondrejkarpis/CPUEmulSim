package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;

/**
 * Výber a zvýraznenie objektov na ploche.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public abstract class HighlightGroup extends Group implements Selectable {

    private Pane cachedDescription;
    private boolean isSelectable;
    private boolean isSelected;

    protected HighlightGroup() {
        this.isSelectable = true;

		EventHandler<MouseEvent> onMouseClickEventHandler = (event) -> {
            if (!event.isPrimaryButtonDown()) return;
            Board board = getBoard();

			//ak je objekt na ploche
			if(board != null) {
				//ak je stlaceny shift -> pridanie do vyberu
				if (event.isShiftDown()) {
					board.addSelect(this);
				} else {
					//ak nie je shift -> vyber iba tento jeden item
					board.clearSelect();
					board.addSelect(this);
				}

			}
		};

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, onMouseClickEventHandler);

	}

    /**
     * Vráti popis objektu, ak je cachovaný, inak null.
     *
     * @return Panel s popisom objektu.
     */
    public Pane getDescription(){
        if (this.cachedDescription != null) return this.cachedDescription;
        return null;
    }

    /**
     * Cachovanie panelu s popisom.
     *
     * @param descriptionPane Panel s popisom.
     */
    protected void cacheDescription(Pane descriptionPane) {
        this.cachedDescription = descriptionPane;
    }

    @Override
	public void select(){
        this.isSelected = true;
    }

	@Override
	public void deselect(){
        this.isSelected = false;
    }

	@Override
	public void delete(){
        this.isSelected = false;
    }

    @Override
    public boolean isSelectable() {
        return this.isSelectable;
    }

    @Override
    public void setSelectable(boolean newValue) {
        this.isSelectable = newValue;
    }

    /**
     * Kontrola, či je objekt vybratý.
     *
     * @return True ak je, false inak.
     */
    public boolean isSelected() {
        return this.isSelected;
    }
}