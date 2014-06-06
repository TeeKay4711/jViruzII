package de.teundem.synth;

public class V2Boost implements Constants {
	boolean enabled;
	float a1, a2;
	float b0, b1, b2;
	float x1[] = new float[2];
	float x2[] = new float[2];
	float y1[] = new float[2];
	float y2[] = new float[2];

	V2Instance inst;

	public void init(V2Instance instance) {
		inst = instance;
	}

	public void set(syVBoost para) {
		enabled = ((int) para.amount) != 0;
		if (!enabled) {
			return;
		}

		float A = (float) Math.pow(2.0, para.amount / 128.0f);

		float beta = (float) Math.sqrt(2.0f * A);

		float bs = beta * inst.SRfcBoostSin;
		float Am1 = A - 1.0f;
		float Ap1 = A + 1.0f;
		float cAm1 = Am1 * inst.SRfcBoostCos;
		float cAp1 = Ap1 * inst.SRfcBoostCos;

		float ia0 = 1.0f / (Ap1 + cAm1 + bs);

		b1 = 2.0f * A * (Am1 - cAp1) * ia0;
		a1 = -2.0f * (Am1 + cAp1) * ia0;
		a2 = (Ap1 + cAm1 - bs) * ia0;
		b0 = A * (Ap1 - cAm1 + bs) * ia0;
		b2 = A * (Ap1 - cAm1 - bs) * ia0;
	}

	//void render(StereoSample[] buf, int nsamples) {
	void render(float[][] buf, int nsamples) {
		if (!enabled) {
			return;
		}

		for (int ch = 0; ch < 2; ch++) {
			float xm1 = x1[ch], xm2 = x2[ch];
			float ym1 = y1[ch], ym2 = y2[ch];

			for (int i = 0; i < nsamples; i++) {
				float x = buf[i][ch] + fcdcoffset;

				float y = b0 * x + b1 * xm1 + b2 * xm2 - a1 * ym1 - a2 * ym2;
				ym2 = ym1;
				ym1 = y;
				xm2 = xm1;
				xm1 = x;

				buf[i][ch] = y;
			}

			x1[ch] = xm1;
			x2[ch] = xm2;
			y1[ch] = ym1;
			y2[ch] = ym2;
		}
	}
}
