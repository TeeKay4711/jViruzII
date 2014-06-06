package de.teundem.synth;

public class V2Reverb implements Constants {
	float gainc[] = new float[4];
	float gaina[] = new float[2];
	float gainin;
	float damp;
	float lowcut;

	V2Delay combd[][] = new V2Delay[2][4];
	float combl[][] = new float[2][4];
	V2Delay alld[][] = new V2Delay[2][2];
	float hpf[] = new float[2];

	V2Instance inst;

	float bcombl0[] = new float[1309];
	float bcombl1[] = new float[1635];
	float bcombl2[] = new float[1811];
	float bcombl3[] = new float[1926];
	float balll0[] = new float[220];
	float balll1[] = new float[74];
	float bcombr0[] = new float[1327];
	float bcombr1[] = new float[1631];
	float bcombr2[] = new float[1833];
	float bcombr3[] = new float[1901];
	float ballr0[] = new float[205];
	float ballr1[] = new float[77];

	public void init(V2Instance instance) {
		combd[0][0] = new V2Delay();
		combd[0][0].init(bcombl0);
		combd[0][1] = new V2Delay();
		combd[0][1].init(bcombl1);
		combd[0][2] = new V2Delay();
		combd[0][2].init(bcombl2);
		combd[0][3] = new V2Delay();
		combd[0][3].init(bcombl3);
		alld[0][0] = new V2Delay();
		alld[0][0].init(balll0);
		alld[0][1] = new V2Delay();
		alld[0][1].init(balll1);
		combd[1][0] = new V2Delay();
		combd[1][0].init(bcombr0);
		combd[1][1] = new V2Delay();
		combd[1][1].init(bcombr1);
		combd[1][2] = new V2Delay();
		combd[1][2].init(bcombr2);
		combd[1][3] = new V2Delay();
		combd[1][3].init(bcombr3);
		alld[1][0] = new V2Delay();
		alld[1][0].init(ballr0);
		alld[1][1] = new V2Delay();
		alld[1][1].init(ballr1);

		inst = instance;
		reset();
	}

	void reset() {
		for (int ch = 0; ch < 2; ch++) {
			for (int i = 0; i < 4; i++) {
				combd[ch][i].reset();
				combl[ch][i] = 0.0f;
			}
			for (int i = 0; i < 2; i++) {
				alld[ch][i].reset();
			}
			hpf[ch] = 0.0f;
		}
	}

	void set(syVReverb para) {
		final float[] gaincdef = new float[] { 0.966384599f, 0.958186359f, 0.953783929f, 0.950933178f };
		final float[] gainadef = new float[] { 0.994260075f, 0.998044717f };

		float e = inst.SRfclinfreq * Utils.sqr(64.0f / (para.revtime + 1.0f));

		for (int i = 0; i < 4; i++) {
			gainc[i] = (float) Math.pow(gaincdef[i], e);
		}

		for (int i = 0; i < 2; i++) {
			gaina[i] = (float) Math.pow(gainadef[i], e);
		}

		damp = inst.SRfclinfreq * (para.highcut / 128.0f);
		gainin = para.vol / 128.0f;
		lowcut = inst.SRfclinfreq * Utils.sqr(Utils.sqr(para.lowcut / 128.0f));
	}

	//void render(StereoSample[] dest, int nsamples) {
	void render(float[][] dest, int nsamples) {

		float[] inbuf = inst.aux1buf;

		for (int i = 0; i < nsamples; i++) {
			float in = inbuf[i] * gainin + fcdcoffset;

			for (int ch = 0; ch < 2; ch++) {
				float cur = 0.0f;
				for (int j = 0; j < 4; j++) {
					/*float dv = combd[ch][j].fetch();
					float nv = gainc[j] * dv + (((j & 1) != 0) ? -in : in);
					combl[ch][j] += damp * (nv - combl[ch][j]);
					combd[ch][j].feed(combl[ch][j]);
					cur += combl[ch][j];*/
			        float dv = gainc[j] * combd[ch][j].fetch();
			        float nv = ((j & 1) != 0) ? (dv - in) : (dv + in); // alternate phase on combs
			        float lp = combl[ch][j] + damp * (nv - combl[ch][j]);
			        combd[ch][j].feed(lp);
			        cur += lp;
				}

				for (int j = 0; j < 2; j++) {
					float dv = alld[ch][j].fetch();
					float dz = cur + gaina[j] * dv;
					alld[ch][j].feed(dz);
					cur = dv - gaina[j] * dz;
				}

				hpf[ch] += lowcut * (cur - hpf[ch]);
				//dest[i].ch[ch] += cur - hpf[ch];
				dest[i][ch] += cur - hpf[ch];
			}
		}
	}
}
