package de.teundem.synth;

public class V2ModDel implements Constants {
	float[][] db = new float[2][];
	long dbufmask;

	long dbptr;
	long[] dboffs = new long[2];

	long mcnt;
	int mfreq;
	long mphase;
	long mmaxoffs;

	float fbval;
	float dryout;
	float wetout;

	V2Instance inst;

	public void init(V2Instance instance, float[] buf1, float[] buf2, int buflen) {
		db[0] = buf1;
		db[1] = buf2;

		dbufmask = buflen - 1;
		inst = instance;

		reset();
	}

	void reset() {
		dbptr = 0;
		mcnt = 0;

		//memset(db[0], 0, (dbufmask + 1) * sizeof(float));
		//memset(db[1], 0, (dbufmask + 1) * sizeof(float));
	}

	void set(syVModDel para) {
		wetout = (para.amount - 64.0f) / 64.0f;
		//dryout = 1.0f - fabsf(wetout);
		dryout = 1.0f - Math.abs(wetout);
		fbval = (para.fb - 64.0f) / 64.0f;

		float lenscale = ((float) dbufmask - 1023.0f) / 128.0f;
		dboffs[0] = (int) (para.llength * lenscale);
		dboffs[1] = (int) (para.rlength * lenscale);

		mfreq = (int)(inst.SRfclinfreq * fcmdlfomul * Utils.calcfreq(para.mrate / 128.0f));
		mmaxoffs = (int)(para.mdepth * 1023.0f / 128.0f);
		mphase = Utils.ftou32((float)((para.mphase - 64.0f) / 128.0f)) & 0xffffffffl;
	}

	//void renderAux2Main(StereoSample[] dest, int nsamples) {
	void renderAux2Main(float[][] dest, int nsamples) {

		if (wetout == 0) {
			return;
		}

		//float[] x = new float[nsamples];
		float[][] x = new float[nsamples][2];
		for (int i = 0; i < nsamples; i++) {

			float in = inst.aux2buf[i] + fcdcoffset;
			processSample(x[i], in, in, 0.0f);

			//dest[i][0] += x[i];
			//dest[i][1] += x[i];
			dest[i][0] += x[i][0];
			dest[i][1] += x[i][1];
		}
	}

	//void renderChan(StereoSample[] chanbuf, int nsamples) {
	void renderChan(float[][] chanbuf, int nsamples) {

		if (wetout == 0) {
			return;
		}

		float dry = dryout;

		for (int i = 0; i < nsamples; i++) {
			processSample(chanbuf[i], chanbuf[i][0] + fcdcoffset, chanbuf[i][1] + fcdcoffset, dry);
		}
	}

	float processChanSample(float in, int ch, float dry) {
		long counter = (mcnt + ((ch != 0) ? mphase : 0)) & 0xffffffffl;
		counter = ((counter < 0x80000000l) ? counter * 2 : 0xffffffffl - counter * 2) & 0xffffffffl;

		long offs32_32 = (long) counter * mmaxoffs;
		long offs_int = ((offs32_32 >> 32) + dboffs[ch]) & 0xffffffffl;
		long index = (long)(dbptr - offs_int) & 0xffffffffl;

		float[] delaybuf = db[ch];
		float x = Utils.utof23((long)(offs32_32 & 0xffffffffl));
		float delayed = Utils.lerp(delaybuf[(int)((index - 0) & dbufmask)], delaybuf[(int)((index - 1) & dbufmask)], x);

		delaybuf[(int)dbptr] = in + delayed * fbval;
		return in * dry + delayed * wetout;
		
		//return in;
	}

	//void processSample(StereoSample out, float l, float r, float dry) {
	void processSample(float[] out, float l, float r, float dry) {
		out[0] = processChanSample(l, 0, dry);
		out[1] = processChanSample(r, 1, dry);

		mcnt += mfreq & 0xffffffffl;
		dbptr = (dbptr + 1) & dbufmask;
	}
}
