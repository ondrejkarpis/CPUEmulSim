package sk.uniza.fri.cp.BreadboardSim.Board;

import javafx.scene.paint.Color;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Wire.Joint;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;
import sk.uniza.fri.cp.BreadboardSim.Wire.WireEnd;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Statické metódy na načítavanie / ukladanie zapojení simulátora.
 *
 * @author Tomáš Hianik
 * @created 8.5.2017
 */
class SchemeLoader {
    private static final String VERSION = "1.1";
    private static final String COMPONENTS_PACKAGE = Component.class.getPackage().getName() + ".";
    private static final String DEVICES_PACKAGE = Device.class.getPackage().getName() + ".";

    /**
     * Uloženie zapojenia podľa layerManager-a.
     *
     * @param file          Súbor, do ktorého sa má zapojenie uložiť.
     * @param layersManager LayerManager s objektami na ploche, ktoré chceme uložiť.
     * @return Výsledok operácie, true, ak bol súbor úspešne uložený, false inak.
     */
    static boolean save(File file, BoardLayersManager layersManager) {
        return saveSchx(file, layersManager);
    }

    /**
     * Načítanie zapojenia zo súboru.
     * Automaticky rozpoznáva príponu .sch a .schx.
     *
     * @param file          Súbor, z ktorého sa má zapojenie načítať.
     * @param layersManager LayerManager, do ktorého sa majú objekty načítať.
     * @return Výsledok operácie, true ak sa načítavanie podarilo, false inak.
     */
    static boolean load(File file, BoardLayersManager layersManager) {
        if (file.getName().substring(file.getName().lastIndexOf(".")).equalsIgnoreCase(".sch"))
            return loadSch(file, layersManager);

        return loadSchx(file, layersManager);
    }

