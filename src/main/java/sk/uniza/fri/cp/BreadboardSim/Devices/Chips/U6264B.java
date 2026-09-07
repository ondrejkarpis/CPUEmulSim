package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Obvod U6264B
 * 8kB pamäť
 *
 * @author Tomáš Hianik
 * @created 14.4.2017.
 */
public class U6264B extends Chip {

    private static final String NAME = "U6264B";
    private static final String SHORT_DESCRIPTION = "8kB pamäť";
    private static final int PINS_COUNT = 28;
    private static final int _NC = 1;
    private static final int _A12 = 2;
    private static final int _A7 = 3;
    private static final int _A6 = 4;
    private static final int _A5 = 5;
    private static final int _A4 = 6;
    private static final int _A3 = 7;
    private static final int _A2 = 8;
    private static final int _A1 = 9;
    private static final int _A0 = 10;
    private static final int _DQ0 = 11;
    private static final int _DQ1 = 12;
    private static final int _DQ2 = 13;
    private static final int _GND = 14;
    private static final int _DQ3 = 15;
    private static final int _DQ4 = 16;
    private static final int _DQ5 = 17;
    private static final int _DQ6 = 18;
    private static final int _DQ7 = 19;
    private static final int _E1_ = 20;
    private static final int _A10 = 21;
    private static final int _G_ = 22;
    private static final int _A11 = 23;
    private static final int _A9 = 24;
    private static final int _A8 = 25;
    private static final int _E2 = 26;
    private static final int _W_ = 27;
    private static final int _VCC = 28;

    private static final int[] addressPins = {_A0, _A1, _A2, _A3, _A4, _A5, _A6, _A7, _A8, _A9, _A10, _A11, _A12};
    private static final int[] dataPins = {_DQ0, _DQ1, _DQ2, _DQ3, _DQ4, _DQ5, _DQ6, _DQ7};

    private final byte[] savedData = new byte[8192];

    //inspector
    private volatile Stage inspectionStage;
    private TableView<String[]> tableView;
    private TableColumn tcAddress;
    private TableColumn[] tcDataHex, tcDataAscii;
    private ObservableList<String[]> observableDataRows;

    public U6264B() {
        super(PINS_COUNT);
    }

    public U6264B(Board board) {
        super(board, PINS_COUNT, 6);
    }

    /**
     * Konstruktor pri kotrom sa da nastavit sirka IC.
     * Z dovodu kompatibility so startym simulatorom.
     *
     * @param board
     */
    public U6264B(Board board, int chipGridHeight) {
        super(board, PINS_COUNT, chipGridHeight);
    }

    private void updateGate() {

        if (this.isLow(_E2) || this.isHigh(_E1_)) {
            Arrays.stream(dataPins).forEach(dataPin -> this.setPin(dataPin, Pin.PinState.HIGH_IMPEDANCE));
            return;
        }

        if (this.isHigh(_W_) && this.isHigh(_G_)) {
            Arrays.stream(dataPins).forEach(dataPin -> this.setPin(dataPin, Pin.PinState.HIGH_IMPEDANCE));
            return;
        }

        if (this.isHigh(_W_) && this.isLow(_G_)) {
            //citanie z pamate
            int data = savedData[decodeAddress()];
            for (int i = 0; i < dataPins.length; i++) {
                if ((data & 1 << i) != 0) { //je tam jednotka
                    this.setPin(dataPins[i], Pin.PinState.HIGH);
                } else { //je tam nula
                    this.setPin(dataPins[i], Pin.PinState.LOW);
                }
            }
            return;
        }

        if (this.isLow(_W_)) {
            //zapis do pamate
            byte data = decodeData();
            int address = decodeAddress();
            this.savedData[address] = data;
            updateInspectorData(address);

            return;
        }

    }

    private int decodeAddress() {
        int address = 0;

        for (int i = 0; i < addressPins.length; i++) {
            if (this.isHigh(addressPins[i]))
                address |= 1 << i;
        }

        return address;
    }

    private byte decodeData() {
        int data = 0;

        for (int i = 0; i < dataPins.length; i++) {
            if (this.isHigh(dataPins[i]))
                data |= 1 << i;
        }

        return (byte) data;
    }

