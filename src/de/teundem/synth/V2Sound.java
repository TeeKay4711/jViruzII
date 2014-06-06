package de.teundem.synth;

public class V2Sound {
	byte[] voice = new byte[59];
	byte[] chan = new byte[29];
	byte maxpoly;
	byte modnum;
	V2Mod[] modmatrix;// = new V2Mod[preInit];

	public V2Sound(byte modulatorNum) {
		modmatrix = new V2Mod[modulatorNum];
		for (int i = 0; i < modulatorNum; i++) {
			modmatrix[i] = new V2Mod();
		}
	}
}
