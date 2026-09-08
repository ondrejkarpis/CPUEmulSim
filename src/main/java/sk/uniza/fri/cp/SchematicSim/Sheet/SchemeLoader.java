package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.scene.paint.Color;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Wire.Joint;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;
import sk.uniza.fri.cp.SchematicSim.Wire.WireEnd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ukladanie / načítanie schémy (súčiastky + vodiče) do XML súboru (.schx).
 * <p>
 * Formát je zámerne jednoduchý - CVO: názov triedy hradla, pozícia na mriežke;
 * vodič: farba, identifikácia koncov (id hradla + index pinu) a pozície zlomov.
 * Hradlá sa vytvárajú reflexiou (musia mať konštruktor {@code (SchematicSheet)}),
 * takže pribúdanie nových súčiastok nevyžaduje zmeny v tomto súbore.
 *
 * @author Claude (návrh podľa BreadboardSim.SchemeLoader)
 */
public class SchemeLoader {

    private static final String VERSION = "1.0";
    private static final String FORGIVEN_ID_PREFIX = "s"; // id pouzite pri nacitani ak chyba ulozene id

    private SchemeLoader() {
    }

    /**
     * Uloženie schémy do súboru.
     *
     * @param file  Súbor, do ktorého sa schéma uloží.
     * @param sheet Plocha schémy.
     * @return true ak sa uloženie podarilo.
     */
    public static boolean save(File file, SchematicSheet sheet) {
        Document jdomDoc = new Document();
        Element rootElement = new Element("Scheme");
        rootElement.setAttribute("ver", VERSION);
        jdomDoc.setRootElement(rootElement);

        //súčiastky
        Element gatesElement = new Element("Gates");
        for (GateSymbol gate : sheet.getGates()) {
            Element gateElement = new Element("Gate");
            gateElement.setAttribute("id", gate.getId());

            Element className = new Element("class");
            className.addContent(gate.getClass().getName());
            gateElement.addContent(className);

            Element gridX = new Element("gridX");
            gridX.addContent(Integer.toString(gate.getGridPosX()));
            gateElement.addContent(gridX);

            Element gridY = new Element("gridY");
            gridY.addContent(Integer.toString(gate.getGridPosY()));
            gateElement.addContent(gridY);

            gatesElement.addContent(gateElement);
        }
        rootElement.addContent(gatesElement);

        //vodiče
        Element wiresElement = new Element("Wires");
        for (Wire wire : sheet.getWires()) {
            Element wireElement = new Element("Wire");
            wireElement.setAttribute("color", colorToHex(wire.getColor()));

            WireEnd[] ends = wire.getEnds();
            appendWireEnd(wireElement, "start", ends[0]);
            appendWireEnd(wireElement, "end", ends[1]);

            Element jointsElement = new Element("Joints");
            for (Joint joint : wire.getJoints()) {
                Element jointElement = new Element("joint");

                Element x = new Element("x");
                x.addContent(Double.toString(joint.getLayoutX()));
                jointElement.addContent(x);

                Element y = new Element("y");
                y.addContent(Double.toString(joint.getLayoutY()));
                jointElement.addContent(y);

                jointsElement.addContent(jointElement);
            }
            wireElement.addContent(jointsElement);
            wiresElement.addContent(wireElement);
        }
        rootElement.addContent(wiresElement);

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            xmlOutputter.output(jdomDoc, bw);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void appendWireEnd(Element wireElement, String name, WireEnd end) {
        Element endElement = new Element(name);
        Pin pin = end.getPin();
        if (pin != null) {
            Element gateId = new Element("gateId");
            gateId.addContent(pin.getOwner().getId());
            endElement.addContent(gateId);

            Element pinIndex = new Element("pinIndex");
            pinIndex.addContent(Integer.toString(pin.getOwner().getPins().indexOf(pin)));
            endElement.addContent(pinIndex);
        } else {
            Element x = new Element("x");
            x.addContent(Double.toString(end.getLayoutX()));
            endElement.addContent(x);

            Element y = new Element("y");
            y.addContent(Double.toString(end.getLayoutY()));
            endElement.addContent(y);
        }
        wireElement.addContent(endElement);
    }

    /**
     * Načítanie schémy zo súboru. Pred načítaním vymaže obsah plochy.
     *
     * @param file  Súbor, z ktorého sa schéma načíta.
     * @param sheet Plocha schémy.
     * @return true ak sa načítanie podarilo.
     */
    public static boolean load(File file, SchematicSheet sheet) {
        SAXBuilder builder = new SAXBuilder();
        try {
            Document jdomDoc = builder.build(file);
            Element rootElement = jdomDoc.getRootElement();
            if (!"Scheme".equalsIgnoreCase(rootElement.getName())) return false;

            sheet.clearSheet();

            //súčiastky
            Map<String, GateSymbol> gatesById = new HashMap<>();
            Element gatesElement = rootElement.getChild("Gates");
            if (gatesElement != null) {
                for (Element gateElement : gatesElement.getChildren("Gate")) {
                    String className = gateElement.getChildText("class");
                    GateSymbol gate = (GateSymbol) Class.forName(className)
                            .getConstructor(SchematicSheet.class).newInstance(sheet);

                    String id = gateElement.getAttributeValue("id");
                    gate.setId(id != null && !id.isEmpty() ? id : FORGIVEN_ID_PREFIX + gatesById.size());
                    sheet.addItem(gate);

                    int gridX = Integer.parseInt(gateElement.getChildText("gridX"));
                    int gridY = Integer.parseInt(gateElement.getChildText("gridY"));
                    gate.moveTo(gridX, gridY);
                    sheet.getOccupancy().occupy(gate);

                    gatesById.put(gate.getId(), gate);
                }
            }

            //vodiče
            Element wiresElement = rootElement.getChild("Wires");
            if (wiresElement != null) {
                for (Element wireElement : wiresElement.getChildren("Wire")) {
                    Wire wire = new Wire(sheet);
                    sheet.addItem(wire);
                    wire.changeColor(Color.valueOf(colorFromHex(wireElement.getAttributeValue("color"))));
                    WireEnd[] ends = wire.getEnds();

                    Pin startPin = findPin(ends[0].getSheet(), wireElement.getChild("start"), gatesById);
                    Pin endPin = findPin(ends[1].getSheet(), wireElement.getChild("end"), gatesById);
                    if (startPin == null || endPin == null
                            || startPin.isOccupied() || endPin.isOccupied()) {
                        wire.delete();
                        continue;
                    }

                    ends[0].connect(startPin);
                    ends[1].connect(endPin);

                    Element jointsElement = wireElement.getChild("Joints");
                    if (jointsElement != null) {
                        for (Element jointElement : jointsElement.getChildren("joint")) {
                            double x = Double.parseDouble(jointElement.getChildText("x"));
                            double y = Double.parseDouble(jointElement.getChildText("y"));
                            wire.splitLastSegment().moveTo(x, y);
                        }
                    }
                }
            }
        } catch (JDOMException | IOException | InvocationTargetException | InstantiationException
                | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static Pin findPin(SchematicSheet sheet, Element endElement, Map<String, GateSymbol> gatesById) {
        if (endElement == null) return null;

        String gateId = endElement.getChildText("gateId");
        if (gateId == null) return null; // voľný koniec - nenačítavame vodiče so slobodnými koncami

        GateSymbol gate = gatesById.get(gateId);
        if (gate == null) return null;

        try {
            int pinIndex = Integer.parseInt(endElement.getChildText("pinIndex"));
            List<Pin> pins = gate.getPins();
            if (pinIndex >= 0 && pinIndex < pins.size()) return pins.get(pinIndex);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private static String colorFromHex(String hex) {
        return hex != null && hex.startsWith("#") ? hex : "#000000";
    }
}