    private static boolean saveSchx(File schxFile, BoardLayersManager layersManager) {
        Document jdomDoc = new Document();
        Element rootElement = new Element("Board");
        jdomDoc.setRootElement(rootElement);

        Element gridX;
        Element gridY;

        //verzia
        rootElement.setAttribute("ver", VERSION);

        //pozicia zobrazenia plochy
        Board board = layersManager.getComponents().get(0).getBoard();
        Element view = new Element("View");
        view.setAttribute("hValue", Double.toString(board.getHvalue()));
        view.setAttribute("vValue", Double.toString(board.getVvalue()));

        rootElement.addContent(view);

        //vyvojove dosky
        Element schoolBreadboardsElement = new Element("SchoolBreadboards");

        //ziskanie vývojových dosiek na ploche
        List<SchoolBreadboard> schoolBreadboardsList = layersManager.getSchoolBreadboards();
        //pre kazdu vyv. dosku
        for (SchoolBreadboard schoolBreadboard : schoolBreadboardsList) {
            //element pre dosku
            Element schoolBreadboardElement = new Element("SchoolBreadboard");

            //atribut ID
            schoolBreadboardElement.setAttribute("id", schoolBreadboard.getId());

            //pozicia X na gride
            gridX = new Element("gridX");
            gridX.addContent(Integer.toString(schoolBreadboard.getGridPosX()));
            schoolBreadboardElement.addContent(gridX);

            //pozicia Y na gride
            gridY = new Element("gridY");
            gridY.addContent(Integer.toString(schoolBreadboard.getGridPosY()));
            schoolBreadboardElement.addContent(gridY);

            int hsIndex = 0;
            //idcka komponentov dosky
            for (Component component : schoolBreadboard.getComponents()) {//skratka pre component
                String elementName = componentShortcut(component);
                if (elementName != null) {
                    if (elementName.equalsIgnoreCase("hs")) elementName += hsIndex++; //index 7-segmentovky
                    Element element = new Element(elementName);
                    element.setAttribute("id", component.getId());
                    schoolBreadboardElement.addContent(element);
                } // inak chyba}
            }

            //zaradenie pod element SchoolBraedboards
            schoolBreadboardsElement.addContent(schoolBreadboardElement);
        }
        //pridanie elemenut SchoolBreadboards pod komponent Board
        rootElement.addContent(schoolBreadboardsElement);


        //komponenty
        Element componentsElement = new Element("Components");

        //ziskanie komponentov na ploche
        List<Component> componentsList = layersManager.getComponents();
        //odstranenie komponentov vyvojovych dosiek zo zoznamu
        schoolBreadboardsList.forEach(list -> componentsList.removeAll(list.getComponents()));
        //vytvaranie elementov pre kazdy zvisny komponent
        for (Component component : componentsList) {
            //element pre komponent
            Element componentElement = new Element("Component");
            //atribut ID
            componentElement.setAttribute("id", component.getId());

            //nazov triedy
            Element className = new Element("class");
            className.addContent(component.getClass().getName().replace(COMPONENTS_PACKAGE, ""));
            componentElement.addContent(className);

            //pozicia X na gride
            gridX = new Element("gridX");
            gridX.addContent(Integer.toString(component.getGridPosX()));
            componentElement.addContent(gridX);

            //pozicia Y na gride
            gridY = new Element("gridY");
            gridY.addContent(Integer.toString(component.getGridPosY()));
            componentElement.addContent(gridY);

            //zaradenie pod element Components
            componentsElement.addContent(componentElement);
        }
        //pridanie elemenut Components pod komponents Board
        rootElement.addContent(componentsElement);


        //zariadenia
        Element devicesElement = new Element("Devices");

        List<Device> devicesList = layersManager.getDevices();
        //pre kazde zariadenie na ploche
        for (Device device : devicesList) {
            //element pre zariadenie
            Element deviceElement = new Element("Device");

            //nazov triedy
            Element className = new Element("class");
            className.addContent(device.getClass().getName().replace(DEVICES_PACKAGE, ""));
            deviceElement.addContent(className);

            //pozicia X na gride
            gridX = new Element("gridX");
            gridX.addContent(Integer.toString(device.getGridPosX()));
            deviceElement.addContent(gridX);

            //pozicia Y na gride
            gridY = new Element("gridY");
            gridY.addContent(Integer.toString(device.getGridPosY()));
            deviceElement.addContent(gridY);

            //element pre piny zariadenia
            Element pinsElement = new Element("pins");
            if (device.isConnected()) {
                List<Pin> pinsList = device.getPins();
                for (int pinIndex = 0; pinIndex < pinsList.size(); pinIndex++) {
                    //element pre pin
                    Element pinElement = new Element("pin");
                    //atribut cislo pinu
                    pinElement.setAttribute("number", Integer.toString(pinIndex + 1));

                    //soket
                    Socket connectedSocket = pinsList.get(pinIndex).getSocket();

                    //id komponentu ku ktoremu je pripojeny
                    Element connectedComponentId = new Element("componentId");
                    connectedComponentId.addContent(connectedSocket.getComponent().getId());
                    pinElement.addContent(connectedComponentId);

                    //id soketu ku ktoremu je pripojeny
                    Element connectedSocketId = new Element("socketId");
                    connectedSocketId.addContent(connectedSocket.getId());
                    pinElement.addContent(connectedSocketId);

                    pinsElement.addContent(pinElement);
                }
            }
            deviceElement.addContent(pinsElement);

            devicesElement.addContent(deviceElement);
        }
        rootElement.addContent(devicesElement);


        //kabliky
        Element wiresElement = new Element("Wires");

        List<Wire> wiresList = layersManager.getWires();
        //pre kazy kablik
        for (Wire wire : wiresList) {
            Element wireElement = new Element("Wire");
            //atribut s farbou
            wireElement.setAttribute("color", wire.getColor().toString());

            //konce kablika
            WireEnd[] ends = wire.getEnds();

            //zaciatocny soket
            Socket startSocket = ends[0].getSocket();
            //ak je pripojeny zaciatok kablika k soketu
            if (startSocket != null) {
                Element startElement = new Element("start");

                Element connectedComponentId = new Element("componentId");
                connectedComponentId.addContent(startSocket.getComponent().getId());
                startElement.addContent(connectedComponentId);

                Element connectedSocketId = new Element("socketId");
                connectedSocketId.addContent(startSocket.getId());
                startElement.addContent(connectedSocketId);

                wireElement.addContent(startElement);
            } else {
                //ak koniec nie je pripojeny, uloz jeho poziciu
                Element startElement = new Element("start");

                //x-ova pozicia na gride
                gridX = new Element("gridX");
                gridX.addContent(Integer.toString(ends[0].getGridPosX()));
                startElement.addContent(gridX);

                //y-ova pozicia na gride
                gridY = new Element("gridY");
                gridY.addContent(Integer.toString(ends[0].getGridPosY()));
                startElement.addContent(gridY);

                wireElement.addContent(startElement);
            }

            //koncovy soket
            Socket endSocket = ends[1].getSocket();
            //ak je pripojeny zaciatok kablika k soketu
            if (endSocket != null) {
                Element endElement = new Element("end");

                Element connectedComponentId = new Element("componentId");
                connectedComponentId.addContent(endSocket.getComponent().getId());
                endElement.addContent(connectedComponentId);

                Element connectedSocketId = new Element("socketId");
                connectedSocketId.addContent(endSocket.getId());
                endElement.addContent(connectedSocketId);

                wireElement.addContent(endElement);
            } else {
                //ak koniec nie je pripojeny, uloz jeho poziciu
                Element endElement = new Element("end");

                //x-ova pozicia na gride
                gridX = new Element("gridX");
                gridX.addContent(Integer.toString(ends[1].getGridPosX()));
                endElement.addContent(gridX);

                //y-ova pozicia na gride
                gridY = new Element("gridY");
                gridY.addContent(Integer.toString(ends[1].getGridPosY()));
                endElement.addContent(gridY);

                wireElement.addContent(endElement);
            }

            //jointy
            List<Joint> jointsList = wire.getJoints();
            Element jointsElement = new Element("Joints");
            if (jointsList.size() > 0) {

                for (Joint joint : jointsList) {
                    //element pre joint
                    Element jointElement = new Element("joint");

                    //x-ova pozicia na gride
                    gridX = new Element("gridX");
                    gridX.addContent(Integer.toString(joint.getGridPosX()));
                    jointElement.addContent(gridX);

                    //y-ova pozicia na gride
                    gridY = new Element("gridY");
                    gridY.addContent(Integer.toString(joint.getGridPosY()));
                    jointElement.addContent(gridY);

                    jointsElement.addContent(jointElement);
                }

            }
            wireElement.addContent(jointsElement);

            wiresElement.addContent(wireElement);
        }
        rootElement.addContent(wiresElement);


        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());

