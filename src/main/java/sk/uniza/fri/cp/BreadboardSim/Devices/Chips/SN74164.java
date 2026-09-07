package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Obvod 74164
 * 8-bit serial shift register
 *
 * @author Tomáš Hianik
 * @created 6.5.2017.
 */
public class SN74164 extends Chip {

    private static final String NAME = "SN74164";
    private static final String SHORT_DESCRIPTION = "8-bit serial shift register";
    private static final int PINS_COUNT = 14;
    private static final int _A = 1;
    private static final int _B = 2;
    private static final int _QA = 3;
    private static final int _QB = 4;
    private static final int _QC = 5;
    private static final int _QD = 6;
    private static final int _GND = 7;
    private static final int _CLK = 8;
    private static final int _CLR_ = 9;
    private static final int _QE = 10;
    private static final int _QF = 11;
    private static final int _QG = 12;
    private static final int _QH = 13;
    private static final int _VCC = 14;

    private int[] outputs = {_QA, _QB, _QC, _QD, _QE, _QF, _QG, _QH};
    private boolean[] Q = new boolean[8];
    private boolean wasClockLow = true;

    //inspector, pristup iba z FXAppThread (okrem stage)
    private volatile Stage inspectionStage;
    private GraphTimeLine gtlA, gtlB, gtlClock, gtlClear_;
    private GraphTimeLine[] gtlQ;


    public SN74164() {
        super(PINS_COUNT);
    }

