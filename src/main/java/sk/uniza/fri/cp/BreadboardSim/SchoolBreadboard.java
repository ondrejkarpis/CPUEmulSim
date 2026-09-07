package sk.uniza.fri.cp.BreadboardSim;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Components.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Objekt vývojovej dosky. Skladá sa z komponentov podľa predlohy fyzickej dosky.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class SchoolBreadboard extends Item {

    public static final Color BACKGROUND_COLOR = Color.rgb(97, 139, 111); // 68, 117, 84
    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 46;

    private BusInterface busInterface;
    private Breadboard breadboard;
    private HexSegmentsPanel hexSegmentsPanel;
    private NumKeys numKeys;
    private Probe probe;

    /**
     * Konštruktor pre itemPicker.
     */
    public SchoolBreadboard() {
    }

    public SchoolBreadboard(Board board) {
        super(board);

		GridSystem grid =  board.getGrid();

        int widthPx = grid.getSizeX() * GRID_WIDTH;
        int heightPx = grid.getSizeY() * GRID_HEIGHT;
        Rectangle background = new Rectangle(widthPx, heightPx, BACKGROUND_COLOR);
        background.setArcWidth(grid.getSizeX());
		background.setArcHeight(grid.getSizeY());

        this.busInterface = new BusInterface(board);
        this.busInterface.makeImmovable();
        this.busInterface.setSelectable(false);
		this.busInterface.setLayoutX(grid.getSizeX() * 2);

		this.breadboard = new Breadboard(board);
		this.breadboard.makeImmovable();
        this.breadboard.setSelectable(false);
		this.breadboard.setLayoutX(grid.getSizeX() * 2);
		this.breadboard.setLayoutY(busInterface.getGridHeight());

		this.hexSegmentsPanel = new HexSegmentsPanel(board);
		this.hexSegmentsPanel.makeImmovable();
        this.hexSegmentsPanel.setSelectable(false);
		this.hexSegmentsPanel.setLayoutX(grid.getSizeX() * 2);
        this.hexSegmentsPanel.setLayoutY(busInterface.getGridHeight() + breadboard.getGridHeight() + grid.getSizeY());

		this.numKeys = new NumKeys(board);
		this.numKeys.makeImmovable();
        this.numKeys.setSelectable(false);
		this.numKeys.setLayoutX(grid.getSizeX() * 2 + breadboard.getGridWidth() - numKeys.getGridWidth());
		this.numKeys.setLayoutY(busInterface.getGridHeight() + breadboard.getGridHeight());

        this.probe = new Probe(board);
        this.probe.makeImmovable();
        this.probe.setSelectable(false);
        this.probe.setLayoutX(grid.getSizeX() * 27);
        this.probe.setLayoutY(busInterface.getGridHeight() + breadboard.getGridHeight() + grid.getSizeY());

        this.getChildren().addAll(background, busInterface, breadboard, hexSegmentsPanel, probe, numKeys);
    }

    /**
     * Vytvorí a vráti zoznam komponentov dosky.
     *
     * @return Zoznam komponentov, ktoré doska obsahuje.
     */
    public List<Component> getComponents() {
        List<Component> list = new LinkedList<>();
        list.add(this.busInterface);
        list.add(this.breadboard);
        list.addAll(this.hexSegmentsPanel.getComponents());
        list.add(this.numKeys);
        list.add(this.probe);
        return list;
    }

    public BusInterface getBusInterface() {
        return busInterface;
    }

    public Breadboard getBreadboard() {
        return breadboard;
    }

    public HexSegmentsPanel getHexSegmentsPanel() {
        return hexSegmentsPanel;
    }

    public NumKeys getNumKeys() {
        return numKeys;
    }

    public Probe getProbe() {
        return probe;
    }

    /**
     * Šírka dosky v mriežkových jednotkách.
     *
     * @return Šírka dosky.
     */
    public int getGridWidth() {
        return GRID_WIDTH;
    }

    /**
     * Výška dosky v mriežkových jednotkách.
     *
     * @return Výška dosky.
     */
    public int getGridHeight() {
        return GRID_HEIGHT;
    }

	@Override
	public void moveTo(int gridPosX, int gridPosY) {
		super.moveTo(gridPosX, gridPosY);

		this.busInterface.setGridPos(getGridPosX(), getGridPosY());
		this.breadboard.setGridPos(getGridPosX(), getGridPosY());
		this.hexSegmentsPanel.setGridPos(getGridPosX(), getGridPosY());
		this.numKeys.setGridPos(getGridPosX(), getGridPosY());

		this.busInterface.updateConnectedDevicesPosition();
		this.breadboard.updateConnectedDevicesPosition();
		this.hexSegmentsPanel.updateConnectedDevicesPosition();
		this.numKeys.updateConnectedDevicesPosition();
	}

	@Override
    public void delete() {
        if (!this.getId().equalsIgnoreCase("sb0"))
            super.delete();
    }

    @Override
    public Pane getImage() {
        return new Pane(new ImageView(new Image("/icons/components/schoolBoard.png")));
    }
}