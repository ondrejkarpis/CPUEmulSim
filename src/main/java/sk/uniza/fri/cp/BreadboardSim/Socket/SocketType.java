package sk.uniza.fri.cp.BreadboardSim.Socket;

/**
 * Vymenovanie možných typov soketov. Slúžia na určenie priority hodnoty potenciálu obvodu.
 *
 * @version 1.1
 * @created 17.3.2017 16:16:36
 */
public enum SocketType {
	OUT, //obyčaný výstup na najvyššiu prioritu pri určovaní hodnoty potenciálu
	WEAK_OUT,   //vystup cez rezistory, moze sa napojit priamo na VCC alebo GND, slabsi OUT
                //neexistuje k nemu pin, typ sa nastavuje priamo pri vytvarani NumKeys (mozne doplnenie pinu v buducnosti)
                //OCO, //vystup s otvorenym kolektorom, dokaze iba stiahnut hodnotu na LOW, nedokaze ju vyhnat na HIGH ak nie je
                TRI_OUT, //tri stavy - HIGH, LOW, HIGH IMPEDANCE
    IO, //soket, ktorý môže byť v jednom momente vstup alebo výstup
    IN, //vstup, nema na hodnotu potencialu vplyv
    NC //nepripojene, nema na hodnotu potencialu vplyv
}