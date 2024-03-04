package ir.ac.ut.fml2;

import net.automatalib.words.Word;

public class TransitionLabel {
	private String input;
	private Word<String> output;

	public TransitionLabel(String i_1, Word<String> o_1) {
		// TODO Auto-generated constructor stub
		this.input = i_1;
		this.output = o_1;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public Word<String> getOutput() {
		return output;
	}

	public void setOutput(Word<String> output) {
		this.output = output;
	}

	public boolean equals(TransitionLabel t) {
		if (this.input.equals(t.getInput()) && this.output.equals(t.getOutput())) {
			return true;
		} else
			return false;
	}

	public void printTransition() {
		System.out.println("input:" + this.getInput() + ", output:" + this.getOutput());
	}
}
