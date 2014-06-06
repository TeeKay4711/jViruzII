package de.teundem.synth;

public class V2LRC implements Constants {

	float l, b;

	public void init() {
		l = b = 0.0f;
	}

	float step(float in, float freq, float reso) {
		l += freq * b;
		float h = in - b * reso - l;
		b += freq * h;

		return h;
	}

	float step_2x(float in, float freq, float reso) {
		in += fcdcoffset;
		l += freq * b - fcdcoffset;
		b += freq * (in - b * reso - l);

		l += freq * b;
		float h = in - b * reso - l;
		b += freq * h;

		return h;
	}
}
