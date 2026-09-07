package sk.uniza.fri.cp.BreadboardSim.Socket;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Potenciál medzi dvoma soketmi.
 * Prepájanie soketov potenciálom vytvára strom spojení.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017 16:16:35
 */
public class Potential {

	public enum Value{ HIGH, LOW, NC }

	private Socket socket1;
	private Socket socket2;
	private Potential parent1;
	private Potential parent2;
	private Potential child;
	volatile private Value value;
	private SocketType type;

	//skrat
    private boolean shortCircuit; //nastal skrat?
    private final LinkedList<Socket> shortedSockets;

	/**
     *  Vytvorenie potenciálu medzi dvoma soketmi.
     *
     * @param socket1 Prvý soket.
     * @param socket2 Druhý soket.
     */
	public Potential(Socket socket1, Socket socket2){
        this.shortedSockets = new LinkedList<>();

		this.socket1 = socket1;
		this.socket2 = socket2;
		this.update();
	}

    /**
     * Ak má potenciál potomka, vráti odkaz na neho, inak vráti seba.
     *
     * @return Výsledný potenciál v strome.
     */
    // POZOR, tu obcas vznikne chyba StackOverflowError - asi nekonecna rekurzia
    public synchronized Potential getPotential(){
		if(child != null){
			return child.getPotential();
		}

		return this;
	}

    /**
     * Naplní zoznam soketmi, ktoré sú v danom strome spojení prepojené.
     *
     * @param listToFill Zoznam, do ktorého sa pridávajú sokety.
     */
    void getConnectedSockets(List<Socket> listToFill) {
        if (this.parent1 == null && this.parent2 == null) {
            if (this.socket1 != null) listToFill.add(this.socket1);
            if (this.socket2 != null) listToFill.add(this.socket2);
        } else {
            if (this.parent1 != null) this.parent1.getConnectedSockets(listToFill);
            if (this.parent2 != null) this.parent2.getConnectedSockets(listToFill);
        }
    }

    /**
     * Naplní množinu zariadeniami, ktoré majú k stromu sopjení pripojené vstupy.
     *
     * @param setToFill Množina, do ktorej sa pridávajú zariadenia.
     */
    public void getDevicesWithInputs(Set<Device> setToFill) {
        if (this.parent1 == null && this.parent2 == null) {
			if (this.socket1 != null && (this.type == SocketType.IN || this.type == SocketType.IO)) {
                setToFill.add(this.socket1.getDevice());
            }

			if (this.socket2 != null &&  (this.type == SocketType.IN || this.type == SocketType.IO)) {
                setToFill.add(this.socket2.getDevice());
            }
		} else {
			if (this.parent1 != null) {
                this.parent1.getDevicesWithInputs(setToFill);
            }

			if (this.parent2 != null) {
                this.parent2.getDevicesWithInputs(setToFill);
            }
		}
	}

    /**
     * Aktualizácia potenciálu
     */
    public synchronized void update(){
        if (this.parent1 != null) {
            this.parent1.child = null;
        }
        if (this.parent2 != null) {
            this.parent2.child = null;
        }

        if (this.socket1 != null) {
            this.parent1 = this.socket1.getPotential();
        }
        if (this.socket2 != null) {
            this.parent2 = this.socket2.getPotential();
        }

        if (this.parent1 == this) {
            this.parent1 = null;
        }
        if (this.parent2 == this) {
            this.parent2 = null;
        }

        if (this.parent1 != null) {
            this.parent1.child = this;
        }
        if (this.parent2 != null) {
            this.parent2.child = this;
        }

        //odpojenie potomka pred aktualizaciou typu a hodnoty (aj na nom by sa volali)
        Potential oldChild = this.child;
        this.child = null;

        this.updateType();

        this.setValue(Value.NC);

        if (oldChild != null)
            oldChild.update();
    }

    /**
     * Hodnota potenciálu.
     *
     * @return Hodnota tohoto potenciálu.
     */
    public synchronized Value getValue(){
        return value;
    }