    private void updateInspectorData(int address) {
        if (inspectionStage != null && inspectionStage.isShowing()) {
            int row = address / 16;
            int indexHex = 1 + address % 16;
            int indexAscii = 17 + address % 16;
            byte data = savedData[address];
            String[] rowData = observableDataRows.get(row);
            rowData[indexHex] = String.format("%02X", data);
            rowData[indexAscii] = String.format("%c", data == 0 ? '.' : (char) data);
            observableDataRows.set(row, rowData);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_NC, new NotConnectedPin(this));
        registerPin(_A12, new InputPin(this, "A12"));
        registerPin(_A7, new InputPin(this, "A7"));
        registerPin(_A6, new InputPin(this, "A6"));
        registerPin(_A5, new InputPin(this, "A5"));
        registerPin(_A4, new InputPin(this, "A4"));
        registerPin(_A3, new InputPin(this, "A3"));
        registerPin(_A2, new InputPin(this, "A2"));
        registerPin(_A1, new InputPin(this, "A1"));
        registerPin(_A0, new InputPin(this, "A0"));
        registerPin(_DQ0, new InputOutputPin(this, "DQ0"));
        registerPin(_DQ1, new InputOutputPin(this, "DQ1"));
        registerPin(_DQ2, new InputOutputPin(this, "DQ2"));
        registerPin(_GND, new OutputPin(this, "VSS"));
        registerPin(_DQ3, new InputOutputPin(this, "DQ3"));
        registerPin(_DQ4, new InputOutputPin(this, "DQ4"));
        registerPin(_DQ5, new InputOutputPin(this, "DQ5"));
        registerPin(_DQ6, new InputOutputPin(this, "DQ6"));
        registerPin(_DQ7, new InputOutputPin(this, "DQ7"));
        registerPin(_E1_, new InputPin(this, "E1_")); // (CE1_)
        registerPin(_A10, new InputPin(this, "A10"));
        registerPin(_G_, new InputPin(this, "G_")); //(OE_)
        registerPin(_A11, new InputPin(this, "A11"));
        registerPin(_A9, new InputPin(this, "A9"));
        registerPin(_A8, new InputPin(this, "A8"));
        registerPin(_E2, new InputPin(this, "E2")); // (CE2)
        registerPin(_W_, new InputPin(this, "W_")); // (WE_)
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
        this.setPin(_DQ0, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ1, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ2, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ3, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ4, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ5, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ6, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ7, Pin.PinState.NOT_CONNECTED);

        Arrays.fill(this.savedData, (byte) 0);

        if (inspectionStage != null && inspectionStage.isShowing()) {
            for (int i = 0; i < observableDataRows.size(); i++) {
                String[] rowData = observableDataRows.get(i);
                for (int j = 0; j < 16; j++) {
                    rowData[j + 1] = "00";
                    rowData[j + 17] = ".";
                }

                observableDataRows.set(i, rowData);
            }
        }
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortStringDescription() {
        return SHORT_DESCRIPTION;
    }

    public Pane getImage() {
        return generateItemImage(NAME, PINS_COUNT, 5);
    }

    @Override
    protected Stage getInspectionWindow() {
        if (inspectionStage != null) return inspectionStage;

        inspectionStage = super.getInspectionWindow();

        tableView = new TableView<>();
        tcDataHex = new TableColumn[16];
        tcDataAscii = new TableColumn[16];

        observableDataRows = FXCollections.observableArrayList();

        //vytvorenie pola s informaciami pre kazdy riadok
        byte d;
        for (int i = 0; i < 512; i++) {
            //adresa + 16 hex + 16 ascii
            String[] data = new String[33];

            //adresa
            data[0] = String.format("0x%04X", i * 16);

            //hex hodnoty
            for (int j = 0; j < 16; j++) {
                data[j + 1] = String.format("%02X", savedData[i * 16 + j]);
            }

            //ascii hodnoty
            for (int j = 0; j < 16; j++) {
                d = savedData[i * 16 + j];
                data[j + 17] = String.format("%c", d == 0 ? '.' : (char) d);
            }

            observableDataRows.add(data);
        }


        //faktorka pre zvyraznenie zmenenych chlievikov
        Callback<TableColumn<String[], String>, TableCell<String[], String>> cellFactory =
                new Callback<TableColumn<String[], String>, TableCell<String[], String>>() {
                    @Override
                    public TableCell<String[], String> call(TableColumn<String[], String> column) {
                        //buffer pre predoslu hodnotu chlievika
                        final StringBuilder sb = new StringBuilder();
                        //buffer pre posledny index chlievika, tableView ich prehadzuje pri mene velkosti okna zmene obsahu
                        SimpleIntegerProperty lastIndex = new SimpleIntegerProperty(-1);

                        TableCell<String[], String> cell = new TableCell<String[], String>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);

                                //ak je chlievik prazdny, nic sa nedeje, hlavne ziadne NullPointerEx
                                if (empty) {
                                    setText("");
                                    return;
                                }

                                //ak sa hodnota v chlieviku zmenila, spusti animaciu zvyraznenia
                                if (sb.length() > 0 && !sb.toString().equals(item) && lastIndex.intValue() == getIndex()) {
                                    final Animation anim = new Transition() {
                                        {
                                            setCycleDuration(Duration.seconds(2));
                                            setInterpolator(Interpolator.EASE_OUT);
                                        }

                                        @Override
                                        protected void interpolate(double frac) {
                                            Color color = new Color(1, 0.2, 0.2, 1 - frac);
                                            setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
                                        }
                                    };

                                    anim.playFromStart();
                                }

                                //aktualizuj obsah chlievika
                                sb.replace(0, 2, item);
                                setText(sb.toString());

                                lastIndex.set(getIndex());
                            }
                        };

                        return cell;
                    }
                };

        //vytvorenie stlpcov
        //adresy
        tcAddress = new TableColumn("Adresa");
        tcAddress.setStyle("-fx-alignment: CENTER;");
        tcAddress.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue>() {
            @Override
            public ObservableValue call(TableColumn.CellDataFeatures<String[], String> param) {
                return new SimpleStringProperty(param.getValue()[0]);
            }
        });
        tableView.getColumns().add(tcAddress);

        //separator
        tableView.getColumns().add(new TableColumn<>());

        //hex data
        for (int i = 0; i < 16; i++) {
            tcDataHex[i] = new TableColumn(Integer.toString(i));
            tcDataHex[i].setCellFactory(cellFactory);

            final int colNo = i;
            tcDataHex[i].setCellValueFactory(new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue>() {
                @Override
                public ObservableValue call(TableColumn.CellDataFeatures<String[], String> param) {
                    return new SimpleStringProperty(param.getValue()[colNo + 1]);
                }
            });

            tcDataHex[i].setPrefWidth(23);
            tcDataHex[i].setStyle("-fx-alignment: CENTER;");
            tableView.getColumns().add(tcDataHex[i]);
        }

        //separator
        tableView.getColumns().add(new TableColumn<>());

        //ascii data
        for (int i = 0; i < 16; i++) {
            tcDataAscii[i] = new TableColumn(Integer.toString(i));
            tcDataAscii[i].setCellFactory(cellFactory);

            final int colNo = i;
            tcDataAscii[i].setCellValueFactory(new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue>() {
                @Override
                public ObservableValue call(TableColumn.CellDataFeatures<String[], String> param) {
                    return new SimpleStringProperty(param.getValue()[colNo + 17]);
                }
            });

            tcDataAscii[i].setPrefWidth(19);
            tcDataAscii[i].setStyle("-fx-alignment: CENTER;");
            tableView.getColumns().add(tcDataAscii[i]);
        }

        tableView.setItems(observableDataRows);
        tableView.setStyle("-fx-font-size: 12px;");
        tableView.setEditable(false);

        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox root = ((VBox) inspectionStage.getScene().getRoot());
        root.getChildren().add(tableView);
        root.setPrefWidth(800);
        root.setPrefHeight(600);

        inspectionStage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            tableView = null;
            tcAddress = null;
            tcDataHex = null;
            tcDataAscii = null;
            observableDataRows = null;
            inspectionStage = null;
        });

        return inspectionStage;
    }


}