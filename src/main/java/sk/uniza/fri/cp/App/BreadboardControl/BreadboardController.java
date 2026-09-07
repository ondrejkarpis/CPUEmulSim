package sk.uniza.fri.cp.App.BreadboardControl;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import sk.uniza.fri.cp.App.AboutDialog;
import sk.uniza.fri.cp.App.CPUControl.CPUController;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Components.Breadboard;
import sk.uniza.fri.cp.BreadboardSim.Components.HexSegment;
import sk.uniza.fri.cp.BreadboardSim.Components.NumKeys;
import sk.uniza.fri.cp.BreadboardSim.Components.Probe;
import sk.uniza.fri.cp.BreadboardSim.DescriptionPane;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.*;
import sk.uniza.fri.cp.BreadboardSim.ItemPicker;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;
import sk.uniza.fri.cp.Bus.Bus;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Kontrolér okna simulátora.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 4.3.2017
 */

public class BreadboardController implements Initializable {

    private File currentFile; //otvorený súbor
    private Board board;
    private CPUController cpuController;

    @FXML private VBox root;
    @FXML private AnchorPane boardPane;

    //panel s nastrojmi
    @FXML private ToggleSwitch tsPower;

    // ovladanie CPU
    @FXML private Button btnF5spusti;
    @FXML private Button btnF7krok;
    @FXML private Button btnF9pauza;
    @FXML private Button btnF10stop;
    @FXML private Button btnF12reset;

    //pravy panel
    @FXML private VBox toolsBox;
    @FXML private ColorPicker wireColorPicker;
    @FXML private SplitPane toolsSplitPane;

    //stavovy riadok
    @FXML private Label lbCoordinates;
    @FXML private Label lbZoom;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //tool box -> napravo - colorPicker, itemPicker, description
        //vyber farby
        this.wireColorPicker.setValue(Wire.getDefaultColor());
        this.wireColorPicker.setOnAction(event -> Wire.setDefaultColor(this.wireColorPicker.getValue()));

        //panel s popisom
        DescriptionPane descriptionPane = new DescriptionPane();

        //vyber objektov
        ItemPicker newItemPicker = new ItemPicker();
        newItemPicker.setPanelForDescription(descriptionPane);
        registerItems(newItemPicker);

        this.toolsSplitPane.getItems().addAll(newItemPicker, descriptionPane);

        //PLOCHA SIMULATORA
        this.board = new Board(2000, 2000, 10);
        this.board.setDescriptionPane(descriptionPane);
        this.boardPane.getChildren().add(board);

        AnchorPane.setTopAnchor(this.board, 0.0);
        AnchorPane.setRightAnchor(this.board, 0.0);
        AnchorPane.setBottomAnchor(this.board, 0.0);
        AnchorPane.setLeftAnchor(this.board, 0.0);
        this.board.setHvalue(0.5);
        this.board.setVvalue(0.5);

