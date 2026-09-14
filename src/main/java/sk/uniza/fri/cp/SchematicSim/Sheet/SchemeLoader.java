package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Side;
import sk.uniza.fri.cp.SchematicSim.Wire.Joint;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;
import sk.uniza.fri.cp.SchematicSim.Wire.WireEnd;
import sk.uniza.fri.cp.SchematicSim.Wire.WireJunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ukladanie / načítanie schémy (súčiastky + vodiče) do XML súboru (.schx).
 * <p>
 * Formát je zámerne jednoduchý - CVO: názov triedy hradla, pozícia na mriežke a generická
 * mapa vlastností {@code <property name value>}, ktorú si každá súčiastka serializuje sama
 * (pozri {@link sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol#saveProperties()});
 * vodič: farba, identifikácia koncov (id hradla + index pinu) a pozície zlomov.
 * Hradlá sa vytvárajú reflexiou (musia mať konštruktor {@code (SchematicSheet)}),
 * takže pribúdanie nových súčiastok nevyžaduje zmeny v tomto súbore.
 *
 * @author Claude (návrh podľa BreadboardSim.SchemeLoader)
 */
public class SchemeLoader {

    private static final String VERSION = "2.0";
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

            Element propertiesElement = new Element("properties");
            for (Map.Entry<String, String> property : gate.saveProperties().entrySet()) {
                Element propertyElement = new Element("property");
                propertyElement.setAttribute("name", property.getKey());
                propertyElement.setAttribute("value", property.getValue());
                propertiesElement.addContent(propertyElement);
            }
            gateElement.addContent(propertiesElement);

            gatesElement.addContent(gateElement);
        }
        rootElement.addContent(gatesElement);

        //vodiče
        Element wiresElement = new Element("Wires");
        for (Wire wire : sheet.getWires()) {
            Element wireElement = new Element("Wire");
            wireElement.setAttribute("color", colorToHex(wire.getColor()));
            if (wire.getBranchExit() != null) {
                wireElement.setAttribute("branchExit", wire.getBranchExit().name());
            }

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
        } else if (end.getJunction() != null) {
            Element junction = new Element("junction");

            Element x = new Element("x");
            x.addContent(Double.toString(end.getJunction().getLayoutX()));
            junction.addContent(x);

            Element y = new Element("y");
            y.addContent(Double.toString(end.getJunction().getLayoutY()));
            junction.addContent(y);

            endElement.addContent(junction);
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

                    gate.loadProperties(readProperties(gateElement));

                    gatesById.put(gate.getId(), gate);
                }
            }

            //vodiče
            Element wiresElement = rootElement.getChild("Wires");
            if (wiresElement != null) {
                List<Wire> loadedWires = new ArrayList<>();
                List<WireEntry> pendingResolve = new ArrayList<>();
                // zdieľané spájače: konce vodičov v jednom bode sa napoja na rovnaký hub
                Map<Long, WireJunction> hubByKey = new HashMap<>();
                for (Element wireElement : wiresElement.getChildren("Wire")) {
                    WireEntry entry = loadWire(sheet, wireElement, gatesById, loadedWires, hubByKey);
                    if (entry == null) continue;

                    if (entry.wire.areBothEndsConnected()) {
                        loadedWires.add(entry.wire);
                    } else {
                        pendingResolve.add(entry);
                    }
                }

                // druhý prechod - vodiče so spájačom na konci, ktoré boli uložené PRED kmeňovým
                // vodičom (v praxi sa to nestáva, ale kvôli robustnosti to ošetrujeme)
                List<WireEntry> stillPending = new ArrayList<>();
                for (WireEntry entry : pendingResolve) {
                    resolveJunctionEnd(entry.wire.getEnds()[0], entry.startElement, loadedWires, hubByKey);
                    resolveJunctionEnd(entry.wire.getEnds()[1], entry.endElement, loadedWires, hubByKey);

                    if (entry.wire.areBothEndsConnected()) {
                        loadedWires.add(entry.wire);
                    } else {
                        stillPending.add(entry);
                    }
                }
                for (WireEntry entry : stillPending) {
                    entry.wire.delete();
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

    /**
     * Pomocná trieda spájajúca načítaný vodič s XML elementmi jeho koncov, aby bolo možné
     * vykonať neskorší (druhý) prechod na vyriešenie koncov napojených na spájače.
     */
    private static final class WireEntry {
        final Wire wire;
        final Element startElement;
        final Element endElement;

        WireEntry(Wire wire, Element startElement, Element endElement) {
            this.wire = wire;
            this.startElement = startElement;
            this.endElement = endElement;
        }
    }

    /**
     * Načítanie jedného vodiča: pripojenie koncov na piny, rozdelanie zlomov a prvý pokus
     * o vyriešenie koncov na spájačoch. Vracia záznam pre prípadný druhý prechod, alebo
     * {@code null} ak sa vodič nepodarilo načítať.
     */
    private static WireEntry loadWire(SchematicSheet sheet, Element wireElement,
                                      Map<String, GateSymbol> gatesById, List<Wire> loadedWires,
                                      Map<Long, WireJunction> hubByKey) {
        Element startElement = wireElement.getChild("start");
        Element endElement = wireElement.getChild("end");

        Wire wire = new Wire(sheet);
        sheet.addItem(wire);
        wire.changeColor(Color.valueOf(colorFromHex(wireElement.getAttributeValue("color"))));

        // preferovaný smer prvej úsečky (ulovené pri ťahaní odbočky zo spájača) - vrátime ho,
        // aby router po načítaní zachoval pôvodný tvar (L) a neprehupol ho do predvoleného Z
        String branchExit = wireElement.getAttributeValue("branchExit");
        if (branchExit != null && !branchExit.isEmpty()) {
            try {
                wire.setBranchExit(Side.valueOf(branchExit));
            } catch (IllegalArgumentException ignored) {
            }
        }

        WireEnd[] ends = wire.getEnds();

        connectPinEnd(ends[0], startElement, gatesById);
        connectPinEnd(ends[1], endElement, gatesById);

        Element jointsElement = wireElement.getChild("Joints");
        if (jointsElement != null) {
            for (Element jointElement : jointsElement.getChildren("joint")) {
                double x = Double.parseDouble(jointElement.getChildText("x"));
                double y = Double.parseDouble(jointElement.getChildText("y"));
                wire.splitLastSegment().moveTo(x, y);
            }
        }

        resolveJunctionEnd(ends[0], startElement, loadedWires, hubByKey);
        resolveJunctionEnd(ends[1], endElement, loadedWires, hubByKey);

        return new WireEntry(wire, startElement, endElement);
    }

    private static Map<String, String> readProperties(Element gateElement) {
        Map<String, String> properties = new HashMap<>();
        Element propertiesElement = gateElement.getChild("properties");
        if (propertiesElement != null) {
            for (Element propertyElement : propertiesElement.getChildren("property")) {
                String name = propertyElement.getAttributeValue("name");
                String value = propertyElement.getAttributeValue("value");
                if (name != null) properties.put(name, value == null ? "" : value);
            }
        }
        return properties;
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

    private static void connectPinEnd(WireEnd end, Element endElement, Map<String, GateSymbol> gatesById) {
        if (endElement == null || end.isConnected()) return;
        Pin pin = findPin(end.getSheet(), endElement, gatesById);
        if (pin != null && !pin.isOccupied()) {
            end.connect(pin);
        }
    }

    /**
     * Pripojenie konca vodiča, ktorý je v súbore uložený ako spájač (junction) na iný vodič.
     * <p>
     * V novom modeli sa kmeňový vodič v mieste spájača rozdeľuje na DVA vodiče a všetky
     * konce zdieľajúce rovnaký bod sa napájajú na jeden spoločný spájač (hub) - preto sa
     * spájače najprv zdieľajú cez mapu {@code hubByKey} (všetci pripojení v danom bode
     * dostanú ten istý objekt). Ak už v bode hub existuje, pripojí sa naň.
     * <p>
     * Pre staré súbory (kde kmeň cez bod len prechádza) sa ako poistka hľadá kmeňový vodič
     * prechádzajúci bodom a na ňom sa spájač vytvorí rozdeľujúcim {@code createJunction}.
     *
     * @return true ak sa koniec podarilo pripojiť na spájač.
     */
    private static boolean resolveJunctionEnd(WireEnd end, Element endElement, List<Wire> loadedWires,
                                              Map<Long, WireJunction> hubByKey) {
        if (endElement == null || end.isConnected()) return true;

        Element junctionElement = endElement.getChild("junction");
        if (junctionElement == null) return false;

        double x;
        double y;
        try {
            x = Double.parseDouble(junctionElement.getChildText("x"));
            y = Double.parseDouble(junctionElement.getChildText("y"));
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }

        Point2D pos = new Point2D(x, y);
        long key = keyOf(x, y);

        // nový model: spájač už môže byť vytvorený iným koncom v rovnakom bode
        WireJunction shared = hubByKey.get(key);
        if (shared != null && !shared.isRemoved()) {
            end.connect(shared);
            return true;
        }

        // existujúci hub presne v bode (najmä starý formát, kde hub zapísal prvý z koncov)
        for (Wire loadedWire : loadedWires) {
            if (loadedWire == end.getWire()) continue;
            WireJunction existing = loadedWire.findJunctionAt(pos);
            if (existing != null) {
                hubByKey.put(key, existing);
                end.connect(existing);
                return true;
            }
        }

        // starý formát: kmeňový vodič len prechádza bodom - rozdelí sa na dva vodiče
        for (Wire loadedWire : loadedWires) {
            if (loadedWire == end.getWire()) continue;
            if (loadedWire.findSegmentNear(pos) != null) {
                WireJunction junction = loadedWire.createJunction(pos);
                if (junction != null) {
                    hubByKey.put(key, junction);
                    end.connect(junction);
                    return true;
                }
            }
        }

        // nový model (koniec je na mieste, kde vodič končí): vytvoríme samostatný hub
        WireJunction standalone = end.getWire().createStandaloneHub(pos);
        if (standalone != null && !standalone.isRemoved()) {
            hubByKey.put(key, standalone);
            end.connect(standalone);
            return true;
        }

        return false;
    }

    /**
     * Kľúč bodu pre zdieľanie spájačov pri načítaní. Body sú zarovnané na mriežku,
     * preto stačí celočíselné zaokrúhlenie.
     */
    private static long keyOf(double x, double y) {
        return Math.round(x) * 1000003L + Math.round(y);
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