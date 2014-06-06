package de.teundem.synth;

public class V2Chan implements Constants {
	/*public static enum FXRouting {
		FXR_DIST_THEN_CHORUS,
		FXR_CHORUS_THEN_DIST
	}*/
	private final int FXR_DIST_THEN_CHORUS = 0;
	private final int FXR_CHORUS_THEN_DIST = 1;

	float chgain;
	float a1gain;
	float a2gain;
	float aasnd;
	float absnd;
	float aarcv;
	float abrcv;
	int fxr;
	V2DCFilter dcf1 = new V2DCFilter();
	V2Boost boost = new V2Boost();
	V2Dist dist = new V2Dist();
	V2DCFilter dcf2 = new V2DCFilter();
	V2ModDel chorus = new V2ModDel();
	V2Comp comp = new V2Comp();

	V2Instance inst;

	void init(V2Instance instance, float[] delbuf1, float[] delbuf2, int buflen) {
		inst = instance;
		dcf1.init(inst);
		boost.init(inst);
		dist.init(inst);
		dcf2.init(inst);
		chorus.init(inst, delbuf1, delbuf2, buflen);
		comp.init(inst);
	}

	void set(syVChan para) {
		aarcv = para.auxarcv / 128.0f;
		abrcv = para.auxbrcv / 128.0f;
		aasnd = fcgain * (para.auxasnd / 128.0f);
		absnd = fcgain * (para.auxbsnd / 128.0f);
		chgain = fcgain * (para.chanvol / 128.0f);
		a1gain = chgain * fcgainh * (para.aux1 / 128.0f);
		a2gain = chgain * fcgainh * (para.aux2 / 128.0f);
		fxr = (int) para.fxroute;

		dist.set(para.dist);
		chorus.set(para.chorus);
		comp.set(para.comp);
		boost.set(para.boost);
	}

	void process(int nsamples) {
		//StereoSample[] chan = inst.chanbuf;
		float[][] chan = inst.chanbuf;

		accumulate(chan, inst.auxabuf, nsamples, aarcv);
		accumulate(chan, inst.auxbbuf, nsamples, abrcv);

		dcf1.renderStereo(chan, chan, nsamples);
		comp.render(chan, nsamples);
		boost.render(chan, nsamples);

		if (fxr == FXR_DIST_THEN_CHORUS) {
			dist.renderStereo(chan, chan, nsamples);
			dcf2.renderStereo(chan, chan, nsamples);
			chorus.renderChan(chan, nsamples);
		} else {
			chorus.renderChan(chan, nsamples);
			dist.renderStereo(chan, chan, nsamples); 
			dcf2.renderStereo(chan, chan, nsamples);
		}

		accumulateMonoMix(inst.aux1buf, chan, nsamples, a1gain);
		accumulateMonoMix(inst.aux2buf, chan, nsamples, a2gain);

		accumulate(inst.auxabuf, chan, nsamples, aasnd);
		accumulate(inst.auxbbuf, chan, nsamples, absnd);

		accumulate(inst.mixbuf, chan, nsamples, chgain);
	}

	//void accumulate(StereoSample[] dest, StereoSample[] src, int nsamples, float gain) {
	void accumulate(float[][] dest, float[][] src, int nsamples, float gain) {
		if (gain == 0.0f) {
			return;
		}

		for (int i = 0; i < nsamples; i++) {
			dest[i][0] += gain * src[i][0];
			dest[i][1] += gain * src[i][1];
		}
	}

	//void accumulateMonoMix(float[] dest, StereoSample[] src, int nsamples, float gain) {
	void accumulateMonoMix(float[] dest, float[][] src, int nsamples, float gain) {
		if (gain == 0.0f) {
			return;
		}

		for (int i = 0; i < nsamples; i++) {
			dest[i] += gain * (src[i][0] + src[i][1]);
		}
	}
}
