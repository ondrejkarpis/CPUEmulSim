package sk.uniza.fri.cp.App;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import sk.uniza.fri.cp.App.BreadboardControl.BreadboardController;
import sk.uniza.fri.cp.App.CPUControl.CPUController;
import sk.uniza.fri.cp.SchematicSim.DescriptionPane;
import sk.uniza.fri.cp.SchematicSim.ItemPicker;
import sk.uniza.fri.cp.SchematicSim.Buses.AddressBus16;
import sk.uniza.fri.cp.SchematicSim.Buses.ControlBus;
import sk.uniza.fri.cp.SchematicSim.Buses.DataBus8;
import sk.uniza.fri.cp.SchematicSim.Gates.AndGate;
import sk.uniza.fri.cp.SchematicSim.Gates.NotGate;
import sk.uniza.fri.cp.SchematicSim.Gates.OrGate;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Aplikácia simulátora vývojovej dosky FRI UNIZA a emulátor 8 bitového CPU.
 *
 * @author Tomáš Hianik
 * @created 7.2.2017
 */
public class App extends Application {
    private static final int CPU_WINDOW_WIDTH = 1280;
    private static final int CPU_WINDOW_HEIGHT = 640;

    public static void main(String[] args) {
        launch();
    }
    public static Stage primStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //nacitanie sablony okna CPU
        FXMLLoader CpuLayoutLoader = new FXMLLoader();
        CpuLayoutLoader.setLocation(getClass().getResource("/fxml/Main_CPUEmul.fxml"));
        Parent mainContent = CpuLayoutLoader.load();

        //nacitanie sceny CPU
        Scene mainScene = new Scene(mainContent, CPU_WINDOW_WIDTH, CPU_WINDOW_HEIGHT);

        //priradenie CSS stylov k scene
        mainScene.getStylesheets().add(getClass().getResource("/css/CPUEmul_style.css").toExternalForm());

        //zachytenie vsetkych klaves (okrem funkcnych klaves) na mainScene
        //(http://stackoverflow.com/questions/25397742/javafx-keyboard-event-shortcut-key)
        //a posielanie ich do CPU
        EventHandler<KeyEvent> onKeyPressed = (ke) -> {
            if (ke.getEventType() == KeyEvent.KEY_PRESSED) {
                CPUController controller = CpuLayoutLoader.getController();
                switch (ke.getCode()) {
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                        if (controller.isExecuting()) {
                            controller.keyboardInput(ke);
                            ke.consume();
                        }
                }
            }
        };
        EventHandler<KeyEvent> onKeyTyped = (ke) -> {
            CPUController controller = CpuLayoutLoader.getController();
            if (controller.isExecuting()) {
                controller.keyboardInput(ke);
                ke.consume();
            }
        };

        mainScene.addEventFilter(KeyEvent.KEY_TYPED, onKeyTyped);
        mainScene.addEventFilter(KeyEvent.ANY, onKeyPressed);

        //pri kliknuti na tlacidlo zatvorenia okna
        primaryStage.setOnCloseRequest(event -> {
            if (!((CPUController) CpuLayoutLoader.getController()).exit())
                event.consume(); //ak si to uzivatel rozmysli, nezatvaraj aplikaciu
        });

        primaryStage.setScene(mainScene);
        primaryStage.setTitle("CPU Emulator");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/cpu_icon.png")));
        primaryStage.show();
        primStage = primaryStage;

        //Breadboard
        FXMLLoader breadboardLayoutLoader = new FXMLLoader();
        breadboardLayoutLoader.setLocation(getClass().getResource("/fxml/BreadboardSim.fxml"));
        BreadboardController bc = new BreadboardController();
        breadboardLayoutLoader.setController(bc);

        Parent breadboardContent = breadboardLayoutLoader.load();

        Scene breadboardScene = new Scene(breadboardContent);
        breadboardScene.getStylesheets().add(getClass().getResource("/css/BreadboardSim_style.css").toExternalForm());

        EventHandler<KeyEvent> onKeyPressedBoard = (ke) -> {
            if (ke.getEventType() == KeyEvent.KEY_PRESSED) {

                BreadboardController controller = breadboardLayoutLoader.getController();
                switch (ke.getCode()) {
                    case F5:
                        controller.handleF5Action(); break;
                    case F7:
                        controller.handleF7Action(); break;
                    case F9:
                        controller.handleF9Action(); break;
                    case F10:
                        controller.handleF10Action(); break;
                    case F12:
                        controller.handleF12Action(); break;
                }
            }
        };

        breadboardScene.addEventFilter(KeyEvent.KEY_TYPED, onKeyTyped);
        breadboardScene.addEventFilter(KeyEvent.ANY, onKeyPressedBoard);

        Stage breadboardStage = new Stage();
        breadboardStage.setTitle("Simulátor - Nový obvod");
        breadboardStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/simulator_icon_128.png")));
        breadboardScene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            //ak bol stalceny delete alebo backspace
            if (event.getCode().equals(KeyCode.DELETE) || event.getCode().equals(KeyCode.BACK_SPACE)) {
                        ((BreadboardController) breadboardLayoutLoader.getController()).callDelete();
                    }
                }
        );

        breadboardStage.setScene(breadboardScene);
        ((CPUController) CpuLayoutLoader.getController()).setBreadboardStage(breadboardStage, breadboardLayoutLoader.getController());
        // odlozenie handle na CPU controller
        ((BreadboardController) breadboardLayoutLoader.getController()).setCPUController(CpuLayoutLoader.getController());

        // SchematicSim - okno kreslenia schémy, ktoré otvára tlačidlo Simulátor.
        SchematicSheet schematicSheet = new SchematicSheet(1200, 800, 20);
        ItemPicker itemPicker = new ItemPicker();
        DescriptionPane descriptionPane = new DescriptionPane();
        itemPicker.registerItem(new AndGate());
        itemPicker.registerItem(new OrGate());
        itemPicker.registerItem(new NotGate());
        itemPicker.registerItem(new AddressBus16());
        itemPicker.registerItem(new DataBus8());
        itemPicker.registerItem(new ControlBus());
        itemPicker.setPanelForDescription(descriptionPane);
        schematicSheet.setDescriptionPane(descriptionPane);

        SplitPane schematicContent = new SplitPane(itemPicker, schematicSheet);
        schematicContent.setDividerPositions(0.2);
        VBox descriptionBox = new VBox(new Label("Popis"), descriptionPane);
        VBox.setVgrow(descriptionPane, Priority.ALWAYS);
        BorderPane schematicRoot = new BorderPane(schematicContent);
        schematicRoot.setBottom(descriptionBox);

        Stage schematicStage = new Stage();
        schematicStage.setTitle("Simulátor - Schéma");
        schematicStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/simulator_icon_128.png")));
        Scene schematicScene = new Scene(schematicRoot, 1280, 850);
        schematicScene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                schematicSheet.deleteSelect();
                event.consume();
            }
        });
        schematicStage.setScene(schematicScene);
        ((CPUController) CpuLayoutLoader.getController()).setSchematicStage(schematicStage);
    }
}
