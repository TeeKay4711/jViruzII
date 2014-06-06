package de.teundem.synth;

public class V2Dist {
	/*public static enum Mode {
		OFF,
		OVERDRIVE,
		CLIP,
		BITCRUSHER,
		DECIMATOR
	};*/

	private final int OFF = 0;
	private final int OVERDRIVE = 1;
	private final int CLIP = 2;
	private final int BITCRUSHER = 3;
	private final int DECIMATOR = 4;

	private final int FLT_BASE = DECIMATOR;
	private final int FLT_LOW = FLT_BASE + V2Flt.LOW;
	private final int FLT_BAND = FLT_BASE + V2Flt.BAND;
	private final int FLT_HIGH = FLT_BASE + V2Flt.HIGH;
	private final int FLT_NOTCH = FLT_BASE + V2Flt.NOTCH;
	private final int FLT_ALL = FLT_BASE + V2Flt.ALL;
	private final int FLT_MOOGL = FLT_BASE + V2Flt.MOOGL;
	private final int FLT_MOOGH = FLT_BASE + V2Flt.MOOGH;

	int mode;
	float gain1;
	float gain2;
	float offs;
	float crush1;
	int crush2;
	int crxor;
	long dcount;
	long dfreq;
	float dvall;
	float dvalr;
	V2Flt fltl = new V2Flt();
	V2Flt fltr = new V2Flt();

	//static int d_cntr = 0;
	
	public void init(V2Instance instance) {
		dcount = 0;
		dvall = dvalr = 0;
		fltl.init(instance);
		fltr.init(instance);
	}

	void set(syVDist para) {
		float x;

		mode = (int) para.mode;
		gain1 = (float) Math.pow(2.0f, (para.ingain - 32.0f) / 16.0f);
		
		//System.out.printf("%d mode %d gain1 %f\n", d_cntr++, mode, gain1);
		switch (mode) {
			case OFF:
				break;
			case OVERDRIVE:
				gain2 = (para.param1 / 128.0f) / ((float) Math.atan(gain1));
				offs = gain1 * 2.0f * ((para.param2 / 128.0f) - 0.5f);
				break;
			case CLIP:
				gain2 = para.param1 / 128.0f;
				offs = gain1 * 2.0f * ((para.param2 / 128.0f) - 0.5f);
				break;
			case BITCRUSHER:
				x = para.param1 * 256.0f + 1.0f;
				crush2 = (int) x;
				crush1 = gain1 * (32768.0f / x);
				crxor = ((int) para.param2) << 9;
				break;
			case DECIMATOR:
				dfreq = Utils.ftou32((long) Utils.calcfreq(para.param1 / 128.0f) & 0xffffffffl);
				break;
			default: {
				syVFlt setup = new syVFlt();
				setup.cutoff = para.param1;
				setup.reso = para.param2;
				setup.mode = (float)(mode - FLT_BASE);
				fltl.set(setup);
				fltr.set(setup);
			}
				break;
		}
	}

	void renderMono(float[] dest, float[] src, int nsamples) {

		switch (mode) {
			case OFF:
				//System.out.println("V2Dist.renderMono-mode 0 (OFF)");
				if (dest != src) {
					System.out.println("V2Dist.renderMono dest != src...");
					//memmove
					/*for(int i = 0; i < nsamples; i++) {
						dest[i] = src[i];
					}*/
				}
				break;

			case OVERDRIVE:
				for (int i = 0; i < nsamples; i++) {
					dest[i] = overdrive(src[i]);
				}
				break;

			case CLIP:
				for (int i = 0; i < nsamples; i++) {
					dest[i] = clip(src[i]);
				}
				break;

			case BITCRUSHER:
				for (int i = 0; i < nsamples; i++) {
					dest[i] = bitcrusher(src[i]);
				}
				break;

			case DECIMATOR:
				for (int i = 0; i < nsamples; i++) {
					decimator_tick(src[i], 0.0f);
					dest[i] = dvall;
				}
				break;

			default:
				fltl.render(dest, src, nsamples);
				break;
		}
	}

	//void renderStereo(StereoSample[] dest, StereoSample[] src, int nsamples) {
	void renderStereo(float[][] dest, float[][] src, int nsamples) {
		
		float[] tleft = new float[nsamples * 2];
		float[] tright = new float[nsamples * 2];
		
		for (int i = 0; i < nsamples; i++) {
			tleft[i] = src[i][0];
			tright[i] = src[i][1];
		}
		
		switch (mode) {
			case DECIMATOR:
				for (int i = 0; i < nsamples; i++) {
					decimator_tick(src[i][0], src[i][1]);
					dest[i][0] = dvall;
					dest[i][1] = dvalr;
				}
				break;

			case FLT_LOW:
			case FLT_BAND:
			case FLT_HIGH:
			case FLT_NOTCH:
			case FLT_ALL:
				//TODO: Fix this fuckin crap in V2Dist.render()
				//fltl.render(dest[0].l, src[0].l, nsamples, 2);
				//fltr.render(dest[0].r, src[0].r, nsamples, 2);
				fltl.render(tleft, tleft, nsamples, 2);
				fltr.render(tright, tright, nsamples, 2);
				break;

			default:
				//renderMono(dest[0].l, dest[0].l, nsamples);
				
				//renderMono(tleft, tleft, nsamples * 2);
				renderMono(tleft, tleft, nsamples);
				renderMono(tright, tright, nsamples);
		}

		for (int i = 0; i < nsamples; i++) {
			dest[i][0] = tleft[i];
			dest[i][1] = tright[i];
		}

	}

	float overdrive(float in) {
		return gain2 * Utils.fastatan(in * gain1 + offs);
	}

	float clip(float in) {
		return gain2 * Utils.clamp(in * gain1 + offs, -1.0f, 1.0f);
	}

	float bitcrusher(float in) {
		int t = (int) (in * crush1);
		t = (int) Utils.clamp(t * crush2, -0x7fff, 0x7fff) ^ crxor;
		return (float) t / 32768.0f;
	}

	void decimator_tick(float l, float r) {
		dcount += dfreq;
		if (dcount < dfreq) {
			dvall = l;
			dvalr = r;
		}
	}
}
