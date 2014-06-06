package de.teundem.synth;

public class Synth_core {
	/*public int synthGetSize() {
		return sizeof(V2Synth);
	}*/

	public static V2Synth synth = new V2Synth();

	public static void synthInit(byte[] pthis, int patchmap, int samplerate) {
		//System.out.println("synthInit called");
		synth.init(patchmap, samplerate);
	}

	public static void synthRender(int bufferp, float[][] buf, long smp, float buf2, boolean add) {
		//System.out.println("synthRender called");
		synth.render(bufferp, buf, smp, buf2, add);
	}

	public static void synthProcessMIDI(byte[] pthis, short[] buffer) {
		//System.out.println("synthProcessMIDI called");
		synth.processMIDI(buffer);
	}

	public static void synthSetGlobals(int ptr_globalParams) {
		synth.setGlobals(ptr_globalParams);
	}

	public void synthGetPoly() {

	}

	public void synthGetPgm() {

	}

	public void synthSetVUMode() {

	}

	public void synthGetChannel() {

	}

	public void synthGetMainVU() {

	}

	public void synthGetFrameSize() {

	}
}