        try (BufferedWriter bw =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(schxFile), StandardCharsets.UTF_8))) {
            xmlOutputter.output(jdomDoc, bw);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private static boolean loadSchx(File schx, BoardLayersManager layersManager) {
        Board board = layersManager.getComponents().get(0).getBoard();
        SAXBuilder builder = new SAXBuilder();

        try {
            Document jdomDoc = builder.build(schx);
            Element rootElement = jdomDoc.getRootElement();

            int gridX;
            int gridY;

            //vycistenie plochy
            layersManager.clear();

            //nacitanie verzie (ak uz bola zavedena)
            String version = rootElement.getAttributeValue("ver");
            if (version == null) version = "1.0";

            if (version.equalsIgnoreCase("1.1")) {
                //posunutie zobrazenia plochy
                Element view = rootElement.getChild("View");
                board.setHvalue(Double.parseDouble(view.getAttributeValue("hValue")));
                board.setVvalue(Double.parseDouble(view.getAttributeValue("vValue")));
            }

            //posunutie schoolBreadboard na miesto kde boli ulozene
            if (version.equalsIgnoreCase("1.0")) {
                Element schoolBreadboardElement = rootElement.getChild("SchoolBreadboard");

                gridX = Integer.parseInt(schoolBreadboardElement.getChildText("gridX"));
                gridY = Integer.parseInt(schoolBreadboardElement.getChildText("gridY"));
                layersManager.getSchoolBreadboards().get(0).moveTo(gridX, gridY);
            } else if (version.equalsIgnoreCase("1.1")) {
                Element schoolBreadboardsElement = rootElement.getChild("SchoolBreadboards");
                List<Element> schoolBreadboardsList = schoolBreadboardsElement.getChildren("SchoolBreadboard");

                final SchoolBreadboard defaultBoard = layersManager.getSchoolBreadboards().get(0);

                //prechadzanie vsetkych ulozenych dosiek
                for (Element schoolBreadboardElement : schoolBreadboardsList) {
                    String id = schoolBreadboardElement.getAttributeValue("id");

                    gridX = Integer.parseInt(schoolBreadboardElement.getChildText("gridX"));
                    gridY = Integer.parseInt(schoolBreadboardElement.getChildText("gridY"));

                    if (id.equalsIgnoreCase("sb0")) {
                        //ak sa jedna o defaultnu dosku
                        defaultBoard.moveTo(gridX, gridY);
                    } else {
                        //inak ju musis vytvorit
                        SchoolBreadboard schoolBreadboard = new SchoolBreadboard(board);
                        schoolBreadboard.setId(id);


                        //nastavenie ID komponentov
                        schoolBreadboardElement.getChildren().forEach(element -> {
                            String componentId = element.getAttributeValue("id");
                            switch (element.getName()) {
                                case "bi":
                                    schoolBreadboard.getBusInterface().setId(componentId);
                                    break;
                                case "bb":
                                    schoolBreadboard.getBreadboard().setId(componentId);
                                    break;
                                case "hsp":
                                    schoolBreadboard.getHexSegmentsPanel().setId(componentId);
                                    break;
                                case "hs0":
                                    schoolBreadboard.getHexSegmentsPanel().getComponents().get(0).setId(componentId);
                                    break;
                                case "hs1":
                                    schoolBreadboard.getHexSegmentsPanel().getComponents().get(1).setId(componentId);
                                    break;
                                case "hs2":
                                    schoolBreadboard.getHexSegmentsPanel().getComponents().get(2).setId(componentId);
                                    break;
                                case "hs3":
                                    schoolBreadboard.getHexSegmentsPanel().getComponents().get(3).setId(componentId);
                                    break;
                                case "pr":
                                    schoolBreadboard.getProbe().setId(componentId);
                                    break;
                                case "nk":
                                    schoolBreadboard.getNumKeys().setId(componentId);
                                    break;
                            }
                        });

                        layersManager.add(schoolBreadboard);

                        schoolBreadboard.moveTo(gridX, gridY);
                    }
                }
            }


            //nacitanie komponentov

            Element componentsElement = rootElement.getChild("Components");
            List<Element> componentsElementsList = componentsElement.getChildren("Component");
            //prechadzanie jednotlivych elementov komponentov
            for (Element componentElement : componentsElementsList) {
                String className = COMPONENTS_PACKAGE + componentElement.getChild("class").getContent(0).getValue();
                Component component = (Component) Class.forName(className).getConstructor(Board.class).newInstance(board);
                component.setId(componentElement.getAttribute("id").getValue());
                layersManager.add(component);

                gridX = Integer.parseInt(componentElement.getChildText("gridX"));
                gridY = Integer.parseInt(componentElement.getChildText("gridY"));

                component.moveTo(gridX, gridY);
            }


            //nacitanie zariadeni
            List<Component> componentsOnBoard = layersManager.getComponents(); //pre pripajanie pinov k soketom

            Element devicesElement = rootElement.getChild("Devices");
            List<Element> devicesElementsList = devicesElement.getChildren("Device");
            for (Element deviceElement : devicesElementsList) {
                String className = DEVICES_PACKAGE + deviceElement.getChild("class").getContent(0).getValue();
                Device device = (Device) Class.forName(className).getConstructor(Board.class).newInstance(board);
                layersManager.add(device);

                //nastavenie pozicie
                gridX = Integer.parseInt(deviceElement.getChildText("gridX"));
                gridY = Integer.parseInt(deviceElement.getChildText("gridY"));
                device.moveTo(gridX, gridY);

                //pripojenie pinov k soketom
                Element pinsElement = deviceElement.getChild("pins");
                List<Element> pinElementstList = pinsElement.getChildren("pin");
                for (Element pinElement : pinElementstList) {
                    int pinNumber = Integer.parseInt(pinElement.getAttributeValue("number"));
                    String componentId = pinElement.getChildText("componentId");
                    int socketId = Integer.parseInt(pinElement.getChildText("socketId")); //id je jeho index

                    //hladanie komponentu ku ktoremu sa ma pripojit (klasicky foreach, nie je predpoklad velkeho mnozstva komponentov)
                    for (Component component : componentsOnBoard) {
                        if (component.getId().equalsIgnoreCase(componentId)) {
                            //pripojenie pinu zariadenia k soketu na komponente
                            component.getSocket(socketId).connect(
                                    device.getPins().get(pinNumber - 1));//cislo pinu -> index pinu

                            //priradenie zariadenia ku komponentu podla prveho pinu
                            if (pinNumber == 1)
                                component.addDevice(device);
                            break;
                        }
                    }
                }
            }


            //nacitanie kablikov
            Element wiresElement = rootElement.getChild("Wires");
            List<Element> wiresElementsList = wiresElement.getChildren("Wire");
            for (Element wireElement : wiresElementsList) {
                //vytvorenie kablika, konce nie su nikam pripojene
                Wire wire = new Wire(board);
                layersManager.add(wire);
                WireEnd[] wireEnds = wire.getEnds();

                //nastavenie farby
                Color wireColor = Color.valueOf(wireElement.getAttributeValue("color"));
                wire.changeColor(wireColor);

                //zaciatok kablika
                Element startElement = wireElement.getChild("start");
                //ak je pripojeny k soketu
                String componentId = startElement.getChildText("componentId");
                if (componentId != null) {
                    //zaciatok je pripojeny k soketu
                    int socketId = Integer.parseInt(startElement.getChildText("socketId"));

                    for (Component component : componentsOnBoard) {
                        if (component.getId().equalsIgnoreCase(componentId)) {
                            wireEnds[0].connectSocket(component.getSocket(socketId));
                            break;
                        }
                    }

                } else {
                    //zaciatok nie je pripojeny k soketu, ma iba poziciu
                    gridX = Integer.parseInt(startElement.getChildText("gridX"));
                    gridY = Integer.parseInt(startElement.getChildText("gridY"));
                    wireEnds[0].moveTo(gridX, gridY);
                }


                //koniec kablika
                Element endElement = wireElement.getChild("end");
                //ak je pripojeny k soketu
                componentId = endElement.getChildText("componentId");
                if (componentId != null) {
                    //zaciatok je pripojeny k soketu
                    int socketId = Integer.parseInt(endElement.getChildText("socketId"));

                    for (Component component : componentsOnBoard) {
                        if (component.getId().equalsIgnoreCase(componentId)) {
                            wireEnds[1].connectSocket(component.getSocket(socketId));
                            break;
                        }
                    }

                } else {
                    //zaciatok nie je pripojeny k soketu, ma iba poziciu
                    gridX = Integer.parseInt(endElement.getChildText("gridX"));
                    gridY = Integer.parseInt(endElement.getChildText("gridY"));
                    wireEnds[1].moveTo(gridX, gridY);
                }


                //jointy
                Element jointsElement = wireElement.getChild("Joints");
                List<Element> jointsElementsList = jointsElement.getChildren("joint");
                for (Element jointElement : jointsElementsList) {
                    gridX = Integer.parseInt(jointElement.getChildText("gridX"));
                    gridY = Integer.parseInt(jointElement.getChildText("gridY"));
                    wire.splitLastSegment().moveTo(gridX, gridY);
                }
            }

        } catch (JDOMException | IOException | InvocationTargetException
                | InstantiationException | IllegalAccessException | ClassNotFoundException
                | NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    private static boolean loadSch(File sch, BoardLayersManager layersManager) {
        Board board = layersManager.getComponents().get(0).getBoard();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(sch), StandardCharsets.UTF_8))) {
            //vycistenie plochy
            layersManager.clear();

            //skolska doska
            SchoolBreadboard schoolBreadboard = layersManager.getSchoolBreadboards().get(0);

            //nacitanie IC
            int chipCount = Integer.parseInt(br.readLine());

            for (int i = 0; i < chipCount; i++) {
                //kazdy IC ma umiestnenie a typ
                //umiestnenie
                int chipPosition = Integer.parseInt(br.readLine());
                //typ
                int chipType = Integer.parseInt(br.readLine());

                Chip chip;
                switch (chipType) {
                    case 0:
                        chip = new Gen7400(board);
                        break;
                    case 1:
                        chip = new Gen7402(board);
                        break;
                    case 2:
                        chip = new Gen7404(board);
                        break;
                    case 3:
                        chip = new Gen7408(board);
                        break;
                    case 4:
                        chip = new Gen7410(board);
                        break;
                    case 5:
                        chip = new Gen7430(board);
                        break;
                    case 6:
                        chip = new Gen7432(board);
                        break;
                    case 7:
                        chip = new Gen7486(board);
                        break;
                    case 8:
                        chip = new SN74125(board);
                        break;
                    case 9:
                        chip = new SN74138(board);
                        break;
                    case 10:
                        chip = new SN74151(board);
                        break;
                    case 11:
                        chip = new SN74153(board);
                        break;
                    case 12:
                        chip = new SN74164(board);
                        break;
                    case 13:
                        chip = new SN74148(board);
                        break;
                    case 14:
                        chip = new SN74573(board);
                        break;
                    case 15:
                        chip = new U6264B(board, 3);
                        break;
                    default:
                        return false;
                }

                layersManager.add(chip);

                int gridX = schoolBreadboard.getGridPosX() + 4 + chipPosition;
                int gridY = schoolBreadboard.getGridPosY() + 15;

                chip.moveTo(gridX, gridY);

                chip.searchForSockets();
                chip.tryToConnectToFoundSockets();
            }

            //nacitanie kablikov
            //pocet kablikov
            int wiresCount = Integer.parseInt(br.readLine());
            for (int i = 0; i < wiresCount; i++) {
                int startX = Integer.parseInt(br.readLine());
                int startY = Integer.parseInt(br.readLine());
                int endX = Integer.parseInt(br.readLine());
                int endY = Integer.parseInt(br.readLine());
                String colorRGB = br.readLine();

                Color wireColor = Color.BLACK;

                if (colorRGB != null)
                    wireColor = decodeOldColor(Integer.parseInt(colorRGB));

                //vytvorenie kablika, konce nie su nikam pripojene
                Wire wire = new Wire(board);
                wire.changeColor(wireColor);
                layersManager.add(wire);
                WireEnd[] wireEnds = wire.getEnds();

                Socket startSocket = getSocketAtPosOnFirstSchoolBreadboard(startX, startY, layersManager);
                Socket endSocket = getSocketAtPosOnFirstSchoolBreadboard(endX, endY, layersManager);

                if (startSocket == null || endSocket == null) return false;

                wireEnds[0].connectSocket(startSocket);
                wireEnds[1].connectSocket(endSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static Socket getSocketAtPosOnFirstSchoolBreadboard(int posX, int posY, BoardLayersManager layersManager) {
        SchoolBreadboard schoolBreadboard = layersManager.getSchoolBreadboards().get(0);
        int index;

        if (posY == 1) {
            //zbernica
            if (posX == 6 || posX == 7) {
                //prvy gnd (33 a 34)
                index = posX + 27;
            } else if (posX >= 14 && posX <= 29) {
                //adresna (15 <-> 0)
                index = Math.abs(posX - 29);
            } else if (posX >= 35 && posX <= 42) {
                //datova (23 <-> 16)
                index = Math.abs(posX - 58);
            } else if (posX >= 44 && posX <= 52) {
                //riadiaca (32 <-> 24)
                index = Math.abs(posX - 76);
            } else if (posX == 55 || posX == 56) {
                //druhy gnd (35 a 36)
                index = posX - 20;
            } else {
                return null;
            }

            return schoolBreadboard.getBusInterface().getSocket(index);

        } else if (posY >= 3 && posY <= 22) {
            //breadboard
            if (posY == 3) {
                //horna modra lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = positionInLine - spaces;
            } else if (posY == 4) {
                //horna cervena lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = 50 + positionInLine - spaces;
            } else if (posY >= 7 && posY <= 11) {
                //horne pole
                index = 100 + 5 * posX + posY - 7;
            } else if (posY >= 14 && posY <= 18) {
                //dolne pole
                index = 415 + 5 * posX + posY - 14;
            } else if (posY == 21) {
                //dolna modra lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = 730 + positionInLine - spaces;
            } else if (posY == 22) {
                //dolna modra lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = 780 + positionInLine - spaces;
            } else {
                return null;
            }

            return schoolBreadboard.getBreadboard().getSocket(index);
        } else if (posY >= 24 && posY <= 32) {
            //spodna cast vyvojovej dosky
            if (posX >= 0 && posX <= 16) {
                //7seg panel
                int segmentIndex = 0;

                if (posX <= 12) {
                    //zapojenie na segmenty
                    switch (posX) {
                        case 0:
                            segmentIndex = 0;
                            break;
                        case 4:
                            segmentIndex = 1;
                            break;
                        case 8:
                            segmentIndex = 2;
                            break;
                        case 12:
                            segmentIndex = 3;
                            break;
                        default:
                            break;
                    }

                    index = posY - 25;

                    return schoolBreadboard.getHexSegmentsPanel().getSegment(segmentIndex).getSocket(index);

                } else if (posX == 15) {
                    //stlpec s vcc a gnd
                    if (posY == 25) {
                        //prvy bod vcc
                        return schoolBreadboard.getHexSegmentsPanel().getSocket(0);
                    } else {
                        //gnd
                        index = 2 - 28 + posY;
                        return schoolBreadboard.getHexSegmentsPanel().getSocket(index);
                    }
                } else if (posX == 16) {
                    //stlpec s VCC a A-ckami
                    if (posY == 25) {
                        //druhy bod vcc
                        return schoolBreadboard.getHexSegmentsPanel().getSocket(1);
                    } else {
                        //zapojenie A-cok
                        segmentIndex = posY - 28;

                        return ((HexSegmentsPanel) schoolBreadboard.getHexSegmentsPanel()).getSegment(segmentIndex).getSocket(8);
                    }
                } else return null;
            } else if (posX == 18) {
                //probe
                return schoolBreadboard.getProbe().getSocket(0);
            } else if (posX >= 46 && posX <= 58) {
                //hw kalvesnica
                if (posY == 24) {
                    //gnd
                    return schoolBreadboard.getNumKeys().getSocket(posX - 55);
                } else if (posY == 25) {
                    //column vcc row

                    if (posX <= 49) {
                        //column
                        return schoolBreadboard.getNumKeys().getSocket(6 + posX - 46);
                    } else if (posX == 52 || posX == 53) {
                        //vcc
                        return schoolBreadboard.getNumKeys().getSocket(4 + posX - 52);
                    } else if (posX >= 55) {
                        //row 10 11 12 13
                        return schoolBreadboard.getNumKeys().getSocket(10 + posX - 55);
                    }
                }
            }
        }

        return null;
    }

    private static Color decodeOldColor(int colorCode) {
        int red = colorCode & 255;
        int green = (colorCode & (255 << 8)) >> 8;
        int blue = (colorCode & (255 << 16)) >> 16;
        return Color.rgb(red, green, blue);
    }

    private static String componentShortcut(Component component) {
        if (component instanceof BusInterface) {
            return "bi";
        } else if (component instanceof Breadboard) {
            return "bb";
        } else if (component instanceof HexSegmentsPanel) {
            return "hsp";
        } else if (component instanceof HexSegment) {
            return "hs";
        } else if (component instanceof Probe) {
            return "pr";
        } else if (component instanceof NumKeys) {
            return "nk";
        }
        return null;
    }
}
