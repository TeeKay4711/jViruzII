package de.teundem.synth;

public class syVV2 {
	public static final int NOSC = 3;
	public static final int NFLT = 2;
	public static final int NENV = 2;
	public static final int NLFO = 2;

	float panning;
	float transp;

	syVOsc osc[] = new syVOsc[NOSC];
	syVFlt flt[] = new syVFlt[NFLT];
	float routing;
	float fltbal;
	syVDist dist = new syVDist();
	syVEnv env[] = new syVEnv[NENV];
	syVLFO lfo[] = new syVLFO[NLFO];
	float oscsync;

	syVV2() {
		for (int i = 0; i < NOSC; i++) {
			osc[i] = new syVOsc();
		}

		for (int i = 0; i < NFLT; i++) {
			flt[i] = new syVFlt();
		}

		for (int i = 0; i < NENV; i++) {
			env[i] = new syVEnv();
		}

		for (int i = 0; i < NLFO; i++) {
			lfo[i] = new syVLFO();
		}

	}
}