        //PANEL S NASTOJMI
        //ak sa zmeni stav simulacie, zmen aj tlacitko spustania simulacie
        this.board.simRunningProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != this.tsPower.isSelected())
                this.tsPower.setSelected(newValue);
        });

        this.tsPower.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                this.board.powerOn();
            else
                this.board.powerOff();
        });

        //STAVOVY RIADOK
        //pozicia kurzora
        this.board.setOnMouseMoved(event -> {
            Point2D gridPoint = this.board.getMousePositionOnGrid(event);
            lbCoordinates.setText(((int) gridPoint.getX()) + "x" + ((int) gridPoint.getY()));
        });

        //aktualne priblizenie
        this.board.zoomScaleProperty().addListener((observable, oldValue, newValue) ->
                this.lbZoom.setText(((int) (newValue.doubleValue() * 100)) + "%"));

        // tlacidla na ovladanie CPU
        setButtons(0);
    }

    /**
     * Volanie zmazania vybraných objektov na ploche simulátora.
     */
    public void callDelete(){
        this.board.deleteSelect();
    }

    /**
     * Spustenie simulácie.
     *
     * @return Predchádzajúci stav simulácie. True - bežala, false - nebola spustená.
     */
    public boolean powerOn() {
        if (this.board.isSimulationRunning()) return true;
        this.board.powerOn();
        return false;
    }

    /**
     * Zastavenie simulácie.
     *
     * @return Predchádzajúci stav simulácie. True - bežala, false - nebola spustená.
     */
    public boolean powerOff() {
        boolean wasRunning = this.board.isSimulationRunning();
        this.board.powerOff();
        return wasRunning;
    }

    /**
     * Registrovanie dostupných objektov, ktoré je možné pridať na plochu.
     *
     * @param newItemPicker ItemPicker v ktorom sa objekty zobrazia.
     */
    private void registerItems(ItemPicker newItemPicker) {
        //zariadenia
        newItemPicker.registerItem(new Gen7400());
        newItemPicker.registerItem(new Gen7402());
        newItemPicker.registerItem(new Gen7404());
        newItemPicker.registerItem(new Gen7408());
        newItemPicker.registerItem(new Gen7410());
        newItemPicker.registerItem(new Gen7430());
        newItemPicker.registerItem(new Gen7432());
        newItemPicker.registerItem(new Gen7486());
        newItemPicker.registerItem(new SN74125());
        newItemPicker.registerItem(new SN74138());
        newItemPicker.registerItem(new SN74148());
        newItemPicker.registerItem(new SN74151());
        newItemPicker.registerItem(new SN74153());
        newItemPicker.registerItem(new SN74164());
        newItemPicker.registerItem(new SN74573());
        newItemPicker.registerItem(new U6264B());

        //komponenty
        newItemPicker.registerItem(new SchoolBreadboard());
        newItemPicker.registerItem(new Breadboard());
        newItemPicker.registerItem(new HexSegment());
        newItemPicker.registerItem(new NumKeys());
        newItemPicker.registerItem(new Probe());
    }

    //TLAČIDLÁ V NÁSTOJOVEJ LIŠTE

    /**
     * Reakcia na sltačenie tlačidla pre uloženie zapojenia do súboru.
     */
    @FXML
    private void handleSaveAction() {
        saveCircuit(false);
    }

    /**
     * Reakcia na sltačenie tlačidla pre uloženie zapojenia do iného súboru.
     */
    @FXML
    private void handleSaveAsAction() {
        saveCircuit(true);
    }

    /**
     * Reakcia na sltačenie tlačidla pre načítanie zapojenia zo súboru
     */
    @FXML
    private void handleLoadAction() {
        if (!continueIfUnsavedFile()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Načítať obvod...");
        chooser.setInitialDirectory(
                currentFile != null
                        ? currentFile.getParentFile()
                        : new File(Paths.get("").toAbsolutePath().toString()));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SCHX", "*.schx"),
                new FileChooser.ExtensionFilter("SCH", "*.sch"));

        File file = chooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) {
            if (board.isSimulationRunning()) {
                powerOff();
                while (true) {
                    if (!(board.isSimulationRunning())) break;
                }
            }

            if (board.load(file)) {
                ((Stage) root.getScene().getWindow()).setTitle("Simulátor - " + file.getName());
                currentFile = file;
                board.clearChange();
            }
        }
    }

    /**
     * Vyčistenie plochy.
     */
    @FXML
    private void handleClearBoardAction() {
        if (!continueIfUnsavedFile()) return;

        if (board.isSimulationRunning()) {
            powerOff();
            while (true) {
                if (!(board.isSimulationRunning())) break;
            }
        }

        board.clearBoard();
        ((Stage) root.getScene().getWindow()).setTitle("Simulátor - Nový obvod");
        currentFile = null;
        board.clearChange();
    }

    /**
     * Spustenie programu.
     */
    @FXML
    public void handleF5Action() {
        cpuController.handleButtonStartAction();
    }

    /**
     * Krokovanie.
     */
    @FXML
    public void handleF7Action() {
        cpuController.handleButtonStepAction();
    }

    /**
     * Pauza.
     */
    @FXML
    public void handleF9Action() {
        cpuController.handleButtonPauseAction();
    }

    /**
     * Zastavenie programu.
     */
    @FXML
    public void handleF10Action() {
        cpuController.handleButtonStopAction();
    }

    /**
     * Reset programu.
     */
    @FXML
    public void handleF12Action() {
        cpuController.handleButtonResetAction();
    }

    /**
     * Výstraha pre užívateľa s otázkou na ďalší postup, ak aktuálny obvod nie je uložený.
     *
     * @return true - volajúca procedúra môže pokračovať, false - užívateľ nechce pokračovať
     */
    public boolean continueIfUnsavedFile() {
        if (!board.hasChanged()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potvrdenie");
        alert.setHeaderText("Zmeny vo vašom obovde neboli uložené");
        alert.setContentText("Prajete si uložiť zmeny?");

        ButtonType btnTypeSave = new ButtonType("Uložiť");
        ButtonType btnTypeSaveAs = new ButtonType("Uložiť ako");
        ButtonType btnTypeNo = new ButtonType("Nie");
        ButtonType btnTypeCancel = new ButtonType("Zrušiť");

        alert.getButtonTypes().clear();
        if (currentFile != null) alert.getButtonTypes().add(btnTypeSave);
        alert.getButtonTypes().addAll(btnTypeSaveAs, btnTypeNo, btnTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btnTypeCancel) {
                return false;
            } else if (result.get() == btnTypeSaveAs) {
                return saveCircuit(true);
            } else if (result.get() == btnTypeSave) {
                return saveCircuit(false);
            }
        }

        return true;
    }

    /**
     * Uloženie do súboru.
     *
     * @param saveAs Uložiť ako?
     * @return True ak sa podarilo uložiť zapojenie do súboru, false inak.
     */
    private boolean saveCircuit(boolean saveAs) {
        File file = saveAs ? null : currentFile;

        if (currentFile == null || saveAs) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Uložiť obvod" + (saveAs ? " ako" : "") + "..");
            chooser.setInitialDirectory(new File(Paths.get("").toAbsolutePath().toString()));
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SCHX", "*.schx"));

            if (currentFile != null)
                chooser.setInitialFileName(currentFile.getName());

            file = chooser.showSaveDialog(root.getScene().getWindow());
        }

        if (file != null) {
            if (!saveAs && currentFile != null && currentFile.getName().equals(file.getName())) {
                //opytanie sa ci chce naozaj prepisat subor
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Potvrdenie");
                alert.setHeaderText("Naozaj si prajete prepísať súbor " + file.getName() + "?");

                ButtonType btnTypeYes = new ButtonType("Áno");
                ButtonType btnTypeNo = new ButtonType("Nie");

                alert.getButtonTypes().clear();
                alert.getButtonTypes().addAll(btnTypeYes, btnTypeNo);

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == btnTypeNo) return false;
            }

            if (board.save(file)) {
                board.clearChange();
                ((Stage) root.getScene().getWindow()).setTitle("Simulátor - " + file.getName());
                currentFile = file;
                return true;
            }
        }

        return false;
    }

    /**
     * Odlozenie handle na CPU controller.
     */
    public void setCPUController(CPUController cpuController) {
        this.cpuController = cpuController;
    }

    /**
     * Povolenie/zakazanie tlacidiel.
     */
    public void setButtons(int typ) {
        switch(typ)
        {
            case 0 :                                // inicializacia
                btnF5spusti.setDisable(false);
                btnF7krok.setDisable(false);
                btnF9pauza.setDisable(true);
                btnF10stop.setDisable(true);
                btnF12reset.setDisable(true);
                break;

            case 1:                                 // beziaci program
                btnF5spusti.setDisable(true);
                btnF7krok.setDisable(true);
                btnF9pauza.setDisable(false);
                btnF10stop.setDisable(false);
                btnF12reset.setDisable(true);
                // zobrat focus z tlacidla (aby stlacenie medzery nesposobilo koniec programu, ak je focus na Stop)
                if (cpuController.breadboardStage.isShowing())
                    toolsSplitPane.requestFocus();
                break;

            case 2:                                 // pozastaveny program
                btnF5spusti.setDisable(false);
                btnF7krok.setDisable(false);
                btnF9pauza.setDisable(true);
                btnF10stop.setDisable(false);
                btnF12reset.setDisable(true);
                break;

            case 3:                                 // cakajuci program
                btnF5spusti.setDisable(true);
                btnF7krok.setDisable(true);
                btnF9pauza.setDisable(true);
                btnF10stop.setDisable(false);
                btnF12reset.setDisable(true);
                break;

            case 4 :                                // necinny program
                btnF5spusti.setDisable(false);
                btnF7krok.setDisable(false);
                btnF9pauza.setDisable(true);
                btnF10stop.setDisable(true);
                btnF12reset.setDisable(false);
                break;

            default: break;
        }
    }
}
