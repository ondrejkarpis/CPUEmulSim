package sk.uniza.fri.cp.BreadboardSim.Board;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Wire.Joint;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;

import java.util.ArrayList;
import java.util.List;

/**
 * Správca vrstiev plochy simulátora.
 * Obashuje 4 vrstvy od najnižšej - pozadie, komponenty, zariadenia, spojenia.
 * Okrem vrstiev v podobe Pane panelov uchováva objekty v listoch.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardLayersManager {

	private Group layers;

	private Pane backgroundLayer;
	private Pane componentsLayer;
	private Pane devicesLayer;
	private Pane wiresLayer;
    private ArrayList<SchoolBreadboard> schoolBreadboards;
    private ArrayList<Component> components;
	private ArrayList<Device> devices;
	private ArrayList<Wire> wires;

    /**
     * Správca vrstiev plochy simulátora.
     *
     * @param backgound Panel s vygenerovaným pozadím.
     */
    BoardLayersManager(Pane backgound) {
        //inicializacia atributov
		this.backgroundLayer = backgound;
		this.componentsLayer = new Pane();
		this.devicesLayer = new Pane();
		this.wiresLayer = new Pane();
        this.schoolBreadboards = new ArrayList<>();
        this.components = new ArrayList<>();
		this.devices = new ArrayList<>();
		this.wires = new ArrayList<>();

        this.componentsLayer.setMinWidth(this.backgroundLayer.getBoundsInParent().getWidth());
        this.componentsLayer.setMinHeight(this.backgroundLayer.getBoundsInParent().getHeight());
        this.devicesLayer.setMinWidth(this.backgroundLayer.getBoundsInParent().getWidth());
        this.devicesLayer.setMinHeight(this.backgroundLayer.getBoundsInParent().getHeight());

		this.layers = new Group(backgroundLayer, componentsLayer, devicesLayer, wiresLayer);

		this.componentsLayer.setPickOnBounds(false);
		this.devicesLayer.setPickOnBounds(false);
		this.wiresLayer.setPickOnBounds(false);
	}


    /**
     * Najdenie co najmensieho nepouziteho Id komponentu
     */
	private int getComponentId() {
	    int id;
	    boolean found;

	    for(id = 0;;id++)
        {
            found = true;
            for (Component comp : components) {
                if (Integer.parseInt(comp.getId().substring(1)) == id) {
                    found = false;
                    break;
                }
            }
            if (found)
                return id;
        }
    }

    /**
     * Najdenie co najmensieho nepouziteho Id SchoolBreadBoard
     */
    private int getSBBId() {
        int id;
        boolean found;

        for(id = 0;;id++)
        {
            found = true;
            for (SchoolBreadboard schb : schoolBreadboards) {
                if (Integer.parseInt(schb.getId().substring(2)) == id) {
                    found = false;
                    break;
                }
            }
            if (found)
                return id;
        }
    }

    /**
     * Pridanie nového objektu na plochu.
     * Podľa typu sa automaticky priradí do správnej vrstvy.
     *
     * @param object Objekt, ktorý sa má pridať na plochu.
     * @return True, ak bol objekt pridaný, false inak.
     */
    public boolean add(Object object){
		if(object instanceof Wire){
			Wire wire = (Wire) object;
			this.wiresLayer.getChildren().add(wire);
			this.wires.add(wire);
			return true;
		}

		if(object instanceof Component){
			Component component = (Component) object;
            if (component.getId() == null)
                component.setId("c" + getComponentId());
            this.componentsLayer.getChildren().add(component);
            this.components.add(component);
            return true;
		}

		if(object instanceof Device){
			Device device = (Device) object;
			this.devicesLayer.getChildren().add(device);
			this.devices.add(device);
			return true;
		}

        if (object instanceof SchoolBreadboard) {
            SchoolBreadboard schoolBreadboard = (SchoolBreadboard) object;

            //ak nema priradene ID, prirad mu nove
            if (schoolBreadboard.getId() == null)
                schoolBreadboard.setId("sb" + getSBBId());

            this.componentsLayer.getChildren().add(schoolBreadboard);
            this.schoolBreadboards.add(schoolBreadboard);
            //ak sa jedna o schoolbreadboard, pridaj vsetky komponenty do zoznamu ktore obsahuje zvlast
            for (Component schBComponent : schoolBreadboard.getComponents()) {
                if (schBComponent.getId() == null)
                    //ak este nebolo ID nastavene prirad mu nove
                    schBComponent.setId("c" + getComponentId());

                this.components.add(schBComponent);
            }
            return true;
        }

		return false;
	}

    /**
     * Odobratie objektu z plochy.
     *
     * @param object Objekt, ktorý sa má pridať na plochu.
     * @return True, ak bol odobratý, false inak.
     */
    public boolean remove(Object object){
        if(object instanceof Wire){
            Wire wire = (Wire) object;
            this.wiresLayer.getChildren().remove(wire);
            this.wires.remove(wire);
            return true;
        }

        if(object instanceof Joint){
            ((Joint) object).getWire().removeJoint( (Joint)object);
        }

        if(object instanceof Component){
            Component component = (Component) object;
            this.componentsLayer.getChildren().remove(component);
            this.components.remove(component);
            return true;
        }

        if(object instanceof Device){
            Device device = (Device) object;
            this.devicesLayer.getChildren().remove(device);
            this.devices.remove(device);
            return true;
        }

        if (object instanceof SchoolBreadboard) {
            SchoolBreadboard schoolBreadboard = (SchoolBreadboard) object;
            this.componentsLayer.getChildren().remove(schoolBreadboard);
            this.schoolBreadboards.remove(schoolBreadboard);
            schoolBreadboard.getComponents().forEach(Component::delete);
            return true;
        }

        return false;
    }

    /**
     * Vráti vstvu na základe jej názvu.
     * Validné názvy - background, components, devices, wires.
     *
     * @param name Názov vrstvy.
     * @return Panel reprezentujúci danú vrstvu.
     */
    Pane getLayer(String name) {
        switch (name){
			case "background": return backgroundLayer;
			case "components": return componentsLayer;
			case "devices": return devicesLayer;
			case "wires": return wiresLayer;
		}

		return backgroundLayer;
	}

    /**
     * Vráti skupinu panelov slúžiacich ako vrstvy plochy.
     *
     * @return Skupina vrstiev.
     */
    Group getLayers() {
        return layers;
    }

    /**
     * Vráti list s vývojovymi doskami na ploche.
     *
     * @return List s vývojovými doskami na ploche.
     */
    List<SchoolBreadboard> getSchoolBreadboards() {
        return new ArrayList<>(this.schoolBreadboards);
    }

    /**
     * Vráti všetky komponenty na ploche. Medzi komponenty sa neráta vývojová doska, iba jej časti.
     *
     * @return List s komponentmi na ploche.
     */
    public List<Component> getComponents(){
        return new ArrayList<>(this.components);
    }

    /**
     * Vráti zariadenia na ploche.
     *
     * @return List so zariadeniami na ploche.
     */
    public List<Device> getDevices() {
        return new ArrayList<>(this.devices);
    }

    /**
     * Vráti spojenia na ploche.
     *
     * @return List so spojeniami na ploche.
     */
    public List<Wire> getWires() {
        return new ArrayList<>(this.wires);
    }

    /**
     * Odstránenie všetkých objektov na ploche, ktoré sú zmazateľné.
     */
    public void clear() {
        //cistenie kablikov
        new ArrayList<>(this.wires).forEach(Wire::delete);

        //cistenie zariadeni
        new ArrayList<>(this.devices).forEach(Device::delete);

        //cistenie komponentov
        new ArrayList<>(this.schoolBreadboards).forEach(SchoolBreadboard::delete);
        List<Component> toRetain = this.schoolBreadboards.get(0).getComponents();
        new ArrayList<>(this.components).stream().filter(component -> !toRetain.contains(component)).forEach(Component::delete);
    }
}