package sk.uniza.fri.cp.CPUEmul;

/**
 * Trieda pre inštrukciu programu spolu s parametrami.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
class Instruction {

	private String firstParameter;
	private String secondParameter;
	private enumInstructionsSet type;

    /**
     * Konštruktor pre inštrukciu bez parametrov.
     *
     * @param typeOfInstruction Typ inštrukcie
     */
	Instruction(enumInstructionsSet typeOfInstruction){
		this.type = typeOfInstruction;
		this.firstParameter = null;
		this.secondParameter = null;
	}

    /**
     * Konštruktor pre inštrukciu s jedným parametrom.
     *
     * @param typeOfInstruction Typ inštrukcie
     * @param firstParameter Prvý parameter inštrukcie
     */
	Instruction(enumInstructionsSet typeOfInstruction, String firstParameter){
		this.type = typeOfInstruction;
		this.firstParameter = firstParameter;
		this.secondParameter = null;
	}

    /**
     * Konštruktor pre inštrukciu s dvoma parametrami.
     *
     * @param typeOfInstruction Typ inštrukcie
     * @param firstParameter Prvý parameter inštrukcie
     * @param secondParameter Druhý parameter inštrukcie
     */
	Instruction(enumInstructionsSet typeOfInstruction, String firstParameter, String secondParameter){
		this.type = typeOfInstruction;
		this.firstParameter = firstParameter;
		this.secondParameter = secondParameter;
	}

    /**
     * Prvý parameter inštrukcie, ak ho inštrukcia má.
     *
     * @return Prvý parameter inštrukcie.
     */
	String getFirstParameter() {
		return firstParameter;
	}

    /**
     * Nastavenie prvého parametra inštrukcie, ak ho inštrukcia má.
     *
     * @param firstParameter Nový prvý parameter inštrukcie.
     */
	void setFirstParameter(String firstParameter) {
		this.firstParameter = firstParameter;
	}

    /**
     * Druhý parameter inštrukcie, ak ho inštrukcia má.
     *
     * @return Druhý parameter inštrukcie.
     */
	String getSecondParameter() {
		return secondParameter;
	}

    /**
     * Nastavenie druhého parametra inštrukcie, ak ho inštrukcia má.
     *
     * @param secondParameter Nový druhý parameter inštrukcie.
     */
	void setSecondParameter(String secondParameter) {
		this.secondParameter = secondParameter;
	}

    /**
     * Typ inštrukcie z inštrukčnej sady.
     *
     * @return Typ inštrukcie
     */
	enumInstructionsSet getType(){
		return type;
	}
}