	/**
	 * Nastavenie novej hodnoty potencálu.
	 * Pri zmene hodnoty potenciálu sa berie do úvahy jeho typ, typ a hodnota predkov, ak nejakých má.
     * Kontroluje sa aj skrat pri spojeni dvoch rozdielnych výstupov.
     *
     * @param newVal Nová požadovaná hodnota potenciálu
     * @return True - hodnota sa spravne aktualizovala / False - nastal skrat
	 */
    public boolean setValue(Value newVal) {
        //zistenie skratu na predkoch
	    this.shortCircuit =
                (this.parent1 != null && this.parent1.shortCircuit)
                || (this.parent2 != null && this.parent2.shortCircuit);

	    //ak je jeden z predkov zoskratovany
	    if( this.shortCircuit ){
            //nastav nahodnu hodnotu
            if(Math.random() < 0.5)
                this.value = Value.LOW;
            else
                this.value = Value.HIGH;
        }
        else if (this.parent1 != null && this.parent2 != null ){
			//ak ma potencial oboch predkov a nie je zoskratovany

			if (this.type == SocketType.NC || this.type == SocketType.IN){
				//potencial je typu vstup alebo nepripojeny -> neovplyvnuje hodnotu vysledneho potencialu
				this.value = Value.NC;
			}
			else if (this.parent1.getValue() == Value.NC && this.parent2.getValue() == Value.NC){
				//ani jeden z rodicov nie je napojeny -> neprebera ziadnu hodnotu
				this.value = Value.NC;
			}
            else if (this.type == SocketType.IO || this.type == SocketType.TRI_OUT) {
                //ak je potencial typu Input-Output -> aspon jeden z predkov je IO, mozno obaja
                if ((this.parent1.type == SocketType.IO || this.parent1.type == SocketType.TRI_OUT) &&
                        (this.parent2.type == SocketType.IO || this.parent2.type == SocketType.TRI_OUT)) {
                    //ak su obaja predkovia typu IO, moze dojst ku skratu ak obaja vysielaju rozne hodnoty
                    this.checkShortCircuit();
                } else {
                    //ak je iba jeden typu IO, druhy je IN alebo NC a tie nevplyvaju na hodnotu potencialu
                    if (this.parent1.type == SocketType.IO || this.parent1.type == SocketType.TRI_OUT)
                        this.value = this.parent1.value;
                    else
                        this.value = this.parent2.value;
                }
			}
			else if (this.type == SocketType.WEAK_OUT ){
			    //ak je potencial typu WEAK_OUT -> aspon jeden je tiez weak out
                if (this.parent1.type == SocketType.WEAK_OUT && this.parent2.type == SocketType.WEAK_OUT) {
                    //ak su oba predkovia typu WEAK_OUT, jeden to moze stiahnut na LOW, inak je HIGH
                    //GND ma mensi odpor
                    if(this.parent1.value == Value.LOW || this.parent2.value == Value.LOW){
                        this.value = Value.LOW;
                    } else {
                        this.value = Value.HIGH;
                    }
                }
                else if(this.parent1.type == SocketType.WEAK_OUT){
                    //ak je iba prvy typu WEAK OUT, berie si jeho hodnotu
                    //this.value = this.parent1.value;      // povodny kod
                    // upraveny kod - riesi spojenie vystupu z klavesnice a DB
                    // ked DB vysiela, musi vyhrat (klavesnica je WEAK_OUT)
                    if (((this.parent2.type == SocketType.IO || this.parent2.type == SocketType.TRI_OUT) && this.parent2.getValue() != Value.NC) ||
                            this.parent2.type == SocketType.OUT )
                        this.value = this.parent2.value;
                    else
                        this.value = this.parent1.value;
                }
                else {
                    //ak je iba druhy typu WEAK OUT, berie si jeho hodnotu
                    // this.value = this.parent2.value;         // povodny kod
                    // upraveny kod - riesi spojenie vystupu z klavesnice a DB
                    // ked DB vysiela, musi vyhrat (klavesnica je WEAK_OUT)
                    if (((this.parent1.type == SocketType.IO || this.parent1.type == SocketType.TRI_OUT) && this.parent1.getValue() != Value.NC) ||
                            this.parent1.type == SocketType.OUT)
                        this.value = this.parent1.value;
                    else
                        this.value = this.parent2.value;
                }
            }
			else if(this.type == SocketType.OUT){
				//ak je potencial typu OUT -> aspon jeden z predkov je tiez OUT
                //ulozenie typu toho druheho potencialu
                SocketType theOther = this.parent1.type == SocketType.OUT ? this.parent2.type : this.parent1.type;

                if (theOther == SocketType.OUT || theOther == SocketType.IO || theOther == SocketType.TRI_OUT) {
                    //ak su obaja predkovia vystupy, moze dojst ku skratu
                    this.checkShortCircuit();
                } else if (this.parent1.type == SocketType.OUT) {
                    //ak je iba prvy typu OUT, berie si jeho hodnotu
                    this.value = this.parent1.value;
                } else {
                    //ak je iba druhy typu OUT, berie si jeho hodnotu
                    this.value = this.parent2.value;
                }
            }
		} else if(this.parent1 != null){
			this.value = this.parent1.getValue();
		} else if(this.parent2 != null){
			this.value = this.parent2.getValue();
		} else {
			this.value = newVal;
		}

        this.unhighlightShortCircuitSockets();

        //ak je potencial na vrchu stromu a nastal niekde skrat, zobraz to na vystupnych soketoch
		if(this.shortCircuit && this.child == null)
            this.highlightShortCircuitSockets();


		//ak ma potencial potomka, aktualizuj jeho hodnotu a vrat oznamenie o moznom skrate (false ak nastal)
		if(this.child != null)
			return this.child.setValue(this.value);

		//ak potencial nema potomka, vrat hodnotu false ak doslo ku skratu lebo sa nenastavila pozadovana hodnota
		return !this.shortCircuit;
	}

