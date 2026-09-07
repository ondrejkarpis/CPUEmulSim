package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Components.BusInterface;
import sk.uniza.fri.cp.BreadboardSim.HighlightGroup;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Káblik spájajúci sokety. Vytvára medzi nimi potenciál.
 * Umožňuje meniť farbu alebo vytvárať zlomy.
 * Nové kábliky majú po vytvorení farbu, ktorá je nastavená ako defaultná (statická prem.).
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class Wire extends HighlightGroup {

	private static Color defaultColor = Color.BLACK;

    private Color color;
    private Potential potential;

	private WireEnd[] ends;	//konce kablika
	private List<Joint> joints;
	private List<WireSegment> segments;

	//grupy pre vrstvovanie -> segmenty dole, jointy hore
	private Group jointsGroup;
	private Group segmentsGroup;

	private Joint createdJoint;

    //EVENTY
    //pri zacati tahania a stlacenom CTRL vytvori novy zlom
    private EventHandler<MouseEvent> onMouseDragDetected = event -> {
        if (event.isPrimaryButtonDown() && (event.isControlDown() || event.getClickCount() == 2) && event.getTarget() instanceof WireSegment) {
            WireSegment segmentToSplit = ((WireSegment) event.getTarget());
            createdJoint = splitSegment(segmentToSplit);
            event.consume();
        }
    };
    //pohyb s novovytvorenym zlomom
    private EventHandler<MouseEvent> onMouseDragged = event -> {
        if (event.isPrimaryButtonDown()) {
            if (createdJoint != null) {
                Event.fireEvent(createdJoint, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                        true, true, true, true, true,
                        true, true, true, true, null));
            }
            event.consume();
        }
    };
    //ukoncenie vytvarania zlomu
    private EventHandler<MouseEvent> onMouseReleased = event -> createdJoint = null;
    //zvyraznenie po prechode nad kablikom
    private EventHandler<MouseEvent> onMouseEntered = event -> {
        if (!this.isSelected()) {
            this.highlightSegments(0.7);
        }
        Color brighter = color.brighter();
        this.setStyle("-fx-effect: dropshadow(gaussian, rgb("
                + brighter.getRed() * 255 + ","
                + brighter.getGreen() * 255 + ","
                + brighter.getBlue() * 255 + "), 1, 1.0, 0, 0)");
    };
    //zrusenie zvyraznenie po opusteni priestoru kablika
    private EventHandler<MouseEvent> onMouseExited = event -> {
        if (!this.isSelected()) {
            this.unhighlighSegments();
        }
        this.setStyle("-fx-effect: none");
    };

	/**
     * Vytvorenie nového káblika.
     * Po vytvorení je voľný koniec prístupný cez metódu catchFreeEnd().
     *
     * @param startSocket Soket, na ktorom sa začal káblik vytvárať. Hneď sa k nemu pripojí.
     */
	public Wire(Socket startSocket){
		this.joints = new LinkedList<>();
		this.segments = new LinkedList<>();
		this.segmentsGroup = new Group();
		this.jointsGroup = new Group();
        this.color = defaultColor;

		//konce kablika
		this.ends = new WireEnd[2];

		//zaciatok kablika, pripojeny k soketu
		this.ends[0] = new WireEnd(startSocket.getComponent().getBoard(), this);
		this.ends[0].connectSocket(startSocket);

		//koniec kablika vo vzduchu
		this.ends[1] = new WireEnd(startSocket.getComponent().getBoard(), this);
		this.ends[1].moveTo(this.ends[0].getLayoutX(), this.ends[0].getLayoutY());

		//prvotny segment
		WireSegment segment = new WireSegment(this, this.ends[0], this.ends[1]);

		this.segments.add(segment);

		this.segmentsGroup.getChildren().addAll(segment);
		this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);
		this.getChildren().addAll(segmentsGroup, jointsGroup);
        this.setId("wire");
        this.registerEvents();
    }

    /**
     * Vytvorenie nového káblika na poche simulátora.
     * Konce sú umiestnené na súradniciach [1,1] a [2,2] na mriežke.
     *
     * @param board Plocha simulátora.
     */
    public Wire(Board board) {
        this.joints = new LinkedList<>();
        this.segments = new LinkedList<>();
        this.segmentsGroup = new Group();
        this.jointsGroup = new Group();
        this.color = defaultColor;

        //konce kablika
        this.ends = new WireEnd[2];

        //zaciatok kablika vo vzduchu
        this.ends[0] = new WireEnd(board, this);
        this.ends[0].moveTo(1, 1);

        //koniec kablika vo vzduchu
        this.ends[1] = new WireEnd(board, this);
        this.ends[1].moveTo(2, 2);

        //prvotny segment
        WireSegment segment = new WireSegment(this, this.ends[0], this.ends[1]);

        this.segments.add(segment);

        this.segmentsGroup.getChildren().addAll(segment);
        this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);
        this.getChildren().addAll(segmentsGroup, jointsGroup);

        this.registerEvents();
    }

    /**
     * Vráti plochu simulátora, na ktorejje káblik umiestnený.
     *
     * @return Plocha simulátora.
     */
    public Board getBoard() {
        return this.ends[0].getBoard();
    }

    /**
     * Defaultná farba novovytvorených káblikov.
     *
     * @param defColor defaultná farba.
     */
	public static void setDefaultColor(Color defColor){
		defaultColor = defColor;
	}

    /**
     * Zísaknie defaultnej farby.
     *
     * @return Defaultná farba.
     */
    public static Color getDefaultColor(){
        return defaultColor;
    }

    /**
     * Zmena farby káblika a jeho segmentov.
     *
     * @param newColor Nová farba.
     */
    public void changeColor(Color newColor) {
        this.color = newColor;
        this.segments.forEach(wireSegment -> wireSegment.setColor(this.color));
        for (WireEnd end : this.ends) {
            end.setDefaultColor();
        }
        if (this.isSelected()) this.highlightSegments(1);
    }

    /**
     * Vráti farbu káblika.
     *
     * @return Farba káblika.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Vráti konce káblika.
     *
     * @return Pole s koncami káblika.
     */
    public WireEnd[] getEnds() {
        return ends;
    }

    /**
     * Vráti zlomy na kábliku.
     *
     * @return List so zlomami.
     */
    public List<Joint> getJoints() {
        return joints;
    }

	/**
	 * Vráti voľný koniec káblika pri jeho prvotnom vytváraní.
	 * Pri opätovnom volaní vráti ten koniec káblika, ktorý bol vytvorený ako druhý.
	 *
	 * @return Voľný koniec káblika pri vytváraní.
	 */
	public WireEnd catchFreeEnd(){
		return this.ends[1];
	}

	/**
	 * Odstránenie spojovača na kábliku. Je možné odstraňovať iba vnútorné jointy, nie konce.
	 * @param joint
	 */
	public void removeJoint(Joint joint){
		if(joint instanceof WireEnd) return;

		//segmenty spojene jointom
		WireSegment firstSegment = joint.getPrimaryWireSegment();
		WireSegment secondSegment = joint.getSecondaryWireSegment();

		//jointy z odlahlych koncov spojenych segmentov
		Joint firstJoint = firstSegment.getOtherJoint(joint);
		Joint secondJoint = secondSegment.getOtherJoint(joint);

		//odstranenie segmentov z joinov
		firstJoint.removeWireSegment(firstSegment);
		secondJoint.removeWireSegment(secondSegment);

		WireSegment newWireSegment = new WireSegment(this, firstJoint, secondJoint);
		this.segments.add(newWireSegment);
		this.segmentsGroup.getChildren().add(newWireSegment);

		this.segments.remove(firstSegment);
		this.segments.remove(secondSegment);
		this.joints.remove(joint);

		this.segmentsGroup.getChildren().removeAll(firstSegment, secondSegment);
		this.jointsGroup.getChildren().remove(joint);
	}

    /**
     * Kontrola, či sú oba konce káblika pripojené.
     *
     * @return True ak sú oba konce káblika pripojené k soketom, false inak.
     */
    public boolean areBothEndsConnected() {
        return this.ends[0].isConnected() && this.ends[1].isConnected();
    }

    /**
     * Rozpolenie posledného segmentu káblika. Vráti novovytvorený zlom.
     *
     * @return Nový zlom na kábliku.
     */
    public Joint splitLastSegment() {
        return this.splitSegment(((LinkedList<WireSegment>) this.segments).getLast());
    }

    private Joint splitSegment(WireSegment segment) {
        Joint newJoint = new Joint(getBoard(), this);

		//konce rozdelujuceho segmentu
		Joint firstJoint = segment.getStartJoint();
		Joint secondJoint = segment.getEndJoint();

		//presun noveho jointu na miesto prveho aby neskakal mimo plochu
		newJoint.moveTo(firstJoint.getLayoutX(), firstJoint.getLayoutY());

		//odpojenie segmentu od koncov
		firstJoint.removeWireSegment(segment);
		secondJoint.removeWireSegment(segment);

		//vytvorenie novych dvoch segmentov
		WireSegment firstSegment = new WireSegment(this, firstJoint, newJoint);
		WireSegment secondSegment = new WireSegment(this, newJoint, secondJoint);

		this.segments.add(firstSegment);
		this.segments.add(secondSegment);

        //zaradenie noveho jointu do zoznamu na poziciu medzi povodne dva
        int fjIndex = this.joints.indexOf(firstJoint);
        this.joints.add(fjIndex + 1, newJoint);

		this.segmentsGroup.getChildren().addAll(firstSegment, secondSegment);
		this.jointsGroup.getChildren().add(newJoint);

		this.segments.remove(segment);
		this.segmentsGroup.getChildren().remove(segment);

		return newJoint;
	}

    private void registerEvents() {
        this.addEventHandler(MouseEvent.DRAG_DETECTED, onMouseDragDetected);
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleased);
        this.addEventHandler(MouseEvent.MOUSE_ENTERED, onMouseEntered);
        this.addEventHandler(MouseEvent.MOUSE_EXITED, onMouseExited);
    }

    //posuvanie jointov spojenia
    private double firstDeltaX, firstDeltaY;
    private WireEnd lastMovingEnd;

    /**
     * Posunitie jointov o deltu podľa zmeny polohy jedného z konca spojenia. Ak sa pohnú oba konce spojenia
     * o rovnakú vzdialenosť, posunú sa aj jointy.
     * To umožňuje posúvanie celých spojení aj pri prepojení dvoch odlišných komponentov, ak sa pohybujú spolu.
     *
     * @param wireEnd Koniec káblika, ktorý vyvolal zmenu.
     * @param deltaX  Zmena pozície X
     * @param deltaY  Zmena pozície Y
     */
    void moveJointsWithEnd(WireEnd wireEnd, double deltaX, double deltaY) {
        //ak je posunutie vyvolane druhym koncom, ako tym co sa posuval naposledy
        if (this.lastMovingEnd != wireEnd) {
            //ak sa oba konce posunuli o rovnaky deltu
            if (Math.abs(firstDeltaX - deltaX) < 0.5 && Math.abs(firstDeltaY - deltaY) < 0.5) {
                //posun aj vsetky jointy a vynuluj posun
                this.joints.forEach(joint -> joint.moveBy(deltaX, deltaY));
                this.firstDeltaX = 0;
                this.firstDeltaY = 0;
                return;
            }
        }
        //nastav posledny posun
        this.firstDeltaX = deltaX;
        this.firstDeltaY = deltaY;
        this.lastMovingEnd = wireEnd;
    }

    void updatePotential() {
        if(this.potential != null){
			this.potential.delete();
			this.potential = null;
		}

		Socket toUpdate;
		if(this.ends[0] != null && this.ends[1] != null){
			Socket start = this.ends[0].getSocket();
			Socket end = this.ends[1].getSocket();
			if(start != null && end != null) {
				//ak su oba konce pripojene, updateujeme jednu vetvu potencialov

                //hmm.. ak sa pripajame na datovu zbernicu, musisa akrualizovat ako prva aby nedoslo ku skratu
                if (end.getComponent() instanceof BusInterface)
                    this.potential = new Potential(end, start);
                else
                    this.potential = new Potential(start, end);

				toUpdate = start;
			} else {
				//ak je iba jeden koniec pripojeny, updateujeme jeho vetvu potencialov (druha sa updatuje pri odpojeni WireEnd)
				toUpdate = start!=null?start:end;
			}

			if(getBoard().simRunningProperty().getValue())
				getBoard().addEvent(new BoardEvent(toUpdate));
		}
	}

	@Override
	public void delete() {
		super.delete();

        this.ends[0].disconnectSocket();
        this.ends[1].disconnectSocket();
		this.ends[0].getBoard().removeItem(this);
	}

    @Override
    public void select() {
        super.select();

        if (!this.isSelectable()) return;

        this.highlightSegments(1);
    }

    @Override
    public void deselect() {
        super.deselect();

        this.unhighlighSegments();
    }

    private ArrayList<Shape> selectionShapes = new ArrayList<>();

    private void highlightSegments(double opacity) {
        this.unhighlighSegments();

        this.segments.forEach(segment -> {

            Line newLine = new Line(segment.getStartX(), segment.getStartY(), segment.getEndX(), segment.getEndY());
            for (double value : STROKE_DASH_ARRAY)
                newLine.getStrokeDashArray().add(value);

            //this.selectionShape.getStrokeDashArray().add(Collections.(STOKE_DASH_ARRAY));
            newLine.setStrokeWidth(1.5);
            newLine.setStroke(this.getColor().invert());
            newLine.setStrokeLineCap(StrokeLineCap.ROUND);
            newLine.setOpacity(opacity);
            newLine.setMouseTransparent(true);

            this.selectionShapes.add(newLine);
            this.getChildren().add(newLine);
        });

    }

    private void unhighlighSegments() {
        this.getChildren().removeAll(this.selectionShapes);
        this.selectionShapes.clear();
    }

    @Override
    public Pane getDescription() {
        Pane cached = super.getDescription();
        if (cached == null) {
            VBox wrapper = new VBox();
            wrapper.setAlignment(Pos.CENTER);

            ColorPicker colorPicker = new ColorPicker(getColor());
            colorPicker.setOnAction(event -> this.changeColor(colorPicker.getValue()));

            wrapper.getChildren().add(colorPicker);

            this.cacheDescription(wrapper);
            return wrapper;
        } else return cached;
    }
}