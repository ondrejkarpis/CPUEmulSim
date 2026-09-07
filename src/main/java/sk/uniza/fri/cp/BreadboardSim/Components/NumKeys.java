package sk.uniza.fri.cp.BreadboardSim.Components;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketType;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketsFactory;

import java.util.List;

/**
 * Komponent s klavesnicou.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class NumKeys extends Component {

    private Socket[] columnSockets;
    private Socket[] rowSockets;
    private Button[][] buttons;

    //kontextove menu
    private ContextMenu contextMenu;
	private MenuItem miChangeButtonBehaviour;
	private boolean twoClick;
    private boolean leftMouseButtonDown;

    /**
     * Konštruktor pre itemPicker.
     */
    public NumKeys() {
    }

    /**
     * Konštruktor pre vytvorenie objektu určeného pre plochu siumlátora.
     *
     * @param board Plocha simulátora
     */
    public NumKeys(Board board) {
        super(board);

        columnSockets = new Socket[4];
        rowSockets = new Socket[4];
        buttons = new Button[4][4];

        //grafika
        GridSystem grid = getBoard().getGrid();

        this.gridWidth = grid.getSizeX() * 22;
        this.gridHeight = grid.getSizeY() * 17;
        Rectangle background = new Rectangle(this.gridWidth, this.gridHeight, SchoolBreadboard.BACKGROUND_COLOR);

        //sokety
        Group columnGroup = generateColumnSockets();
        columnGroup.setLayoutX(grid.getSizeX() * 6);
        columnGroup.setLayoutY(grid.getSizeY() * 2);

        Group rowGroup = generateRowSockets();
        rowGroup.setLayoutX(grid.getSizeX() * 15);
        rowGroup.setLayoutY(grid.getSizeY() * 2);

        //GND
        Group gndGroup = SocketsFactory.getHorizontalPower(this, 4, Potential.Value.LOW, getPowerSockets());
        Text rowText = Board.getLabelText("GND", grid.getSizeMin());
        rowText.setLayoutX(grid.getSizeX() * 4);
        rowText.setLayoutY(rowText.getBoundsInParent().getHeight()/2.0);
        gndGroup.getChildren().add(rowText);

        gndGroup.setLayoutX(grid.getSizeX() * 15);
        gndGroup.setLayoutY(grid.getSizeY());

        //+5V
        Group vccGroup = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.HIGH, getPowerSockets());
        Text vccText = Board.getLabelText("+5V", grid.getSizeMin());
        vccText.setLayoutX(grid.getSizeX()/2.0 - vccText.getBoundsInLocal().getWidth()/2.0);
        vccText.setLayoutY( grid.getSizeY() * 1.5);
        vccGroup.getChildren().add(vccText);

        vccGroup.setLayoutX(grid.getSizeX() * 11);
        vccGroup.setLayoutY(grid.getSizeY() * 2);

        //tlacitka
        Group buttonsGroup = new Group();

        String[] btnLabels = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                Button button = new Button(board, rowSockets[3 - y], columnSockets[x], y, x, btnLabels[x + y * 4]);
                button.setUserData(x + ", " + y);
                button.setLayoutX(button.getWidth() * x);
                button.setLayoutY(button.getHeight() * (3-y));

                buttons[x][y] = button;
                buttonsGroup.getChildren().add(button);
            }
        }
        buttonsGroup.setLayoutX(grid.getSizeX() * 5);
        buttonsGroup.setLayoutY(grid.getSizeY() * 4);

        this.getChildren().addAll(background, buttonsGroup, columnGroup, rowGroup, gndGroup, vccGroup);

        //registracia vsetkych soketov komponentu
        this.addAllSockets(getPowerSockets());
        this.addAllSockets(columnSockets);
        this.addAllSockets(rowSockets);


        //kontextove menu pre zmenu spravania tlacitok
        this.miChangeButtonBehaviour = new MenuItem("Dva kliky");
        this.miChangeButtonBehaviour.setOnAction(event -> {
            twoClick = !twoClick;
            miChangeButtonBehaviour.setText(twoClick?"Jeden klik":"Dva kliky");
        });

        this.contextMenu = new ContextMenu();
        this.contextMenu.getItems().add(this.miChangeButtonBehaviour);

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            leftMouseButtonDown = event.isSecondaryButtonDown();
            if(!leftMouseButtonDown && contextMenu.isShowing())
                contextMenu.hide();
        });

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if(leftMouseButtonDown) {
                this.contextMenu.show(this, event.getScreenX(), event.getScreenY());
                leftMouseButtonDown = false;
            }
        });
    }

	private Group generateColumnSockets(){

		GridSystem grid = getBoard().getGrid();

		Group columnGroup = new Group();

		for (int i = 0; i < 4; i++) {
            Socket socket = new Socket(this);
            socket.setType(SocketType.WEAK_OUT);
			socket.setPotential(Potential.Value.HIGH);

			socket.setLayoutX(grid.getSizeX() * i);

			Text numberText = new Text(String.valueOf(i+1));
			numberText.setFont(Font.font(grid.getSizeX()));
			numberText.setId("breadboardLabel");
			numberText.setStrokeWidth(0);
			numberText.setLayoutX(grid.getSizeX() * i - numberText.getBoundsInLocal().getWidth() / 2.0);
            numberText.setLayoutY(grid.getSizeY() * 1.5);

			columnSockets[i] = socket;
			columnGroup.getChildren().addAll(socket, numberText);
		}

		Text columnText = new Text("COLUMN");
		columnText.setFont(Font.font(grid.getSizeY()));
		columnText.setId("breadboardLabel");
		columnText.setStrokeWidth(0);
		columnText.setLayoutX(-columnText.getBoundsInParent().getWidth() - grid.getSizeX());
		columnText.setLayoutY(columnText.getBoundsInParent().getHeight()/2.0);
		columnGroup.getChildren().add(columnText);

		return columnGroup;
	}

    private Group generateRowSockets(){

        GridSystem grid = getBoard().getGrid();

        Group rowGroup = new Group();

        for (int i = 0; i < 4; i++) {
            Socket socket = new Socket(this);
            //socket.setType(SocketType.);
            socket.setLayoutX(grid.getSizeX() * (3-i) );
            socket.setUserData(i);

            Text numberText = new Text(String.valueOf(i+1));
            numberText.setFont(Font.font(grid.getSizeX()));
            numberText.setId("breadboardLabel");
            numberText.setStrokeWidth(0);
            numberText.setLayoutX(grid.getSizeX() * (3-i) - numberText.getBoundsInLocal().getWidth() / 2.0);
            numberText.setLayoutY(grid.getSizeY() * 1.5);

            rowSockets[3-i] = socket;
            rowGroup.getChildren().addAll(socket, numberText);
        }

        Text rowText = new Text("ROW");
        rowText.setFont(Font.font(grid.getSizeY()));
        rowText.setId("breadboardLabel");
        rowText.setStrokeWidth(0);
        rowText.setLayoutX(grid.getSizeX() * 4);
        rowText.setLayoutY(rowText.getBoundsInParent().getHeight()/2.0);
        rowGroup.getChildren().add(rowText);

        return rowGroup;
    }

    @Override
    public Pane getImage() {
        return new Pane(new ImageView(new Image("/icons/components/hwKeys.png")));
    }

    private class Button extends Device {
        private final Color DEFAULT_BUTTON_COLOR = Color.BLACK;
		private final Color DEFAULT_HOVER_BUTTON_COLOR = Color.DARKGRAY;
		private final Color PUSHED_BUTTON_COLOR = Color.RED;
		private final Color PUSHED_HOVER_BUTTON_COLOR = Color.DARKRED;

        private final Socket row;
        private final Socket column;

        private final int rowPos;
        private final int columnPos;

        private final InputPin rowInputPin;

		//eventy
		private boolean mouseHover;
        private volatile boolean pressed;

		//grafika
        private final Circle button;
        private final Group buttonBase;

        private final double width;
        private final double height;

		private EventHandler<MouseEvent> onMouseEnter = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(pressed)
                    setColor(PUSHED_HOVER_BUTTON_COLOR);
                else
                    setColor(DEFAULT_HOVER_BUTTON_COLOR);

                mouseHover = true;

                event.consume();
            }
        };

        private EventHandler<MouseEvent> onMouseExit = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(event.isPrimaryButtonDown())
                    setColor(PUSHED_BUTTON_COLOR);
                else if(pressed)
                    setColor(PUSHED_HOVER_BUTTON_COLOR);
                else
                    setColor(DEFAULT_BUTTON_COLOR);

            	mouseHover = false;

                event.consume();
            }
        };

        private EventHandler<MouseEvent> onMousePress = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(event.isPrimaryButtonDown() && !contextMenu.isShowing()) {
                    setColor(PUSHED_BUTTON_COLOR);

					pressed = !twoClick || !pressed;

					if (pressed)
                        simulate();//fireEvent(true);

                    event.consume();
                }
            }
        };

		private EventHandler<MouseEvent> onMouseRelease = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {

				if(!twoClick) pressed = false;

				if(!pressed) {
                    simulate();//fireEvent(false);

					if(mouseHover)
                        setColor(DEFAULT_HOVER_BUTTON_COLOR);
                    else
                        setColor(DEFAULT_BUTTON_COLOR);
                } else {
					if(mouseHover)
                        setColor(PUSHED_HOVER_BUTTON_COLOR);
                    else
                        setColor(PUSHED_BUTTON_COLOR);
                }

				event.consume();
			}
		};

        Button(Board board, Socket row, Socket column, int rowPos, int columnPos, String label) {
            super(board);
            this.row = row;
			this.column = column;
            this.rowInputPin = new InputPin(this, "ROW PIN " + row.getUserData());
            this.rowPos = rowPos;
            this.columnPos = columnPos;

            Socket innerSocket = new Socket(row.getComponent());
            new Potential(innerSocket, this.row);
            innerSocket.connect(this.rowInputPin);

			GridSystem grid = getBoard().getGrid();

			width = grid.getSizeX()*3;
			height = grid.getSizeY()*3;

			//button core
			button = new Circle(grid.getSizeX(), Color.BLACK);
			button.setCenterX(width/2.0);
			button.setCenterY(height/2.0);

			//button base
			double shrinkX = width * 1/10;
			double shrinkY = height * 1/10;

			Rectangle baseBck = new Rectangle(width - 2*shrinkX, height - 2*shrinkY, Color.LIGHTGRAY);
			baseBck.setLayoutX(shrinkX);
			baseBck.setLayoutY(shrinkY);
			baseBck.setArcWidth(grid.getSizeX()/2.0);
			baseBck.setArcHeight(grid.getSizeY()/2.0);

			double boltRadius = grid.getSizeX() / 5.0;
			double boltOffset = grid.getSizeX() / 2.5;

			Circle topLeftBolt = new Circle(boltRadius, Color.BLACK);
			topLeftBolt.setCenterX(boltOffset);
			topLeftBolt.setCenterY(boltOffset);

			Circle topRightBolt = new Circle(boltRadius, Color.BLACK);
			topRightBolt.setCenterX(width - boltOffset);
			topRightBolt.setCenterY(boltOffset);

			Circle bottomLeftBolt = new Circle(boltRadius, Color.BLACK);
			bottomLeftBolt.setCenterX(boltOffset);
			bottomLeftBolt.setCenterY(height - boltOffset);

			Circle bottomRightBolt = new Circle(boltRadius, Color.BLACK);
			bottomRightBolt.setCenterX(width - boltOffset);
			bottomRightBolt.setCenterY(height - boltOffset);

            // button label
            Text labelText = new Text(label);
            labelText.setX(-labelText.getBoundsInParent().getWidth() / 2.0 + shrinkX + baseBck.getWidth() / 2.0);
            labelText.setY(labelText.getBoundsInParent().getHeight() / 2.0 + baseBck.getHeight() / 2.0);
            labelText.setFill(Color.valueOf("white"));
            labelText.setFont(Font.font(grid.getSizeY() * 1.4));
            labelText.setMouseTransparent(true);

			buttonBase = new Group(baseBck, topLeftBolt, topRightBolt, bottomLeftBolt, bottomRightBolt);

			button.addEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEnter);
            button.addEventFilter(MouseEvent.MOUSE_EXITED, onMouseExit);
            button.addEventFilter(MouseEvent.MOUSE_PRESSED, onMousePress);
			button.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseRelease);
			button.addEventFilter(MouseEvent.MOUSE_DRAGGED, Event::consume);

            this.getChildren().addAll(buttonBase, button, labelText);

            this.makeImmovable();
        }


        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        private void setColor(Color newColor) {
            if (Platform.isFxApplicationThread()) button.setFill(newColor);
            else Platform.runLater(() -> button.setFill(newColor));
        }

        /**
         * Je stlacene tlacidlo a riadok zapojeny?
         *
         * @return
         */
        private boolean isActive() {
            return this.pressed && this.isLow(this.rowInputPin);
        }

        /**
         * Je niekto v stlpci aktivny?
         *
         * @return
         */
        private boolean shouldBeColumnLow() {
            for (int y = 0; y < 4; y++) {
                Button btn = buttons[columnPos][y];
                if (btn != null && btn.isActive()) return true;
            }

            return false;
        }

        @Override
        public void simulate() {
            if (shouldBeColumnLow()) {
                getBoard().addEvent(new BoardChangeEvent(this.column, Potential.Value.LOW));
            } else {
                getBoard().addEvent(new BoardChangeEvent(this.column, Potential.Value.HIGH));
            }
        }

        @Override
        public void reset() {

        }

        @Override
        public List<Pin> getPins() {
            return null;
        }
    }
}