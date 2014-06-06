package de.teundem.synth;

public class V2DCF implements Constants {
	float xm1;
	float ym1;

	public void init() {
		xm1 = ym1 = 0.0f;
	}

	float step(float in, float R) {
		float y = (fcdcoffset + R * ym1 - xm1 + in) - fcdcoffset;
		xm1 = in;
		ym1 = y;
		return y;
	}
}