    /**
     * Vráti typ potenciálu.
     *
     * @return Typ potenciálu.
     */
    public synchronized SocketType getType() {
        return this.type;
    }

    /**
     * Nastaví typ potenciálu a aktualizuje strom.
     *
     * @param newType Nový typ potenciálu.
     */
    public synchronized void setType(SocketType newType){
        if (this.parent1 == null && this.parent2 == null) {
            this.type = newType;

            if (this.child != null)
                this.child.updateType();
        } else {
            this.updateType();
        }
    }

    /**
     * Vráti potomka potenciálu.
     *
     * @return Potomok potenciálu.
     */
    public Potential getChild(){
        return this.child;
    }

    /**
     * Zrušenie potenciálu.
     * Pri rušení potenciálu sa odpojí od svojich predkov a aktualizuje potomkov.
     */
    public void delete(){
        this.parent1.child = null;
        this.parent2.child = null;

        //this.shortCircuit = false;
        if(this.shortedSockets.size() > 0)
            this.shortedSockets.forEach(socket -> socket.unhighlight(Socket.WARNING));

        if (this.child != null) {
            //ak ma potomka, aktualizuj ho
            this.child.update();
        } else if(this.shortCircuit) {
            //ak nema potomka a nastal skrat, aktualizuj predkov na zvyraznenie skratu
            this.parent1.highlightShortCircuitSockets();
            this.parent2.highlightShortCircuitSockets();
        }
    }