    public SN74164(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate() {
        boolean clrLow = this.isLow(_CLR_);

        if (clrLow) {
            for (int i = 0; i < Q.length; i++) {
                Q[i] = false;
            }
        }

        if (this.isLow(_CLK)) {
            //ak nastala zmena v CLK alebo nastalo vymazanie
            if (!wasClockLow || clrLow) this.tickNonWrite(!clrLow, this.isHigh(_A), this.isHigh(_B));

            wasClockLow = true;
        } else if (wasClockLow && this.isHigh(_CLK)) {
            //shift
            System.arraycopy(Q, 0, Q, 1, Q.length - 1);

            boolean aHigh = this.isHigh(_A);
            boolean bHigh = this.isHigh(_B);

            Q[0] = aHigh && bHigh;

            //ak nastala zmena v CLK alebo nastalo vymazanie
            if (wasClockLow || clrLow) this.tickWrite(!clrLow, aHigh, bHigh);

            wasClockLow = false;
        }


        for (int i = 0; i < outputs.length; i++) {
            this.setPin(outputs[i], Q[i] ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    /**
     * Tick CLK do LOW -> nenastáva posun ale môže nastať mazanie.
     *
     * @param clrHigh CLEAR je v high?
     * @param aHigh   A je v high?
     * @param bHigh   B je v high?
     */
    private void tickNonWrite(boolean clrHigh, boolean aHigh, boolean bHigh) {
        if (this.inspectionStage != null && this.inspectionStage.isShowing()) {
            Platform.runLater(() -> {
                this.gtlClear_.addValue(clrHigh);
                this.gtlA.addValue(aHigh);
                this.gtlB.addValue(bHigh);
                this.gtlClock.addValue(false);

                if (!clrHigh) {
                    //mazanie
                    for (int i = 0; i < 8; i++)
                        this.gtlQ[i].addValue(false);

                } else {
                    //hodnoty ostavaju
                    for (int i = 0; i < 8; i++)
                        this.gtlQ[i].addSameValue();
                }
            });
        }
    }

    /**
     * Tick CLK do HIGH -> nastáva posun a zápis do QA alebo mazanie.
     *
     * @param clrHigh CLEAR je v high?
     * @param aHigh   A je v high?
     * @param bHigh   B je v high?
     */
    private void tickWrite(boolean clrHigh, boolean aHigh, boolean bHigh) {
        if (this.inspectionStage != null && this.inspectionStage.isShowing()) {
            Platform.runLater(() -> {
                this.gtlClear_.addValue(clrHigh);
                this.gtlA.addValue(aHigh);
                this.gtlB.addValue(bHigh);
                this.gtlClock.addValue(true);

                if (!clrHigh) {
                    //mazanie
                    for (int i = 0; i < 8; i++)
                        this.gtlQ[i].addValue(false);

                } else {
                    //hodnoty sa posuvaju
                    for (int i = 7; i >= 1; i--)
                        this.gtlQ[i].addValue(this.gtlQ[i - 1].getCurrentState());

                    this.gtlQ[0].addValue(aHigh && bHigh);
                }
            });
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_A, new InputPin(this, "A"));
        registerPin(_B, new InputPin(this, "B"));
        registerPin(_QA, new OutputPin(this, "QA"));
        registerPin(_QB, new OutputPin(this, "QB"));
        registerPin(_QC, new OutputPin(this, "QC"));
        registerPin(_QD, new OutputPin(this, "QD"));
        registerPin(_GND, new OutputPin(this, "GND"));
        registerPin(_CLK, new InputPin(this, "CLK"));
        registerPin(_CLR_, new InputPin(this, "CLR_"));
        registerPin(_QE, new OutputPin(this, "QE"));
        registerPin(_QF, new OutputPin(this, "QF"));
        registerPin(_QG, new OutputPin(this, "QG"));
        registerPin(_QH, new OutputPin(this, "QH"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate();
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_QA, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QB, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QC, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QD, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QE, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QF, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QG, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QH, Pin.PinState.NOT_CONNECTED);
        wasClockLow = true;
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortStringDescription() {
        return SHORT_DESCRIPTION;
    }

    @Override
    public Pane getImage() {
        return generateItemImage(NAME, PINS_COUNT);
    }

    @Override
    protected Stage getInspectionWindow() {
        if (this.inspectionStage != null) return this.inspectionStage;

        this.inspectionStage = super.getInspectionWindow();

        Canvas canvas = new Canvas(400, 400);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);

        //vytvorenie timeLine-ov
        double timeLineWidth = 350;
        double timeLineHeight = 20;
        double timeLineNameWidth = 50;
        double margin = 10;

        this.gtlClear_ = new GraphTimeLine(gc, 0, margin, timeLineWidth, timeLineHeight, "CLEAR_", timeLineNameWidth);
        this.gtlA = new GraphTimeLine(gc, 0, 2 * margin + timeLineHeight, timeLineWidth, timeLineHeight, "A", timeLineNameWidth);
        this.gtlB = new GraphTimeLine(gc, 0, 2 * (margin + timeLineHeight) + margin, timeLineWidth, timeLineHeight, "B", timeLineNameWidth);
        this.gtlClock = new GraphTimeLine(gc, 0, 3 * (margin + timeLineHeight) + margin, timeLineWidth, timeLineHeight, "CLOCK", timeLineNameWidth);

        //vytvorenie timeLine pre vystupy
        this.gtlQ = new GraphTimeLine[8];
        for (int i = 0; i < 8; i++) {
            this.gtlQ[i] = new GraphTimeLine(gc, 0, (4 + i) * (margin + timeLineHeight) + margin, timeLineWidth, timeLineHeight, "Q" + (char) (65 + i), timeLineNameWidth);
        }

        ((VBox) this.inspectionStage.getScene().getRoot()).getChildren().add(canvas);

        this.inspectionStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            this.inspectionStage = null;
        });

        return this.inspectionStage;
    }


    /**
     * Časová os pre zobrazenie priebehu zmeny hodnôt potenciálov na vstupoch/výstupoch IC.
     */
    private static class GraphTimeLine {
        private static final int HISTORY_POINTS = 20;

        private final String name;
        private final GraphicsContext gc;
        private final double x, y, width, height, nameWidth;
        private final boolean[] pointsY;

        GraphTimeLine(GraphicsContext gc, double x, double y, double width, double height, String name, double nameWidth) {
            this.gc = gc;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.name = name;
            this.nameWidth = nameWidth;

            this.pointsY = new boolean[HISTORY_POINTS];

            redraw();
        }

        public boolean getCurrentState() {
            return this.pointsY[HISTORY_POINTS - 1];
        }

        public void addHigh() {
            addValue(true);
        }

        public void addLow() {
            addValue(false);
        }

        public void addSameValue() {
            this.addValue(this.pointsY[HISTORY_POINTS - 1]);
        }

        public void addValue(boolean isHigh) {
            //shift
            System.arraycopy(pointsY, 1, pointsY, 0, pointsY.length - 1);
            this.pointsY[HISTORY_POINTS - 1] = isHigh;
            redraw();
        }

        private void redraw() {
            double lineX, lineY;

            clearRect();

            //nazov grafu
            TextAlignment ta = this.gc.getTextAlign();
            this.gc.setTextAlign(TextAlignment.RIGHT);
            this.gc.fillText(this.name, this.x + this.nameWidth - 5, this.y + height - 5);
            this.gc.setTextAlign(ta);

            this.gc.beginPath();

            //prvy bod
            lineX = this.x + this.nameWidth;
            lineY = pointsY[0] ? this.y : this.y + height - 1;
            gc.moveTo(lineX, lineY);

            for (int i = 1; i < HISTORY_POINTS; i++) {
                lineX = this.x + this.nameWidth + i * (width / HISTORY_POINTS);
                lineY = pointsY[i] ? this.y : this.y + height - 1;

                if (pointsY[i - 1] != pointsY[i]) {
                    //ak doslo k zmene hodnoty, najprv prejdi na novu hodnotu
                    gc.lineTo(lineX - width / HISTORY_POINTS, lineY);
                }

                gc.lineTo(lineX, lineY);
            }

            //this.gc.closePath();
            this.gc.stroke();
        }

        private void clearRect() {
            this.gc.clearRect(this.x, this.y, this.width + this.nameWidth, this.height);
        }
    }
}
