package sk.uniza.fri.cp.BreadboardSim;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * Panel s výberom itemov, ktoré je možné pridať na plochu simulátora.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class ItemPicker extends VBox {

	private ScrollPane contentPane;
    private DescriptionPane descriptionPane;

	private FlowPane devicesPane;
	private FlowPane componentsPane;

    /**
     * Vytvorenie panelu s výberom objektov pre plochu simulátora.
     */
    public ItemPicker(){
        this.contentPane = new ScrollPane();
        VBox.setVgrow(this.contentPane, Priority.ALWAYS);

        //panel s ikonami zariadeni
        this.devicesPane = new FlowPane();
        this.devicesPane.setVgap(5);
        this.devicesPane.setHgap(5);

        //panel s ikonami komponentov
        this.componentsPane = new FlowPane();
        this.componentsPane .setVgap(5);
        this.componentsPane .setHgap(5);

        //tlacidla pre zmenu zobrazovanych itemov
        Button btnDevices = new Button("Súčiastky");
        ;
        btnDevices.setOnMouseClicked(event -> contentPane.setContent(devicesPane));

        Button btnComponents = new Button("Komponenty");
        btnComponents.setOnMouseClicked(event -> contentPane.setContent(componentsPane));

        HBox buttonsBox = new HBox(btnDevices, btnComponents);
        buttonsBox.setSpacing(10);
        buttonsBox.setAlignment(Pos.CENTER);

        //nastavenie defaultneho zobrazenia
        this.contentPane.setContent(this.devicesPane);
        this.contentPane.setFitToWidth(true);

        this.getChildren().addAll(buttonsBox, this.contentPane);
    }

    /**
     * Registrovanie nového itemu.
     *
     * @param item Item, ktorý je možné pridať na plochu.
     */
    public void registerItem(Item item){
        if(item instanceof Device)
            devicesPane.getChildren().add(item);
        else if (item instanceof Component || item instanceof SchoolBreadboard)
            componentsPane.getChildren().add(item);

        item.addEventFilter(MouseEvent.DRAG_DETECTED, event -> 	item.startFullDrag() );
        item.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (this.descriptionPane != null) this.descriptionPane.setDescription(item);
        });
    }

    /**
     * Nastavenie panelu, na ktorom sa zobrazí informácia o objekte po jeho vybratí v panely.
     *
     * @param descriptionPane Panel pre popis.
     */
    public void setPanelForDescription(DescriptionPane descriptionPane) {
        this.descriptionPane = descriptionPane;
    }
}