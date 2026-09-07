package sk.uniza.fri.cp.BreadboardSim;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Wire.Joint;

/**
 * Objekt s ktorým je možné pohybovať pomocou kurzora na ploche simulátora.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public abstract class Movable extends HighlightGroup {

	private int gridPosX;
	private int gridPosY;
	private Board board;

	private double nodeOffsetX = -1;
	private double nodeOffsetY = -1;

	private EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			if(!event.isPrimaryButtonDown()) return;

            nodeOffsetX = event.getSceneX() - getLayoutX() * board.getAppliedScale();
            nodeOffsetY = event.getSceneY() - getLayoutY() * board.getAppliedScale();

			event.consume();
		}
	};

	private EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			if(!event.isPrimaryButtonDown()) return;

			setCursor(Cursor.DEFAULT);

			//ak nebol nastaveny offset, zrejme nejde o kliknutie na objekt a tahanie ale vytvorenie noveho objektu
			//ten chceme chitit v strede
			if(nodeOffsetX == -1){
                Point2D layout = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
                Bounds boundsInParent = getBoundsInParent();
                nodeOffsetX = event.getSceneX() - (layout.getX() - boundsInParent.getWidth() / 2.0 - boundsInParent.getMinX()) * board.getAppliedScale();
                nodeOffsetY = event.getSceneY() - (layout.getY() - boundsInParent.getHeight() / 2.0 - boundsInParent.getMinY()) * board.getAppliedScale();
            }

			GridSystem grid = board.getGrid();
			int gridX;
			int gridY;

			if(event.getSource() instanceof Joint) {
                //jointy chytame za stred
                Point2D boardXY = board.sceneToBoard(event.getSceneX(), event.getSceneY());
                gridX = (int) (Math.round(boardXY.getX() / grid.getSizeX()) * grid.getSizeX()) / grid.getSizeX();
                gridY = (int) (Math.round(boardXY.getY() / grid.getSizeY()) * grid.getSizeY()) / grid.getSizeY();
            } else {
                gridX = (int) (Math.round((event.getSceneX() - nodeOffsetX) / grid.getSizeX() / board.getAppliedScale()) * grid.getSizeX()) / grid.getSizeX();
                gridY = (int) (Math.round((event.getSceneY() - nodeOffsetY) / grid.getSizeY() / board.getAppliedScale()) * grid.getSizeY()) / grid.getSizeY();
            }

			//ak sa pozicia zmenila
			if (gridPosX != gridX || gridPosY != gridY) {
				if( gridX < 0)
					gridX = 0;
				else if( gridX * grid.getSizeX() + getBoundsInParent().getWidth() > board.getWidthPx() )
					gridX = (int) (Math.round(board.getWidthPx() - getBoundsInParent().getWidth()) / grid.getSizeX());

				if( gridY < 0)
					gridY = 0;
				else if( gridY * grid.getSizeY() + getBoundsInParent().getHeight() > board.getHeightPx() )
					gridY = (int) (Math.round(board.getHeightPx() - getBoundsInParent().getHeight()) / grid.getSizeY());


				moveTo(gridX, gridY);


				gridPosX = gridX;
				gridPosY = gridY;
			}

			event.consume();
		}
	};

	/**
	 * 
	 * @param board
	 */
	public Movable(Board board){
		this.board = board;

		this.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
		this.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);

	}

	/**
	 * Zrusi moznost presuvat objekt mysou.
	 */
	public void makeImmovable(){
		this.removeEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
		this.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
	}

	/**
	 * Relativne posunutie objektu v jednotkach pixelov k aktualnej pozicii na scene.
	 * @param deltaX X-ova zmena pozicie na mriezke oproti aktualnej
	 * @param deltaY Y-ova zmena pozicie na mriezke oproti aktualnej
	 */
	public void moveBy(double deltaX, double deltaY){
        this.gridPosX += Math.round(deltaX) / board.getGrid().getSizeX();
        this.gridPosY += Math.round(deltaY) / board.getGrid().getSizeY();

        this.setLayoutX(getLayoutX() + deltaX);
        this.setLayoutY(getLayoutY() + deltaY);
	}

	/**
	 * Relativne posunutie objektu v jednotkach mriezky k aktualnej pozicii na mriezke.
	 * @param deltaX X-ova zmena pozicie na mriezke oproti aktualnej
	 * @param deltaY Y-ova zmena pozicie na mriezke oproti aktualnej
	 */
	public void moveBy(int deltaX, int deltaY){
		this.gridPosX += deltaX;
		this.gridPosY += deltaY;
        Point2D point = board.getGrid().gridToPixel(this.gridPosX, this.gridPosY);
        this.relocate(point.getX(), point.getY());
	}

	/**
	 * Posunutie na poziciu X,Y vramci plochy board.
	 * @param point Struktira s novymi suradnicami objektu.
	 */
	public void moveTo(Point2D point){
		this.relocate(point.getX(), point.getY());
	}

	/**
	 * Posunutie na poziciu X,Y vramci plochy board.
	 * @param posX Suradnica X na ploche board
	 * @param posY Suradnica Y na ploche board
	 */
	public void moveTo(double posX, double posY){
        this.setLayoutX(posX);
        this.setLayoutY(posY);
		//this.relocate(posX, posY);
	}

	/**
	 * Posunutie objektu na poziciu X,Y v mriezkovej sustave.
	 * @param gridPosX Pozicia X na mriezke
	 * @param gridPosY Pozicia Y na mriezke
	 */
	public void moveTo(int gridPosX, int gridPosY){
		this.gridPosX = gridPosX;
		this.gridPosY = gridPosY;

        Point2D point = getBoard().getGrid().gridToPixel(gridPosX, gridPosY);
        this.setLayoutX(point.getX());
		this.setLayoutY(point.getY());
		//this.relocate(point.getX(), point.getY());
	}

	/**
	 * Vrati instanciu triedy Board na ktorej je zorbazeny objekt.
	 * @return Board na ktorom je objekt umiestneny
	 */
	public Board getBoard(){
		return this.board;
	}

    /**
     * Vráti X-ovú pozíciu na ploche v jednotkách mriežky.
     *
     * @return X-ová pozícia na ploche v jednotkách mriežky.
     */
    public int getGridPosX(){ return gridPosX; }

    /**
     * Vráti Y-ovú pozíciu na ploche v jednotkách mriežky.
     *
     * @return Y-ová pozícia na ploche v jednotkách mriežky.
     */
    public int getGridPosY(){ return gridPosY; }

	/**
	 * Aktualizácia premenných pozície. Nepremiestňuje objekt a nemusí odpovedať naozajstrej pozícií v rámci plochy.
	 * Slúži skôr na aktualizáciu offsetov v rámci objektu (napr. schoolboard sa skladá z viacerých komponentov, ktorým
	 * sa neaktualizuje pozícia)
	 *
	 * @param gridPosX X-ová pozícia
	 * @param gridPosY Y-ová pozícia
	 */
	public void setGridPos(int gridPosX, int gridPosY){
		this.gridPosX = gridPosX;
		this.gridPosY = gridPosY;
	}

	@Override
	public void delete() {
		super.delete();

		this.getBoard().removeItem(this);
	}

}