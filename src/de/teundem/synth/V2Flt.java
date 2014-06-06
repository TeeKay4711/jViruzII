package de.teundem.synth;

public class V2Flt {
	/*public static enum Mode {
		BYPASS,
		LOW,
		BAND,
		HIGH,
		NOTCH,
		ALL,
		MOOGL,
		MOOGH
	};*/

	static final int BYPASS = 0;
	static final int LOW = 1;
	static final int BAND = 2;
	static final int HIGH = 3;
	static final int NOTCH = 4;
	static final int ALL = 5;
	static final int MOOGL = 6;
	static final int MOOGH = 7;

	float mode;
	float cfreq;
	float res;
	float moogf, moogp, moogq;
	V2LRC lrc = new V2LRC();
	V2Moog moog = new V2Moog();

	V2Instance inst;

	public void init(V2Instance instance) {
		lrc.init();
		moog.init();
		inst = instance;
	}

	void set(syVFlt para) {
		mode = (int) para.mode;
		float f = Utils.calcfreq(para.cutoff / 128.f) * inst.SRfclinfreq;
		float r = para.reso / 128.0f;

		if (mode < MOOGL) {
			res = 1.0f - r;
			cfreq = f;
		} else {
			f *= 0.25f;
			float t = 1.0f - f;
			moogp = f + 0.8f * f * t;
			moogf = 1.0f - moogp - moogp;
			moogq = 4.0f * r * (1.0f + 0.5f * t * (1.0f - t + 5.6f * t * t));
		}
	}

	void render(float[] dest, float[] src, int nsamples) {
		render(dest, src, nsamples, 1);
	}

	void render(float[] dest, float[] src, int nsamples, int step) {
		V2LRC flt;// = new V2LRC();
		V2Moog m;// = new V2Moog(); 

		step = 1;
		
		switch ((int) mode & 7) {
			case BYPASS:
				if (dest != src) {
					// memmove
					//System.out.println("V2Flit.render() dest != src");
					System.arraycopy(src, 0, dest, 0, nsamples);
				}
				break;

			case LOW:
				flt = lrc;
				for (int i = 0; i < nsamples; i++) {
					flt.step_2x(src[i * step], cfreq, res);
					dest[i * step] = flt.l;
				}
				lrc = flt;
				break;

			case BAND:
				flt = lrc;
				for (int i = 0; i < nsamples; i++) {
					flt.step_2x(src[i * step], cfreq, res);
					dest[i * step] = flt.b;
				}
				lrc = flt;
				break;

			case HIGH:
				flt = lrc;
				for (int i = 0; i < nsamples; i++) {
					float h = flt.step_2x(src[i * step], cfreq, res);
					dest[i * step] = h;
				}
				lrc = flt;
				break;

			case NOTCH:
				flt = lrc;
				for (int i = 0; i < nsamples; i++) {
					float h = flt.step_2x(src[i * step], cfreq, res);
					dest[i * step] = flt.l + h;
				}
				lrc = flt;
				break;

			case ALL:
				flt = lrc;
				for (int i = 0; i < nsamples; i++) {
					float h = flt.step_2x(src[i * step], cfreq, res);
					dest[i * step] = flt.l + flt.b + h;
				}
				lrc = flt;
				break;

			case MOOGL:
				m = moog;
				for (int i = 0; i < nsamples; i++) {
					float in = src[i * step];
					m.step(in, moogf, moogp, moogq);
					dest[i * step] = m.step(in, moogf, moogp, moogq);
				}
				moog = m;
				break;

			case MOOGH:
				m = moog;
				for (int i = 0; i < nsamples; i++) {
					float in = src[i * step];
					m.step(in, moogf, moogp, moogq);
					dest[i * step] = in - m.step(in, moogf, moogp, moogq);
				}
				moog = m;
		}
	}
}
