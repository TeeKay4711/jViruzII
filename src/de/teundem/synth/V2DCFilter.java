package de.teundem.synth;

public class V2DCFilter {
	V2DCF fl = new V2DCF();
	V2DCF fr = new V2DCF();
	V2Instance inst;

	public void init(V2Instance instance) {
		inst = instance;
		fl.init();
		fr.init();
	}

	void renderMono(float[] dest, float[] src, int nsamples) {
		float R = inst.SRfcdcfilter;

		V2DCF l = fl;
		for (int i = 0; i < nsamples; i++) {
			dest[i] = l.step(src[i], R);
		}

		fl = l;
	}

	//void renderStereo(StereoSample[] dest, StereoSample[] src, int nsamples) {
	void renderStereo(float[][] dest, float[][] src, int nsamples) {

		float R = inst.SRfcdcfilter;

		V2DCF l = fl;
		V2DCF r = fr;

		for (int i = 0; i < nsamples; i++) {
			//dest[i].l = l.step(src[i].l, R);
			//dest[i].r = r.step(src[i].r, R);
			dest[i][0] = l.step(src[i][0], R);
			dest[i][1] = r.step(src[i][1], R);
		}
		fl = l;
		fr = r;
	}
}
