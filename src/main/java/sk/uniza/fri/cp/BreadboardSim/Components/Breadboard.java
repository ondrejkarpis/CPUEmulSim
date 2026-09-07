package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketsFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Komponent kontaktnej dosky.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class Breadboard extends Component {

    private static final Color BACKGROUND_COLOR = Color.rgb(217, 217, 217);

    /**
     * Konštruktor pre ItemPicker.
     */
    public Breadboard() {
    }

    /**
     * Konštruktor pre vytovrenie objeku pre plochu simulátora.
     *
     * @param board Plocha simulátora.
     */
    public Breadboard(Board board){
        super(board);

        //grafika
        GridSystem grid = getBoard().getGrid();
        this.gridWidth = grid.getSizeX() * 66;
        this.gridHeight = grid.getSizeY() * 23;

        Rectangle background = new Rectangle(this.gridWidth, this.gridHeight, BACKGROUND_COLOR);
        Rectangle middleSpace = new Rectangle(this.gridWidth, grid.getSizeY(), Color.rgb(204, 201, 201));
        middleSpace.setLayoutY(grid.getSizeY() * 11);

        Group topPowerLines = generatePowerLines();
        topPowerLines.setLayoutX(grid.getSizeX() * 4);
        topPowerLines.setLayoutY(grid.getSizeY() * 2);

        Group topSockets = generate5VerticalSocketsGroups();
        topSockets.setLayoutX(grid.getSizeX() * 2);
        topSockets.setLayoutY(grid.getSizeX() * 6);

        Group bottomSockets = generate5VerticalSocketsGroups();
        bottomSockets.setLayoutX(grid.getSizeX() * 2);
        bottomSockets.setLayoutY(grid.getSizeX() * 13);

        Group bottomPowerLines = generatePowerLines();
        bottomPowerLines.setLayoutX(grid.getSizeX() * 4);
        bottomPowerLines.setLayoutY(grid.getSizeY() * 20);

        //popisky
        Group charLabelsLeft = generateAtoJLabels();
        charLabelsLeft.setLayoutX(grid.getSizeX());
        charLabelsLeft.setLayoutY(grid.getSizeY() * 6);

        Group charLabelsRight = generateAtoJLabels();
        charLabelsRight.setLayoutX(grid.getSizeX() * 65);
        charLabelsRight.setLayoutY(grid.getSizeY() * 6);

        Group numberLabelsTop = generateNumberLabels();
        numberLabelsTop.setLayoutX(grid.getSizeX() * 2);
        numberLabelsTop.setLayoutY(grid.getSizeY() * 4.5);

        Group numberLabelsBottom = generateNumberLabels();
        numberLabelsBottom.setLayoutX(grid.getSizeX() * 2);
        numberLabelsBottom.setLayoutY(grid.getSizeY() * 17.5);

        this.getChildren().addAll(background, middleSpace, topPowerLines, topSockets, bottomSockets, bottomPowerLines, charLabelsLeft, charLabelsRight, numberLabelsTop, numberLabelsBottom);
    }

    private Group generatePowerLines() {
        GridSystem grid = getBoard().getGrid();

        List<Socket> socketList = new LinkedList<>();
        //sokety
        Group vccSockets = SocketsFactory.getHorizontal(this, 50, 5, 1, socketList);

        Group gndSockets = SocketsFactory.getHorizontal(this, 50, 5, 1, socketList);
        gndSockets.setLayoutY(grid.getSizeY());

        this.addAllSockets(socketList);

		//ciarove oznacenie
        Line redLine = new Line(-grid.getSizeX(), grid.getSizeY() * 2, grid.getSizeX() * 59, grid.getSizeY() * 2);
        redLine.setStrokeWidth(2);
		redLine.setStroke(Color.RED);

        Line blueLine = new Line(-grid.getSizeX(), -grid.getSizeY(), grid.getSizeX() * 59, -grid.getSizeY());
        blueLine.setStrokeWidth(2);
		blueLine.setStroke(Color.BLUE);

		return new Group(vccSockets, gndSockets, redLine, blueLine);
	}

    private Group generate5VerticalSocketsGroups() {
        GridSystem grid = getBoard().getGrid();

        List<Socket> socketList = new LinkedList<>();
        Group sockets = new Group();

		for (int i = 0; i < 63; i++) {
            Group vertiacalLine = SocketsFactory.getVertical(this, 5, socketList);
            vertiacalLine.setLayoutX(i * grid.getSizeX());
			sockets.getChildren().add(vertiacalLine);
		}

        this.addAllSockets(socketList);

        return sockets;
	}

	private Group generateAtoJLabels(){
		GridSystem grid = getBoard().getGrid();

		Group AtoJLabels = new Group();
		char ch = 'A';
		int pos = 0;
		do {
			Label label = new Label(String.valueOf(ch));
			label.setFont(new Font("Lucida Console", grid.getSizeX()));
			label.setId("breadboardLabel");
			label.setPrefWidth(grid.getSizeX());
			label.setLayoutX(pos * grid.getSizeX() - grid.getSizeX() / 2.0);
			AtoJLabels.getChildren().add(label);

			if (pos != 4)
				pos++;
			else
				pos += 3;

			ch++;
		} while	(ch <= 'J');
		AtoJLabels.setRotate(-90);
		AtoJLabels.setLayoutX(grid.getSizeX() * -5.5 );
		AtoJLabels.setLayoutY(grid.getSizeY() * 5);

		return new Group(AtoJLabels);
	}

	private Group generateNumberLabels(){
		GridSystem grid = getBoard().getGrid();

		Group numberLabels = new Group();
		Label number = new Label("1");
		number.setFont(new Font("Lucida Console", grid.getSizeX()));
		number.setId("breadboardLabel");
		number.setPrefWidth(grid.getSizeX());
		number.setLayoutX(- grid.getSizeX() / 2.0);
		numberLabels.getChildren().add(number);

		for (int i = 5; i <= 60; i+=5) {
			number = new Label(String.valueOf(i));
			number.setFont(new Font("Lucida Console", grid.getSizeX()));
			number.setId("breadboardLabel");
			number.setPrefWidth(grid.getSizeX());
			number.setLayoutX((i-1) * grid.getSizeX() - grid.getSizeX() / 2.0);
			numberLabels.getChildren().add(number);
		}

		return numberLabels;
	}

    @Override
    public Pane getImage() {
        return new Pane(new ImageView(new Image("/icons/components/breadboard.png")));
    }
}