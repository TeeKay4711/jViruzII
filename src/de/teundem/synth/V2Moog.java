package de.teundem.synth;

public class V2Moog implements Constants {
	float b[] = new float[5];

	public void init() {
		b[0] = b[1] = b[2] = b[3] = b[4] = 0.0f;
	}

	float step(float realin, float f, float p, float q) {
		float in = realin + fcdcoffset;
		float t1, t2, t3, b4;

		in -= q * b[4];
		t1 = b[1];
		b[1] = (in + b[0]) * p - b[1] * f;
		t2 = b[2];
		b[2] = (t1 + b[1]) * p - b[2] * f;
		t3 = b[3];
		b[3] = (t2 + b[2]) * p - b[3] * f;
		b4 = (t3 + b[3]) * p - b[4] * f;

		b4 -= b4 * b4 * b4;
		b4 -= fcdcoffset;
		b[4] = b4 - fcdcoffset;
		b[0] = realin;

		return b4;
	}
}
