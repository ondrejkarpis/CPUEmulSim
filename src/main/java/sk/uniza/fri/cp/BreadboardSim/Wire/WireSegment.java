package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * Segment káblika.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class WireSegment extends Line {

	private Wire wire;
	private Joint startJoint;
	private Joint endJoint;

    WireSegment(Wire wire, Joint start, Joint end) {
        this.wire = wire;
		this.startJoint = start;
		this.endJoint = end;

		this.startJoint.connectWireSegment(this);
		this.endJoint.connectWireSegment(this);

		//nabindovanie k stredu jointov na koncoch kablika
        this.startXProperty().bind(this.startJoint.layoutXProperty());
        this.startYProperty().bind(this.startJoint.layoutYProperty());
        this.endXProperty().bind(this.endJoint.layoutXProperty());
        this.endYProperty().bind(this.endJoint.layoutYProperty());

        this.setStrokeWidth(start.getBoard().getGrid().getSizeMin() * 2.0 / 6.0);
        this.setFill(wire.getColor());
        this.setStroke(wire.getColor());
        this.setOpacity(1);
    }

    /**
     * Vráti káblik, ku ktorému patrí.
     *
     * @return Káblik ku ktorému patrí.
     */
    public Wire getWire(){return this.wire;}

    /**
     * Nastavenie farby segmentu.
     *
     * @param color Nová farba segmentu.
     */
    public void setColor(Paint color) {
        this.setFill(color);
        this.setStroke(color);
    }

    Joint getEndJoint() {
        return endJoint;
	}

    Joint getOtherJoint(Joint joint) {
        return startJoint == joint ? endJoint : startJoint;
	}

    void setEndJoint(Joint newVal) {
        endJoint = newVal;
	}

    Joint getStartJoint() {
        return startJoint;
	}

    void setStartJoint(Joint newVal) {
        startJoint = newVal;
	}
}