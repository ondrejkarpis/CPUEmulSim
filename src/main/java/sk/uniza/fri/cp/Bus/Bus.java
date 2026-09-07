package sk.uniza.fri.cp.Bus;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Zbernica zabezpečujúca komunikáciu medzi CPU a doskou (fyzickou / simulovanou).
 * Uchováva aktuálne hodnoty na adresnej, dátovej a riadiacej zbernici.
 * Singleton
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Bus{
	private static Bus instance; //inštancia singletonu
    private Semaphore dataSemaphore; //semafor pre cakanie na nastavenie dat simulatorom
    private boolean isSimulationRunning; //je spustena simulacia?

	private IntegerProperty addressBus;
	private IntegerProperty dataBus;
	private IntegerProperty controlBus;

	private Random rand;

	private Bus(){
		this.addressBus = new SimpleIntegerProperty(0);
		this.dataBus = new SimpleIntegerProperty(0);
		this.controlBus = new SimpleIntegerProperty(0);
        this.dataSemaphore = new Semaphore(0);

		rand = new Random();

        initControlBus();
	}

    /**
     * Prístup k inštancií singletonu. (synchronizované)
     * 
     * @return Inśtancia zbernice.
     */
	synchronized public static Bus getBus(){
		if(instance == null){
			instance = new Bus();
		}

		return instance;
	}

    /**
     * Informácia, či je zbernica pripojená k USB.
     * 
     * @return True ak je pripojená cez USB, false inak.
     */

    public IntegerProperty addressBusProperty(){
        return addressBus;
    }

    public IntegerProperty dataBusProperty(){
        return dataBus;
    }

    public IntegerProperty controlBusProperty(){
        return controlBus;
    }

    /**
     * Resetovanie stavu riadiacej zbernice, nastavenie náhodnej adresy a dát.
     */
	synchronized public void reset(){
		setRandomAddress();
		setRandomData();
		initControlBus();
	}

    /**
     * Vrátenie hodnoty na adresnej zbernici.
     * 
     * @return hodnota na adresnej zbernici.
     */
	synchronized public short getAddressBus() {
        return addressBus.getValue().shortValue();
	}

    /**
     * Vrátenie hodnoty na dátovej zbernici.
     * 
     * @return Hodnota na dátovej zbernici.
     */
    synchronized public byte getDataBus() {
        return dataBus.getValue().byteValue();
    }

    /**
     * Vrátenie hodnoty na riadiacej zbernici.
     * 
     * @return Hodnota na riadiacej zbernici.
     */
    public int getControlBus() {
        return controlBus.getValue();
    }

    /**
     * Nastavenie hodnoty na adresnú zbernicu.
     * 
     * @param addressBus Nová hodnota adresnej zbernice.
     */
    synchronized public void setAddressBus(short addressBus) {
        this.addressBus.setValue(Short.toUnsignedInt(addressBus));
    }

    /**
     * Nastavenie hodnoty na dátovú zbernicu.
     * 
     * @param dataBus Nová hodnota dátovej zbernice.
     */
    public void setDataBus(byte dataBus) {
        this.dataBus.setValue(Byte.toUnsignedInt(dataBus));
	}

    /**
     * Nastavenie náhodnej adresnej zbernice.
     */
	synchronized public void setRandomAddress(){
        this.addressBus.setValue(rand.nextInt(65535));
	}

    /**
     * Nastaveni náhodnej dátovej zbernice.
     */
	synchronized public void setRandomData(){
        this.dataBus.setValue(rand.nextInt(256));
	}

    /**
     * Pasívne čakanie na ustálenie simulácie,
     * maximálne však 5 sekund v prípade, ak je pripojený k simulátoru a simulácia beží. Ak nebeží, nečaká.
     *
     * @return True ak prišiel v časovom úseku oznam o nastavení dát, false inak.
     */
    public boolean waitForSteadyState() {
        if (Thread.currentThread().getName().equalsIgnoreCase("SimulationThread")) return true;

        if (isSimulationRunning) {
            while (true) {
                try {
                    //TODO korekcia ak semafor vrati false, aj metoda ma vratit false
                    if (!(!dataSemaphore.tryAcquire(5, TimeUnit.SECONDS) || queue.size() != 0)) break;
                } catch (InterruptedException e) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }

    }

    private LinkedBlockingQueue<?> queue;

    public void setEventsQueue(LinkedBlockingQueue<?> queue) {
        this.queue = queue;
    }

    /**
     * Oznámevnie zbernici, že dáta boli ustálené a je možné ich čítať.
     */
    public void dataInSteadyState() {
        dataSemaphore.release(20);
    }

    /**
     * Oznámevnie zbernici, že prebiehajú zmeny, ktoré môžu ovplyvniť dáta na dátovej zbernici a nie je teda bezpečné
     * z nej čítať.
     */
    public void dataIsChanging() {
        dataSemaphore.drainPermits();
    }

    /**
     * Oznámenie simulácie o zmene stavu. Či je spustená a má CPU čakať na nastavenie dát alebo nie je spustená.
     *
     * @param isRunning Stav simulácie, true ak je spustená, false inak.
     */
    public void simulationIsRunning(boolean isRunning) {
        this.isSimulationRunning = isRunning;
    }

    /**
     * Nastavenie negovaného signálu MW - memory write.
     * 
     * @param MW_ Nová hodnota signálu MW.
     */
    synchronized public void setMW_(boolean MW_) {
        mapToControlBus("MW_", MW_);
    }

    /**
     * Nastavenie negovaného signálu MR - memory read.
     * 
     * @param MR_ Nová hodnota signálu MR.
     */
    synchronized public void setMR_(boolean MR_) {
        //this.dataBus.setValue(0); //kvazi odpojenie od zbernice
        mapToControlBus("MR_", MR_);
    }
    
    /**
     * Nastavenie negovaného signálu IW - input write.
     *
     * @param IW_ Nová hodnota signálu IW.
     */
    synchronized public void setIW_(boolean IW_) {
        mapToControlBus("IW_", IW_);
    }

    /**
     * Nastavenie negovaného signálu IR - input read.
     *
     * @param IR_ Nová hodnota signálu IR.
     */
    synchronized public void setIR_(boolean IR_) {
        //this.dataBus.setValue(0); //kvazi odpojenie od zbernice
        mapToControlBus("IR_", IR_);
    }

    /**
     * Nastavenie negovaného signálu IA - interruption acknowledge.
     *
     * @param IA_ Nová hodnota signálu IA.
     */
    synchronized public void setIA_(boolean IA_) {
         mapToControlBus("IA_", IA_);
    }

    /**
     * Nastavenie signálu IT - interruption.
     *
     * @param IT Nová hodnota signálu IT.
     */
    public void setIT(boolean IT) {
        mapToControlBus("IT", IT);
    }

    /**
     * Nastavenie signálu RY - ready.
     *
     * @param RY Nová hodnota signálu RY.
     */
    public void setRY(boolean RY) {
        mapToControlBus("RY", RY);
    }

    /**
     * Zistenie hodoty signálu IT - interruption.
     * 
     * @return Hodnota signálu IT.
     */
	synchronized public boolean isIT() {
        return mapFromControlBus("IT");
	}

    /**
     * Inicializácia riadiacej zbernice do východzích hodnôt.
     */
	private void initControlBus(){
        mapToControlBus("MW_", true);
        mapToControlBus("MR_", true);
        mapToControlBus("IW_", true);
        mapToControlBus("IR_", true);
        mapToControlBus("IA_", true);

        mapToControlBus("IT", false);
        mapToControlBus("RY", false);
        mapToControlBus("BQ", false);
        mapToControlBus("BA", false);
    }

    /**
     * Mapovanie hodnoty signálu podľa návzu signálu do riadiacej zbernice.
     * 
     * @param signal Názov signálu.
     * @param value Nová hodnota signálu.
     */
    private void mapToControlBus(String signal, boolean value) {
        int pos = mapSignal(signal);

		if(value)
			controlBus.setValue( controlBus.getValue() | ( 1<<pos ) );
		else
			controlBus.setValue( controlBus.getValue() & ~( 1<<pos ) );
	}

    /**
     * Mapovanie hodnoty signálu podľa návzu signálu z riadiacej zbernice.
     *
     * @param signal Názov signálu.
     * @return Aktuálna hodnota signálu na zbernici.
     */
    synchronized private boolean mapFromControlBus(String signal){
        int pos = mapSignal(signal);

        return (controlBus.getValue() & 1<<pos ) != 0;
    }

    /**
     * Prevodník medzi názvom signálu a jemu odpovedajúcim bitom na riadiacej zbernici.
     *
     * @param signal Názov signálu.
     * @return Odpovedajúci bit na riadiacej zbernici.
     */
    private int mapSignal(String signal){
        switch (signal){
            case "MW_": return 8;
            case "MR_": return 7;
            case "IW_": return 6;
            case "IR_": return 5;
            case "IA_": return 4;
            case "IT": return 3;
            case "RY": return 2;
            case "BQ": return 1;
            case "BA": return 0;
        }

        return -1;
    }

    //
//    synchronized public boolean isMW_() {
//		return mapFromControlBus("MW_");
//	}
//
//	synchronized public boolean isMR_() {
//		return mapFromControlBus("MR_");
//	}
//
//	synchronized public boolean isIW_() {
//		return mapFromControlBus("IW_");
//	}
//
//	synchronized public boolean isIR_() {
//		return mapFromControlBus("IR");
//	}
//
    public boolean isIA_() {
        return mapFromControlBus("IA_");
    }
//    
//    /**
//     * Zistenie hodoty signálu RY - ready
//     *
//     * @return Hodnota signálu RY.
//     */
//    synchronized public boolean isRY() {
//            return mapFromControlBus("RY");
//    }


}//end Bus