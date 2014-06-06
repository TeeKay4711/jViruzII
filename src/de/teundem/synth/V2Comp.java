package de.teundem.synth;

class V2Comp implements Constants {
	static final int COMPDLEN = 5700;
	static final int RMSLEN = 8192; // must be a power of 2

	static final int MODE_OFF = 0;
	static final int MODE_PEAK = 1;
	static final int MODE_RMS = 2;

	static final int MODE_BIT_PEAK = 0;
	static final int MODE_BIT_RMS = 1;
	static final int MODE_BIT_MONO = 0;
	static final int MODE_BIT_STEREO = 2;
	static final int MODE_BIT_ON = 0;
	static final int MODE_BIT_OFF = 4;

	int mode; // bit 0: Peak/RMS, bit 1: Stereo, bit 2: off

	float invol; // input gain (1/threshold, internal threshold is always 0dB)
	float ratio;
	float outvol; // output gain (outgain * threshold)
	float attack; // attack (lpf coeff, 0..1)
	float release; // release (lpf coeff, 0..1)

	int /*long*/ dblen; // lookahead buffer length
	int /*long*/ dbcnt; // lookahead buffer offset

	float[] curgain = new float[2]; // current gain
	float[] peakval = new float[2]; // peak value
	float[] rmsval = new float[2]; // rms current value
	int /*long*/ rmscnt; // rms counter

	float[][] dbuf = new float[COMPDLEN][2]; // lookahead delay buffer
	float[][] rmsbuf = new float[RMSLEN][2]; // RMS ring buffer

	V2Instance inst;

	void init(V2Instance instance) {
		mode = MODE_BIT_STEREO;
		//memset(dbuf, 0, sizeof(dbuf));
		inst = instance;

		reset();
	}

	void reset() {
		for (int i = 0; i < 2; i++) {
			peakval[i] = 0.0f;
			rmsval[i] = 0.0f;
			curgain[i] = 1.0f;
		}
		//memset(rmsbuf, 0, sizeof(rmsbuf));
		for(int i = 0; i < rmsbuf.length;i++) {
			rmsbuf[i][0] = 0.0f;
			rmsbuf[i][1] = 0.0f;
		}
		rmscnt = 0;
	}

	void set(syVComp para) {
		int oldmode = mode;
		switch ((int) para.mode) {
			case MODE_OFF:
				mode = MODE_BIT_OFF;
				break;
			case MODE_PEAK:
				mode = MODE_BIT_PEAK | MODE_BIT_ON;
				break;
			case MODE_RMS:
				mode = MODE_BIT_RMS | MODE_BIT_ON;
				break;
			default:
				//assert (false);
		}

		if (para.stereo != 0.0f)
			mode |= MODE_BIT_STEREO;

		if (mode != oldmode)
			reset();

		// @@@BUG: original V2 code uses "fcsamplesperms" here which is
		// hard-coded to 44.1kHz
		dblen = (int)(para.lookahead * inst.SRfcsamplesperms);

		float thresh = 8.0f * Utils.calcfreq(para.threshold / 128.0f);
		invol = 1.0f / thresh;
		if (para.autogain != 0.0f)
			thresh = 1.0f;
		outvol = (float) (thresh * Math.pow(2.0f, (para.outgain - 64.0f) / 16.0f));
		ratio = para.ratio / 128.0f;

		// attack: 0 (!) ... 200ms (5Hz)
		attack = (float) Math.pow(2.0f, -para.attack * 12.0f / 128.0f);
		// release: 5ms .. 5s
		release = (float) Math.pow(2.0f, -para.release * 16.0f / 128.0f);
	}

	//void render(StereoSample *buf, int nsamples)
	void render(float[][] buf, int nsamples) {
		if ((mode & MODE_BIT_OFF) != 0)
			return;

		//COVER("COMP render");

		// Step 1: level detect (fills LD buffers)
		//StereoSample *levels = inst.levelbuf;
		float[][] levels = inst.levelbuf;
		float t = 0.0f;
		switch (mode & (MODE_BIT_RMS | MODE_BIT_STEREO)) {
			case MODE_BIT_PEAK | MODE_BIT_MONO:
				//COVER("COMP level peak mono");
				for (int i = 0; i < nsamples; i++) {
					//levels[i][0] = levels[i][1] = invol * doPeak(0.5f * (buf[i][0] + buf[i][1]), 0);
					t = invol * doPeak(0.5f * (buf[i][0] + buf[i][1]), 0);
					levels[i][0] = t;
					levels[i][1] = t;
				}
				break;

			case MODE_BIT_RMS | MODE_BIT_MONO:
				//COVER("COMP level rms mono");
				for (int i = 0; i < nsamples; i++) {
					//levels[i][0] = levels[i][1] = invol * doRMS(0.5f * (buf[i][0] + buf[i][1]), 0);
					t = invol * doRMS(0.5f * (buf[i][0] + buf[i][1]), 0);
					levels[i][0] = t;
					levels[i][1] = t;
					rmscnt = (rmscnt + 1) & (RMSLEN - 1);
				}
				break;

			case MODE_BIT_PEAK | MODE_BIT_STEREO:
				//COVER("COMP level peak stereo");
				for (int i = 0; i < nsamples; i++) {
					levels[i][0] = invol * doPeak(buf[i][0], 0);
					levels[i][1] = invol * doPeak(buf[i][1], 1);
				}
				break;

			case MODE_BIT_RMS | MODE_BIT_STEREO:
				//COVER("COMP level rms stereo");
				for (int i = 0; i < nsamples; i++) {
					levels[i][0] = invol * doRMS(buf[i][0], 0);
					levels[i][1] = invol * doRMS(buf[i][1], 1);
					rmscnt = (rmscnt + 1) & (RMSLEN - 1);
				}
				break;
		}

		// Step 2: compress!
		for (int ch = 0; ch < 2; ch++) {
			float gain = curgain[ch];
			int /*long*/ dbind = dbcnt;

			for (int i = 0; i < nsamples; i++) {
				// lookahead delay line
				float v = outvol * dbuf[dbind][ch];
				dbuf[dbind][ch] = invol * buf[i][ch];
				if (++dbind >= dblen)
					dbind = 0;

				// determine dest gain
				float dgain = 1.0f;
				float lvl = levels[i][ch];
				if (lvl >= 1.0f)
					dgain = 1.0f / (1.0f + ratio * (lvl - 1.0f));

				// and compress
				gain += (dgain < gain ? attack : release) * (dgain - gain);
				buf[i][ch] = v * gain;
			}

			curgain[ch] = gain;
			if (ch == 1)
				dbcnt = dbind;
		}
	}

	// level detection variants
	float doPeak(float in, int ch) {
		peakval[ch] = Utils.max(peakval[ch] * fccpdfalloff + fcdcoffset, Math.abs(in));
		return peakval[ch];
	}

	float doRMS(float in, int ch) {
		float insq = Utils.sqr(in + fcdcoffset);
		rmsval[ch] += insq - rmsbuf[(int) rmscnt][ch]; // add new sample, remove oldest
		rmsbuf[(int) rmscnt][ch] = insq; // keep track of value we added
		return (float) Math.sqrt(rmsval[ch] / (float) RMSLEN);
	}
}