    /**
     * Aktualizácia typu potenciálu podľa predkov. Po úprave typu sa aktualizuje aj jeho potomok.
     */
    private synchronized void updateType(){
		SocketType type1 = SocketType.NC;
		SocketType type2 = SocketType.NC;

		//ak potencial ma aspon jedneho predka
		if(this.parent1 != null || this.parent2 != null){
			if(this.parent1 != null)
				type1 = this.parent1.getType();

			if(this.parent2 != null)
				type2 = this.parent2.getType();

			if(type1 == SocketType.OUT || type2 == SocketType.OUT){ //ak je jeden z predkov vystup
				this.type = SocketType.OUT;
			} else if(type1 == SocketType.WEAK_OUT || type2 == SocketType.WEAK_OUT) { //ak je predok vystup s rezistorom
                this.type = SocketType.WEAK_OUT;
            } /*else if(type1 == SocketType.OCO || type2 == SocketType.OCO){ //ak je predok vystup s otvorenym kolektorom
                this.type = SocketType.OCO;
			}*/ else if (type1 == SocketType.TRI_OUT || type2 == SocketType.TRI_OUT) { //ak je predok vystup s tromi hodnotami
                this.type = SocketType.TRI_OUT;
            } else if(type1 == SocketType.IO|| type2 == SocketType.IO){ //ak je predok vstup/vystup
                this.type = SocketType.IO;
            } else if(type1 == SocketType.IN || type2 == SocketType.IN){ //ak je predok iba vstup
                this.type = SocketType.IN;
            } else {
                this.type = SocketType.NC;
            }

		} else { //ak nema ani jedneho predka, je nepripojeny
			this.type = SocketType.NC;
		}

		//aktualizuj typ potomka
		if(this.child != null)
			this.child.updateType();
	}

	/**
     * Zvýraznenie výstupných soketov na ktorých došlo ku skratu, ak k nemu došlo.
     */
    private void highlightShortCircuitSockets(){
        if(this.shortCircuit) {
            synchronized (this.shortedSockets) {
                getOutputSockets(this.shortedSockets);
                this.shortedSockets.forEach(socket -> socket.highlight(Socket.WARNING));
            }
        }
    }

    /**
     * Zrušenie zvýraznenie výstupných soketov, na ktorých bol zobrazený skrat, ak také existujú.
     */
    private void unhighlightShortCircuitSockets(){
        synchronized (this.shortedSockets) {
            if (this.shortedSockets.size() > 0) {
                try {
                    this.shortedSockets.forEach(socket -> socket.unhighlight(Socket.WARNING));
                    this.shortedSockets.clear();
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Kontrola skratu na vystupoch predkov. Ak nastal skrat, nastavi nahodnu hodnotu potencialu a aktualizuje
     * atribut shortCircuit. Predpokladá sa, že obaja predkovia sú výstupy!
     */
	private void checkShortCircuit(){
        //ak su obaja predkovia zapojeny
        if (this.parent1.value != Value.NC && this.parent2.value != Value.NC) {

            //ak obaja predkovia vysielaju rozne hodnoty potencialov (kontroluje sa pri vystupoch)
            if (this.parent1.value != this.parent2.value) {
                //bum, doslo ku skratu
                this.shortCircuit = true;
                //nastav nahodnu hodnotu potencialu
                if (Math.random() < 0.5) this.value = Value.LOW;
                else this.value = Value.HIGH;
                return;
            } else {
                //nedoslo ku skratu, na obochy vystupoch su rovnake hodnoty
                this.value = this.parent1.value;
            }
        } else if (this.parent1.value != Value.NC) {
            //ak je zapojeny iba prvy predok
            this.value = this.parent1.value;
        } else if (this.parent2.value != Value.NC) {
            //ak je zapojeny iba druhy predok
            this.value = this.parent2.value;
        }


        this.shortCircuit = false;
    }

    /**
     * Naplnenie listu predaneho parametrom soketmi, ktoré sa správajú ako výstup a môže na nich dôjsť ku skratu.
     *
     * @param listForSockets List pre naplnenie soketmi.
     */
    private void getOutputSockets(List<Socket> listForSockets){

        //ak potencial nema rodicov, je pripojeny priamo na soket
        if (this.parent1 == null && this.parent2 == null) {
            if (this.socket1 != null && (this.type == SocketType.OUT || this.type == SocketType.IO)) {
                listForSockets.add(this.socket1);
            }

            if (this.socket2 != null &&  (this.type == SocketType.OUT || this.type == SocketType.IO)) {
                listForSockets.add(this.socket2);
            }
        } else {
            if (this.parent1 != null) {
                this.parent1.getOutputSockets(listForSockets);
            }
            if (this.parent2 != null) {
                this.parent2.getOutputSockets(listForSockets);
            }
        }
    }

}