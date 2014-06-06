package de.teundem.synth;

import static de.teundem.synth.Utils.*;

public class V2LFO implements Constants {
	/*public static enum Mode {
		SAW,
		TRI,
		PULSE,
		SIN,
		S_H
	};*/

	private final int SAW = 0;
	private final int TRI = 1;
	private final int PULSE = 2;
	private final int SIN = 3;
	private final int S_H = 4;

	float out;
	int mode;
	boolean sync;
	boolean eg;
	int freq;
	long cntr;
	long cphase;
	float gain;
	float dc;
	long nseed;
	long last;

	V2Instance inst;

	public void init(V2Instance instance) {
		cntr = last = 0;
		nseed = (long) 0x75B89C44; //Math.random() & 0xffffffffl;
	}

	void set(syVLFO para) {
		mode = (int) para.mode;
		sync = (int) para.sync != 0;
		eg = (int) para.egmode != 0;
		freq = (int) (0.5f * fc32bit * Utils.calcfreq(para.rate / 128.0f));
		cphase = ftou32(para.phase / 128.0f) & 0xffffffffl;

		switch ((int) para.pol) {
			case 0:
				gain = para.amp;
				dc = 0.0f;
				break;

			case 1:
				gain = -para.amp;
				dc = 0.0f;
				break;

			case 2:
				gain = para.amp;
				dc = -0.5f * para.amp;
				break;
		}
	}

	void keyOn() {
		if (sync) {
			cntr = cphase; //& 0xffffffffl;
			last = (~0) & 0xffffffffl;
		}
	}

	void tick() {
		float v = 0;
		long x;

		switch (mode & 7) {
			case SAW:
			default:
				v = Utils.utof23(cntr);
				break;

			case TRI:
				x = (cntr << 1) ^ (cntr >>> 31);
				v = Utils.utof23(x &0xffffffffl);
				//System.out.printf("%f\n", v);
				break;

			case PULSE:
				x = (cntr >>> 31);
				v = Utils.utof23(x & 0xffffffffl);
				break;

			case SIN:
				v = Utils.utof23(cntr);
				v = Utils.fastsinrc(v * fc2pi) * 0.5f + 0.5f;
				break;

			case S_H:
				if (cntr < last) {
					nseed = Utils.urandom(nseed);
				}
				last = cntr;
				v = Utils.utof23(nseed);
				break;
		}

		out = v * gain + dc;
		cntr = (cntr + freq) & 0xffffffffl;
		if (cntr < (long) freq && eg) {
			cntr = (~0) & 0xffffffffl;
		}
		
		assert cntr >= 0 && cntr <= 0xffffffffl : "V2LFO-Counter out auf bounds " + cntr;
		assert cphase >= 0 && cphase <= 0xffffffffl : "V2LFO-CPhase out of bounds " + cphase;
		assert nseed >= 0 && nseed <= 0xffffffffl : "V2LFO-nseed out of bounds " + nseed;
		assert last >= 0 && last <= 0xffffffffl : "V2LFO-last out of bounds " + last;
		
	}
}
